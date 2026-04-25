package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.block.*;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ModuleInfo(
    name = "BaseRadar",
    category = Category.DONUT,
    desc = "Scanează chunk-urile încărcate pentru concentrații de blocuri plasate de jucători."
)
public class BaseRadar extends Function {

    private final NumberSetting  minScore           = new NumberSetting("Min Score",             this, 5,   1,   50,  1);
    private final BooleanSetting detectWorkstations = new BooleanSetting("Workstations",         true);
    private final BooleanSetting detectStorage      = new BooleanSetting("Storage",              true);
    private final BooleanSetting detectFarming      = new BooleanSetting("Farming Blocks",       true);
    private final BooleanSetting detectLighting     = new BooleanSetting("Light Sources",        true);
    private final BooleanSetting detectPaths        = new BooleanSetting("Paths",                true);
    private final BooleanSetting detectNetherBlocks = new BooleanSetting("Nether Blocks",        true);
    private final NumberSetting  scanRadius         = new NumberSetting("Scan Radius (chunks)",  this, 12,  4,   32,  1);
    private final NumberSetting  renderHeight       = new NumberSetting("Render Height",         this, 64, -64, 320,  1);
    private final BooleanSetting chatNotify         = new BooleanSetting("Chat Notify",          true);

    private final Map<ChunkPos, Integer> scores          = new ConcurrentHashMap<>();
    private final Set<ChunkPos>          flagged          = ConcurrentHashMap.newKeySet();
    private final Set<ChunkPos>          alreadyNotified  = ConcurrentHashMap.newKeySet();

