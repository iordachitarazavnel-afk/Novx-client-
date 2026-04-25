package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ModuleInfo(
    name = "SpawnerDetect",
    category = Category.DONUT,
    desc = "Detectare strictă de spawner-e. Flaghează doar semnale de înaltă încredere sub deepslate."
)
public class SpawnerDetect extends Function {

    private final BooleanSetting strictBlockEntity    = new BooleanSetting("Strict Spawner Scan", true);
    private final BooleanSetting detectLightAnomalies = new BooleanSetting("Light Anomalies",     true);
    private final NumberSetting  scanRadius           = new NumberSetting("Scan Radius", this, 12, 1, 32, 1);

    private final Set<ChunkPos>    spawnerChunks = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos>    notified      = ConcurrentHashMap.newKeySet();
    private final ExecutorService  executor      = Executors.newSingleThreadExecutor();
    private final AtomicBoolean    isScanning    = new AtomicBoolean(false);
    private int scanTicks = 0;

    public SpawnerDetect() {
        this.addSettings(new Setting[]{strictBlockEntity, detectLightAnomalies, scanRadius});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        spawnerChunks.clear();
        notified.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (++scanTicks < 100) return;
        scanTicks = 0;
        if (isScanning.compareAndSet(false, true))
            executor.execute(this::scan);
    }

    private void scan() {
        try {
            if (mc.world == null || mc.player == null) return;
            ChunkPos center = mc.player.getChunkPos();
            int radius = scanRadius.getValueInt();
            for (int x = -radius; x <= radius; x++)
                for (int z = -radius; z <= radius; z++) {
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(center.x + x, center.z + z);
                    if (chunk != null) processChunk(chunk);
                }
        } finally {
            isScanning.set(false);
        }
    }

    private void processChunk(WorldChunk chunk) {
        ChunkPos cp = chunk.getPos();

        if ((Boolean) strictBlockEntity.getValue()) {
            for (BlockEntity be : chunk.getBlockEntities().values()) {
                if (be instanceof MobSpawnerBlockEntity) {
                    addSpawner(be.getPos(), "spawner entity");
                    return;
                }
            }
        }

        if ((Boolean) detectLightAnomalies.getValue()) {
            int anomalies = 0;
            for (int y = mc.world.getBottomY(); y < 0; y += 4)
                for (int x = 0; x < 16; x += 4)
                    for (int z = 0; z < 16; z += 4) {
                        BlockPos pos = new BlockPos(cp.getStartX() + x, y, cp.getStartZ() + z);
                        if (mc.world.getLightLevel(LightType.BLOCK, pos) > 11
                                && chunk.getBlockState(pos).getOpacity() == 0
                                && isAnomalousArea(pos))
                            anomalies++;
                    }
            if (anomalies > 8)
                addSpawner(new BlockPos(cp.getStartX() + 8, -32, cp.getStartZ() + 8), "lit spawner room");
        }
    }

    private boolean isAnomalousArea(BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values())
            if (mc.world.getLightLevel(LightType.BLOCK, pos.offset(dir)) > 8) count++;
        return count >= 2;
    }

    @Subscribe
    public void onPacket(PacketEvent event) {
        if (mc.world == null) return;
        if (event.getPacket() instanceof BlockUpdateS2CPacket p)
            if (p.getState().getBlock() == Blocks.SPAWNER)
                addSpawner(p.getPos(), "placed spawner");
    }

    private void addSpawner(BlockPos pos, String reason) {
        ChunkPos cp = new ChunkPos(pos);
        if (spawnerChunks.add(cp) && notified.add(cp))
            sendMessage("Spawner detectat la " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + " via " + reason);
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null) return;

        double camX = event.getCamera().getPos().x;
        double camY = event.getCamera().getPos().y;
        double camZ = event.getCamera().getPos().z;
        Matrix4f matrix = event.getMatrix();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        for (ChunkPos pos : spawnerChunks) {
            double x1 = pos.getStartX() - camX;
            double z1 = pos.getStartZ() - camZ;
            double x2 = x1 + 16;
            double z2 = z1 + 16;
            double y1 = mc.world.getBottomY() - camY;
            double y2 = mc.world.getTopY()    - camY;
            drawBoxEdges(buf, matrix, x1, y1, z1, x2, y2, z2, 255, 255, 255, 200);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ─── render util ─────────────────────────────────────────────────────────

    private static void drawBoxEdges(BufferBuilder buf, Matrix4f mat,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     int r, int g, int b, int a) {
        float ax = (float) x1, bx = (float) x2;
        float ay = (float) y1, by = (float) y2;
        float az = (float) z1, bz = (float) z2;
        ln(buf, mat, ax, ay, az, bx, ay, az, r, g, b, a);
        ln(buf, mat, bx, ay, az, bx, ay, bz, r, g, b, a);
        ln(buf, mat, bx, ay, bz, ax, ay, bz, r, g, b, a);
        ln(buf, mat, ax, ay, bz, ax, ay, az, r, g, b, a);
        ln(buf, mat, ax, by, az, bx, by, az, r, g, b, a);
        ln(buf, mat, bx, by, az, bx, by, bz, r, g, b, a);
        ln(buf, mat, bx, by, bz, ax, by, bz, r, g, b, a);
        ln(buf, mat, ax, by, bz, ax, by, az, r, g, b, a);
        ln(buf, mat, ax, ay, az, ax, by, az, r, g, b, a);
        ln(buf, mat, bx, ay, az, bx, by, az, r, g, b, a);
        ln(buf, mat, bx, ay, bz, bx, by, bz, r, g, b, a);
        ln(buf, mat, ax, ay, bz, ax, by, bz, r, g, b, a);
    }

    private static void ln(BufferBuilder buf, Matrix4f mat,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            int r, int g, int b, int a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
    }

    private void sendMessage(String msg) {
        if (mc.player != null)
            mc.player.sendMessage(Text.literal("§b[SpawnerDetect]§r " + msg), false);
    }
}
