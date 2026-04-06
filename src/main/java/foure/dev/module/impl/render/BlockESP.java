package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.world.Dir;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(
    name = "BlockESP",
    category = Category.RENDER,
    desc = "Highlights selected blocks through walls"
)
public class BlockESP extends Function {

    private final BooleanSetting tracers = new BooleanSetting("Tracers", true);
    private final NumberSetting alpha = new NumberSetting("Alpha", this, 125.0, 0.0, 255.0, 1.0);
    private final NumberSetting lineWidth = new NumberSetting("LineWidth", this, 1.5, 0.5, 5.0, 0.1);

    private final Set<Block> targetBlocks = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Long, Set<BlockPos>> cachedBlocks = new ConcurrentHashMap<>();
    private final Map<Block, Color> blockColors = new HashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Set<Long> scanningChunks = ConcurrentHashMap.newKeySet();
    private long lastRescan = 0;

    private final List<BlockESP.RenderEntry> boxesToRender = new ArrayList<>();

    private static final int[][] FACE_INDICES = new int[][]{{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}};
    private static final byte[] FACE_DIRS = new byte[]{4,2,8,16,32,64};
    private static final float[] FACE_SHADING = new float[]{0.5f,1.0f,0.7f,0.8f,0.6f,0.9f};

    public BlockESP() {
        this.addSettings(new Setting[]{this.tracers, this.alpha, this.lineWidth});
    }

    public void onEnable() {
        super.onEnable();
        this.cachedBlocks.clear();
        this.scanningChunks.clear();
    }

    public void onDisable() {
        super.onDisable();
        this.cachedBlocks.clear();
        this.scanningChunks.clear();
    }

    public void addTargetBlock(Block block) {
        targetBlocks.add(block);
    }

    public void clearTargetBlocks() {
        targetBlocks.clear();
        cachedBlocks.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!fullNullCheck() && !targetBlocks.isEmpty()) {
            long now = System.currentTimeMillis();
            if (now - lastRescan > 2000L) {
                lastRescan = now;
                scanAllChunks();
            }
        }
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (fullNullCheck() || targetBlocks.isEmpty()) return;

        boxesToRender.clear();

        for (Set<BlockPos> blockSet : cachedBlocks.values()) {
            if (blockSet == null) continue;
            for (BlockPos pos : blockSet) {
                if (pos == null) continue;
                double distSq = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                if (distSq > 20000.0) continue;
                BlockState state = mc.world.getBlockState(pos);
                Block block = state.getBlock();
                Color base = blockColors.getOrDefault(block, Color.WHITE);
                Box box = new Box(pos.getX(), pos.getY(), pos.getZ(),
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
                boxesToRender.add(new RenderEntry(box, base));
            }
        }
    }