    public BaseRadar() {
        this.addSettings(new Setting[]{
            minScore, detectWorkstations, detectStorage, detectFarming,
            detectLighting, detectPaths, detectNetherBlocks,
            scanRadius, renderHeight, chatNotify
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        scores.clear();
        flagged.clear();
        alreadyNotified.clear();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        scores.clear();
        flagged.clear();
        alreadyNotified.clear();
    }

    /**
     * Apelat din mixin-ul ChunkData (sau evenimentul tău de chunk load).
     * Exemplu în mixin: FourEClient.getInstance().getFunctionManager()
     *     .getModule(BaseRadar.class).ifPresent(m -> m.onChunkLoaded(chunk));
     */
    public void onChunkLoaded(WorldChunk chunk) {
        ChunkPos pos = chunk.getPos();
        int score = scoreChunk(chunk);
        scores.put(pos, score);
        if (score >= minScore.getValueInt()) {
            flagged.add(pos);
            if ((Boolean) chatNotify.getValue() && alreadyNotified.add(pos)) {
                sendMessage("Baza posibila la chunk " + pos.x + ", " + pos.z + " (score: " + score + ")");
            }
        } else {
            flagged.remove(pos);
        }
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        int radius = scanRadius.getValueInt();
        ChunkPos playerChunk = mc.player.getChunkPos();
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

        for (ChunkPos pos : flagged) {
            if (Math.abs(pos.x - playerChunk.x) > radius || Math.abs(pos.z - playerChunk.z) > radius) continue;

            int score = scores.getOrDefault(pos, 0);
            double ratio = Math.min(1.0, (double)(score - minScore.getValueInt()) / (double) Math.max(1, minScore.getValueInt() * 4));

            // galben (low) -> rosu (high)
            int r = 255;
            int g = (int)(255 * (1.0 - ratio));
            int a = (int)(80 + ratio * 60);

            double x1 = pos.getStartX() - camX;
            double z1 = pos.getStartZ() - camZ;
            double x2 = x1 + 16;
            double z2 = z1 + 16;
            double y  = renderHeight.getValueFloat() - camY;

            drawBoxEdges(buf, matrix, x1, y - 0.5, z1, x2, y + 0.5, z2, r, g, 0, a);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) BufferRenderer.drawWithGlobalProgram(built);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ─── scoring ─────────────────────────────────────────────────────────────

    private int scoreChunk(WorldChunk chunk) {
        int score = 0;
        for (ChunkSection sec : chunk.getSectionArray()) {
            if (sec == null || sec.isEmpty()) continue;
            for (int x = 0; x < 16; x++)
                for (int z = 0; z < 16; z++)
                    for (int y = 0; y < 16; y++)
                        score += scoreBlock(sec.getBlockState(x, y, z).getBlock());
        }
        return score;
    }

    private int scoreBlock(Block b) {
        if ((Boolean) detectStorage.getValue()) {
            if (b == Blocks.CHEST || b == Blocks.TRAPPED_CHEST || b == Blocks.ENDER_CHEST) return 4;
            if (b instanceof ShulkerBoxBlock) return 5;
            if (b == Blocks.BARREL) return 3;
            if (b == Blocks.HOPPER || b == Blocks.DROPPER || b == Blocks.DISPENSER) return 3;
        }
        if ((Boolean) detectWorkstations.getValue()) {
            if (b == Blocks.CRAFTING_TABLE) return 3;
            if (b == Blocks.FURNACE || b == Blocks.BLAST_FURNACE || b == Blocks.SMOKER) return 3;
            if (b == Blocks.ENCHANTING_TABLE || b == Blocks.BOOKSHELF || b == Blocks.LECTERN) return 4;
            if (b == Blocks.BREWING_STAND) return 4;
            if (b == Blocks.ANVIL) return 6;
            if (b == Blocks.GRINDSTONE) return 4;
            if (b == Blocks.CARTOGRAPHY_TABLE || b == Blocks.FLETCHING_TABLE || b == Blocks.SMITHING_TABLE) return 2;
            if (b instanceof BedBlock) return 2;
        }
        if ((Boolean) detectFarming.getValue()) {
            if (b == Blocks.FARMLAND) return 2;
            if (b == Blocks.COMPOSTER) return 2;
            if (b == Blocks.PUMPKIN || b == Blocks.MELON) return 1;
        }
        if ((Boolean) detectLighting.getValue()) {
            if (b == Blocks.TORCH || b == Blocks.WALL_TORCH) return 1;
            if (b == Blocks.SOUL_TORCH || b == Blocks.SOUL_WALL_TORCH) return 1;
            if (b == Blocks.LANTERN || b == Blocks.SOUL_LANTERN) return 1;
            if (b == Blocks.GLOWSTONE || b == Blocks.SEA_LANTERN) return 1;
            if (b == Blocks.SHROOMLIGHT || b == Blocks.JACK_O_LANTERN) return 1;
        }
        if ((Boolean) detectPaths.getValue()) {
            if (b == Blocks.DIRT_PATH || b == Blocks.GRAVEL) return 1;
            if (b instanceof SlabBlock || b instanceof StairsBlock) return 1;
        }
        if ((Boolean) detectNetherBlocks.getValue()) {
            if (b == Blocks.NETHER_BRICKS || b == Blocks.RED_NETHER_BRICKS) return 4;
            if (b == Blocks.QUARTZ_BLOCK || b == Blocks.SMOOTH_QUARTZ) return 3;
            if (b == Blocks.OBSIDIAN) return 2;
            if (b == Blocks.CRYING_OBSIDIAN || b == Blocks.RESPAWN_ANCHOR) return 2;
        }
        return 0;
    }

    // ─── render util ─────────────────────────────────────────────────────────

    private static void drawBoxEdges(BufferBuilder buf, Matrix4f mat,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     int r, int g, int b, int a) {
        float ax = (float) x1, bx = (float) x2;
        float ay = (float) y1, by = (float) y2;
        float az = (float) z1, bz = (float) z2;
        // bottom
        ln(buf, mat, ax, ay, az, bx, ay, az, r, g, b, a);
        ln(buf, mat, bx, ay, az, bx, ay, bz, r, g, b, a);
        ln(buf, mat, bx, ay, bz, ax, ay, bz, r, g, b, a);
        ln(buf, mat, ax, ay, bz, ax, ay, az, r, g, b, a);
        // top
        ln(buf, mat, ax, by, az, bx, by, az, r, g, b, a);
        ln(buf, mat, bx, by, az, bx, by, bz, r, g, b, a);
        ln(buf, mat, bx, by, bz, ax, by, bz, r, g, b, a);
        ln(buf, mat, ax, by, bz, ax, by, az, r, g, b, a);
        // verticals
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
            mc.player.sendMessage(Text.literal("§e[BaseRadar]§r " + msg), false);
    }
}
