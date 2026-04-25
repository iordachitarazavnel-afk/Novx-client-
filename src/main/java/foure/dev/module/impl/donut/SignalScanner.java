package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ModuleInfo(
    name = "SignalScanner",
    category = Category.DONUT,
    desc = "Detectează semnale anomale (lumină, block entities) în chunk-uri pentru a găsi baze."
)
public class SignalScanner extends Function {

    private final NumberSetting  scanRadius           = new NumberSetting("Scan Radius",   this, 12, 1, 32, 1);
    private final NumberSetting  scanInterval         = new NumberSetting("Scan Interval", this, 20, 1, 100, 1);
    private final BooleanSetting detectBlockEntities  = new BooleanSetting("Block Entities",  true);
    private final BooleanSetting detectLightAnomalies = new BooleanSetting("Light Anomalies", true);
    private final BooleanSetting chatNotify           = new BooleanSetting("Chat Notify",     true);

    private final Map<ChunkPos, Signal> signals  = new ConcurrentHashMap<>();
    private final Set<ChunkPos>         announced = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor        = Executors.newSingleThreadExecutor();
    private final AtomicBoolean   isScanning      = new AtomicBoolean(false);
    private int ticks = 0;

    public SignalScanner() {
        this.addSettings(new Setting[]{
            scanRadius, scanInterval, detectBlockEntities, detectLightAnomalies, chatNotify
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        signals.clear();
        announced.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (++ticks < scanInterval.getValueInt()) return;
        ticks = 0;
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
        int score = 0;
        List<String> reasons = new ArrayList<>();

        if ((Boolean) detectBlockEntities.getValue()) {
            int count = 0;
            for (BlockEntity be : chunk.getBlockEntities().values())
                if (be.getCachedState().getBlock() != Blocks.AIR) count++;
            if (count > 0) {
                score += count * 5;
                reasons.add(count + " block entities");
            }
        }

        if ((Boolean) detectLightAnomalies.getValue()) {
            int anomalies = 0;
            int bottom = mc.world.getBottomY();
            int top    = bottom + mc.world.getHeight();
            for (int y = bottom; y < top; y += 8)
                for (int x = 0; x < 16; x += 4)
                    for (int z = 0; z < 16; z += 4) {
                        BlockPos pos = new BlockPos(cp.getStartX() + x, y, cp.getStartZ() + z);
                        if (mc.world.getLightLevel(LightType.BLOCK, pos) > 10
                                && chunk.getBlockState(pos).getOpacity() == 0
                                && isAnomalousArea(pos))
                            anomalies++;
                    }
            if (anomalies > 4) {
                score += anomalies * 10;
                reasons.add("light anomaly cluster (" + anomalies + ")");
            }
        }

        if (score > 0) {
            signals.put(cp, new Signal(cp, reasons));
            if ((Boolean) chatNotify.getValue() && announced.add(cp))
                sendMessage("Signal la " + cp.x + ", " + cp.z + " | " + String.join(", ", reasons));
        }
    }

    private boolean isAnomalousArea(BlockPos pos) {
        int count = 0;
        for (Direction dir : Direction.values())
            if (mc.world.getLightLevel(LightType.BLOCK, pos.offset(dir)) > 8) count++;
        return count >= 2;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null || signals.isEmpty()) return;

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

        for (Signal s : signals.values()) {
            double x1 = s.pos.getStartX() - camX;
            double z1 = s.pos.getStartZ() - camZ;
            double x2 = x1 + 16;
            double z2 = z1 + 16;
            double y1 = mc.world.getBottomY() - camY;
            double y2 = mc.world.getTopY()    - camY;
            drawBoxEdges(buf, matrix, x1, y1, z1, x2, y2, z2, 255, 50, 255, 220);
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
            mc.player.sendMessage(Text.literal("§d[SignalScanner]§r " + msg), false);
    }

    // ─── inner ───────────────────────────────────────────────────────────────

    private static class Signal {
        final ChunkPos    pos;
        final List<String> reasons;
        Signal(ChunkPos pos, List<String> reasons) { this.pos = pos; this.reasons = reasons; }
    }
}