    @Subscribe
    public void onRender2D(RenderEvent event) {
        if (boxesToRender.isEmpty()) return;

        Renderer2D r = event.renderer();
        int currentAlpha = (int) this.alpha.getValueFloat();
        float thickness = (float) this.lineWidth.getValueFloat();

        for (RenderEntry entry : boxesToRender) {
            Box b = entry.box;
            Color base = entry.color;
            Color fill = new Color(base.getRed(), base.getGreen(), base.getBlue(), currentAlpha);
            Color outline = new Color(base.getRed(), base.getGreen(), base.getBlue(), 255);

            Vec3d[] corners = new Vec3d[]{
                new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.maxX, b.minY, b.minZ),
                new Vec3d(b.maxX, b.minY, b.maxZ), new Vec3d(b.minX, b.minY, b.maxZ),
                new Vec3d(b.minX, b.maxY, b.minZ), new Vec3d(b.maxX, b.maxY, b.minZ),
                new Vec3d(b.maxX, b.maxY, b.maxZ), new Vec3d(b.minX, b.maxY, b.maxZ)
            };

            Vec3d[] sc = new Vec3d[8];
            int onScreen = 0;
            for (int i = 0; i < 8; i++) {
                sc[i] = ProjectionUtil.toScreen(corners[i]);
                if (sc[i] != null) onScreen++;
            }
            if (onScreen == 0) continue;

            // fill faces
            for (int f = 0; f < 6; f++) {
                int[] idx = FACE_INDICES[f];
                Vec3d p1 = sc[idx[0]], p2 = sc[idx[1]], p3 = sc[idx[2]], p4 = sc[idx[3]];
                if (p1 != null && p2 != null && p3 != null && p4 != null) {
                    float shading = FACE_SHADING[f];
                    Color shaded = new Color(
                        (int)(fill.getRed() * shading),
                        (int)(fill.getGreen() * shading),
                        (int)(fill.getBlue() * shading),
                        fill.getAlpha());
                    r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y,
                        (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, shaded.getRGB());
                }
            }

            // outline
            int oc = outline.getRGB();
            drawLine(r, sc[0], sc[1], thickness, oc); drawLine(r, sc[1], sc[2], thickness, oc);
            drawLine(r, sc[2], sc[3], thickness, oc); drawLine(r, sc[3], sc[0], thickness, oc);
            drawLine(r, sc[4], sc[5], thickness, oc); drawLine(r, sc[5], sc[6], thickness, oc);
            drawLine(r, sc[6], sc[7], thickness, oc); drawLine(r, sc[7], sc[4], thickness, oc);
            drawLine(r, sc[0], sc[4], thickness, oc); drawLine(r, sc[1], sc[5], thickness, oc);
            drawLine(r, sc[2], sc[6], thickness, oc); drawLine(r, sc[3], sc[7], thickness, oc);

            // tracer
            if ((Boolean) this.tracers.getValue()) {
                Vec3d center = new Vec3d((b.minX + b.maxX) / 2.0, (b.minY + b.maxY) / 2.0, (b.minZ + b.maxZ) / 2.0);
                Vec3d screenCenter = ProjectionUtil.toScreen(center);
                if (screenCenter != null) {
                    r.line(event.scaledWidth() / 2.0f, event.scaledHeight() / 2.0f,
                        (float) screenCenter.x, (float) screenCenter.y, 1.0f, outline.getRGB());
                }
            }
        }
    }

    private void drawLine(Renderer2D r, Vec3d p1, Vec3d p2, float thickness, int color) {
        if (p1 != null && p2 != null)
            r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, thickness, color);
    }

    private void scanAllChunks() {
        if (mc.world == null || mc.player == null) return;
        ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                int cx = playerChunk.x + x;
                int cz = playerChunk.z + z;
                if (mc.world.isChunkLoaded(cx, cz)) {
                    scanChunk(mc.world.getChunk(cx, cz));
                }
            }
        }
    }

    private void scanChunk(WorldChunk chunk) {
        if (chunk == null || targetBlocks.isEmpty()) return;
        long key = chunk.getPos().toLong();
        if (!scanningChunks.add(key)) return;

        executor.submit(() -> {
            try {
                Set<BlockPos> found = ConcurrentHashMap.newKeySet();
                ChunkPos cPos = chunk.getPos();
                int startX = cPos.getStartX();
                int startZ = cPos.getStartZ();
                ChunkSection[] sections = chunk.getSectionArray();
                int minSection = mc.world.getBottomSectionCoord();

                for (int si = 0; si < sections.length; si++) {
                    ChunkSection section = sections[si];
                    if (section == null || section.isEmpty()) continue;
                    int sectionY = (minSection + si) * 16;
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = 0; y < 16; y++) {
                                BlockState state = section.getBlockState(x, y, z);
                                Block block = state.getBlock();
                                if (targetBlocks.contains(block)) {
                                    found.add(new BlockPos(startX + x, sectionY + y, startZ + z));
                                    blockColors.computeIfAbsent(block, b -> {
                                        int hash = Math.abs(b.hashCode());
                                        float hue = (float)(hash % 360) / 360.0f;
                                        float sat = 0.7f + (float)((hash >> 8) % 30) / 100.0f;
                                        return Color.getHSBColor(hue, sat, 0.9f);
                                    });
                                }
                            }
                        }
                    }
                }

                if (found.isEmpty()) cachedBlocks.remove(key);
                else cachedBlocks.put(key, found);
            } catch (Exception ignored) {
            } finally {
                scanningChunks.remove(key);
            }
        });
    }

    private record RenderEntry(Box box, Color color) {}
}
