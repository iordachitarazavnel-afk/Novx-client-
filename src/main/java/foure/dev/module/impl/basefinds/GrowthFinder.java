package foure.dev.module.impl.basefinds;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.KelpBlock;
import net.minecraft.block.KelpPlantBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.Color;
import java.util.*;
import java.util.Map.Entry;

@ModuleInfo(
    name = "GrowthFinder",
    category = Category.BASEFINDS,
    desc = "Finds activity",
    icon = "foure:icons/growth_finder"
)
public class GrowthFinder extends Function {

    private final BooleanSetting renderVines     = new BooleanSetting("Render Vines",     true);
    private final BooleanSetting renderKelp      = new BooleanSetting("Render Kelp",      true);
    private final BooleanSetting renderDripstone = new BooleanSetting("Render Dripstone", true);
    private final BooleanSetting renderBerries   = new BooleanSetting("Render Berries",   true);
    private final BooleanSetting depthTest       = new BooleanSetting("Depth Test",       false);

    private static final int    SCAN_RADIUS             = 5;
    private static final int    CHUNKS_PER_TICK         = 2;
    private static final int    MIN_VINE_LENGTH         = 6;
    private static final int    MAX_VINE_SCAN_PER_CHUNK = 2;
    private static final int    MIN_KELP_HEIGHT         = 4;
    private static final double PLATE_HEIGHT            = 0.08D;
    private static final int    RESCAN_INTERVAL         = 100;

    private static final Color CHUNK_PLATE_COLOR         = new Color(135, 135, 135,  80);
    private static final Color SOURCE_PLATE_COLOR        = new Color(203,  64, 255, 255);
    private static final Color EXTREME_PLATE_COLOR       = new Color(255, 198,  64, 255);
    private static final Color LOW_SUSPICION_PLATE_COLOR = new Color(139,   0,   0,  60);

    private final Map<ChunkPos, Long>                 chunkScanTimes   = new HashMap<>();
    private final Map<ChunkPos, SuspiciousGrowthData> suspiciousChunks = new HashMap<>();
    private final List<ChunkPos>                      scanQueue        = new ArrayList<>();
    private ChunkPos             lastQueueCenter  = null;
    private int                  scanCursor       = 0;
    private List<ChunkPos>       lockedBaseChunks = new ArrayList<>();
    private final Map<ChunkPos, Double> sourceHistory = new HashMap<>();

    // animation state
    private final Map<ChunkPos, Long>  chunkFirstSeen    = new HashMap<>();
    private final Map<ChunkPos, Float> chunkAnimProgress = new HashMap<>();

    // render lists
    private final List<RenderEntry> boxesToRender = new ArrayList<>();

    private static final int[][]  FACE_INDICES = {{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}};
    private static final float[]  FACE_SHADING = {0.5f,1.0f,0.7f,0.8f,0.6f,0.9f};

    public GrowthFinder() {
        this.addSettings(new Setting[]{
            this.renderVines, this.renderKelp,
            this.renderDripstone, this.renderBerries, this.depthTest
        });
    }

    public void onEnable() {
        super.onEnable();
        clearData();
    }

    public void onDisable() {
        super.onDisable();
        clearData();
    }

    private void clearData() {
        suspiciousChunks.clear();
        chunkScanTimes.clear();
        scanQueue.clear();
        scanCursor = 0;
        lastQueueCenter = null;
        lockedBaseChunks.clear();
        sourceHistory.clear();
        chunkFirstSeen.clear();
        chunkAnimProgress.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        ChunkPos playerChunk = new ChunkPos(mc.player.getBlockPos());
        rebuildScanQueueIfNeeded(playerChunk);
        processChunkBatch(mc.world);
        pruneFarChunks(playerChunk, 5);
        if (mc.world.getTime() % 20L == 0L) {
            refreshLockedBaseChunk();
        }

        // update animation progress
        long now = System.currentTimeMillis();
        for (ChunkPos cp : suspiciousChunks.keySet()) {
            chunkFirstSeen.putIfAbsent(cp, now);
            long elapsed = now - chunkFirstSeen.get(cp);
            float progress = Math.min(1.0f, elapsed / 600.0f);
            chunkAnimProgress.put(cp, progress);
        }
        chunkFirstSeen.keySet().removeIf(cp -> !suspiciousChunks.containsKey(cp));
        chunkAnimProgress.keySet().removeIf(cp -> !suspiciousChunks.containsKey(cp));
    }

    private void rebuildScanQueueIfNeeded(ChunkPos playerChunk) {
        if (this.lastQueueCenter == null
                || playerChunk.x != this.lastQueueCenter.x
                || playerChunk.z != this.lastQueueCenter.z
                || this.scanCursor >= this.scanQueue.size()) {
            this.scanQueue.clear();
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    this.scanQueue.add(new ChunkPos(playerChunk.x + x, playerChunk.z + z));
                }
            }
            this.scanCursor = 0;
            this.lastQueueCenter = playerChunk;
        }
    }

    private void processChunkBatch(World world) {
        int processed = 0;
        long currentTime = world.getTime();
        while (this.scanCursor < this.scanQueue.size() && processed < 2) {
            ChunkPos chunkPos = this.scanQueue.get(this.scanCursor++);
            Long lastScanTime = this.chunkScanTimes.get(chunkPos);
            if (lastScanTime == null || currentTime - lastScanTime >= 100L) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkPos.x, chunkPos.z);
                if (chunk != null) {
                    this.analyzeChunk(chunk, chunkPos);
                    this.chunkScanTimes.put(chunkPos, currentTime);
                }
                processed++;
            }
        }
    }

    private void pruneFarChunks(ChunkPos playerChunk, int radius) {
        int maxDistance = radius + 2;
        int pX = playerChunk.x;
        int pZ = playerChunk.z;
        this.suspiciousChunks.entrySet().removeIf(entry ->
            Math.abs(entry.getKey().x - pX) > maxDistance || Math.abs(entry.getKey().z - pZ) > maxDistance);
        this.chunkScanTimes.entrySet().removeIf(entry ->
            Math.abs(entry.getKey().x - pX) > maxDistance || Math.abs(entry.getKey().z - pZ) > maxDistance);
    }

    private void refreshLockedBaseChunk() {
        if (this.suspiciousChunks.isEmpty()) {
            this.sourceHistory.clear();
            return;
        }
        for (SuspiciousGrowthData data : this.suspiciousChunks.values()) {
            ChunkPos sourceChunk = data.baseChunk;
            this.sourceHistory.merge(sourceChunk, (double) data.suspicionLevel, Double::sum);
        }
        this.sourceHistory.entrySet().removeIf(e -> {
            double val = e.getValue() * 0.95D;
            e.setValue(val);
            return val < 0.5D;
        });
        List<Entry<ChunkPos, Double>> ranked = new ArrayList<>(this.sourceHistory.entrySet());
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        this.lockedBaseChunks.clear();
        for (int i = 0; i < Math.min(2, ranked.size()); i++) {
            this.lockedBaseChunks.add(ranked.get(i).getKey());
        }
    }

    private void analyzeChunk(WorldChunk chunk, ChunkPos chunkPos) {
        int xStart = chunkPos.getStartX();
        int zStart = chunkPos.getStartZ();

        List<VineCluster>      vineClusters      = this.renderVines.getValue()
            ? this.detectTallGroundedVines(chunk, xStart, zStart) : Collections.emptyList();
        List<KelpColumn>       kelpColumns       = this.renderKelp.getValue()
            ? this.detectMaxGrownKelp(chunk, xStart, zStart) : Collections.emptyList();
        List<DripstoneCluster> dripstoneClusters = this.renderDripstone.getValue()
            ? this.detectMaxDripstoneClusters(chunk, xStart, zStart) : Collections.emptyList();
        List<BerryCluster>     berryClusters     = this.renderBerries.getValue()
            ? this.detectMaxGrownBerries(chunk, xStart, zStart) : Collections.emptyList();

        List<WeightedEvidence> evidencePoints = new ArrayList<>();
        double vineWeight = 0.0D;
        int maxVineLength = 0;

        for (VineCluster cluster : vineClusters) {
            double weight = cluster.length;
            vineWeight += weight;
            if (cluster.length > maxVineLength) maxVineLength = cluster.length;
            evidencePoints.add(new WeightedEvidence(cluster.centroid(), weight));
        }

        double kelpWeight = 0.0D;
        int maxKelpHeight = 0;

        for (KelpColumn column : kelpColumns) {
            double weight = column.height;
            kelpWeight += weight;
            if (column.height > maxKelpHeight) maxKelpHeight = column.height;
            evidencePoints.add(new WeightedEvidence(column.center(), weight));
        }

        double dripWeight = 0.0D;
        for (DripstoneCluster cluster2 : dripstoneClusters) {
            dripWeight += 2.5D;
            evidencePoints.add(new WeightedEvidence(cluster2.center(), 2.5D));
        }

        double berryWeight = 0.0D;
        for (BerryCluster cluster2 : berryClusters) {
            berryWeight++;
            evidencePoints.add(new WeightedEvidence(cluster2.pos, 1.0D));
        }

        int suspicionLevel = (int) Math.round(
            vineWeight * 0.5D + kelpWeight * 0.4D + dripWeight * 0.75D + berryWeight * 1.0D);

        if (suspicionLevel > 0) {
            BlockPos estimatedSource = this.calculateWeightedSource(evidencePoints, chunkPos);
            boolean isExtreme = maxVineLength >= 100 || maxKelpHeight >= 50;
            boolean isSource  = maxVineLength >= 25  || maxKelpHeight >= 20;
            this.suspiciousChunks.put(chunkPos, new SuspiciousGrowthData(
                chunkPos, suspicionLevel, new ChunkPos(estimatedSource),
                isExtreme, isSource, maxVineLength, maxKelpHeight));
        } else {
            this.suspiciousChunks.remove(chunkPos);
        }
    }

    private List<VineCluster> detectTallGroundedVines(WorldChunk chunk, int xStart, int zStart) {
        List<VineCluster> clusters = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        int groundY = chunk.getBottomY();
        int tallClusters = 0;

        outer:
        for (int x = xStart; x < xStart + 16; x += 2) {
            for (int z = zStart; z < zStart + 16 && tallClusters < 2; z += 2) {
                int y = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(x - xStart, z - zStart);
                while (true) {
                    if (y < groundY) continue outer;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (visited.contains(pos)) { y--; continue; }
                    BlockState state = chunk.getBlockState(pos);
                    if (!this.isVineBlock(state.getBlock())) { y--; continue; }
                    List<BlockPos> positions = new ArrayList<>();
                    BlockPos currTrace = pos;
                    while (currTrace.getY() >= groundY && this.isVineBlock(chunk.getBlockState(currTrace).getBlock())) {
                        visited.add(currTrace);
                        positions.add(currTrace);
                        currTrace = currTrace.down();
                    }
                    if (positions.size() >= 6) {
                        clusters.add(new VineCluster(positions));
                        tallClusters++;
                    }
                    y = currTrace.getY() - 1;
                }
            }
        }
        return clusters;
    }

    private boolean isVineBlock(Block block) {
        return block instanceof VineBlock
            || block == Blocks.CAVE_VINES       || block == Blocks.CAVE_VINES_PLANT
            || block == Blocks.WEEPING_VINES    || block == Blocks.WEEPING_VINES_PLANT
            || block == Blocks.TWISTING_VINES   || block == Blocks.TWISTING_VINES_PLANT;
    }

    private List<KelpColumn> detectMaxGrownKelp(WorldChunk chunk, int xStart, int zStart) {
        List<KelpColumn> columns = new ArrayList<>();
        int bottomY = chunk.getBottomY();
        for (int x = xStart; x < xStart + 16; x++) {
            for (int z = zStart; z < zStart + 16; z++) {
                int oceanFloorY = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR).get(x - xStart, z - zStart);
                if (oceanFloorY > bottomY) {
                    int y = oceanFloorY + 1;
                    BlockPos basePos = new BlockPos(x, y, z);
                    BlockState baseState = chunk.getBlockState(basePos);
                    if (baseState.getBlock() instanceof KelpBlock || baseState.getBlock() instanceof KelpPlantBlock) {
                        int height = 0;
                        for (int currentY = y; currentY < 256; currentY++) {
                            BlockState s = chunk.getBlockState(new BlockPos(x, currentY, z));
                            if (!(s.getBlock() instanceof KelpBlock) && !(s.getBlock() instanceof KelpPlantBlock)) break;
                            height++;
                        }
                        if (height >= 4) columns.add(new KelpColumn(basePos, height));
                    }
                }
            }
        }
        return columns;
    }

    private List<DripstoneCluster> detectMaxDripstoneClusters(WorldChunk chunk, int xStart, int zStart) {
        List<DripstoneCluster> clusters = new ArrayList<>();
        for (int x = xStart; x < xStart + 16; x += 4) {
            for (int z = zStart; z < zStart + 16; z += 4) {
                int heightmapY = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(x - xStart, z - zStart);
                for (int y = -64; y <= heightmapY; y += 4) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (chunk.getBlockState(pos).getBlock() instanceof PointedDripstoneBlock) {
                        clusters.add(new DripstoneCluster(pos));
                        break;
                    }
                }
            }
        }
        return clusters;
    }

    private List<BerryCluster> detectMaxGrownBerries(WorldChunk chunk, int xStart, int zStart) {
        List<BerryCluster> clusters = new ArrayList<>();
        for (int x = xStart; x < xStart + 16; x += 2) {
            for (int z = zStart; z < zStart + 16; z += 2) {
                int heightmapY = chunk.getHeightmap(Heightmap.Type.MOTION_BLOCKING).get(x - xStart, z - zStart);
                for (int y = heightmapY; y >= heightmapY - 5; y--) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = chunk.getBlockState(pos);
                    if (state.getBlock() instanceof SweetBerryBushBlock) {
                        try {
                            if (state.get(SweetBerryBushBlock.AGE) == 3) clusters.add(new BerryCluster(pos));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return clusters;
    }

    private BlockPos calculateWeightedSource(List<WeightedEvidence> evidencePoints, ChunkPos chunkPos) {
        if (evidencePoints.isEmpty()) {
            return new BlockPos(chunkPos.getStartX() + 8, 30, chunkPos.getStartZ() + 8);
        }
        double sumX = 0.0D, sumY = 0.0D, sumZ = 0.0D, sumW = 0.0D;
        for (WeightedEvidence e : evidencePoints) {
            sumX += e.pos.getX() * e.weight;
            sumY += e.pos.getY() * e.weight;
            sumZ += e.pos.getZ() * e.weight;
            sumW += e.weight;
        }
        return new BlockPos((int)(sumX / sumW), (int)(sumY / sumW), (int)(sumZ / sumW));
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        boxesToRender.clear();
        if (suspiciousChunks.isEmpty()) return;

        long now = System.currentTimeMillis();
        // pulse: full cycle every 1500ms, value between 0 and 1
        float pulse = (float)(Math.sin((now % 1500L) / 1500.0 * Math.PI * 2.0) * 0.5 + 0.5);

        for (SuspiciousGrowthData data : suspiciousChunks.values()) {
            double sx = data.chunkPos.getStartX();
            double sz = data.chunkPos.getStartZ();

            Color baseColor;
            if (data.extreme)                                                   baseColor = EXTREME_PLATE_COLOR;
            else if (data.source)                                               baseColor = SOURCE_PLATE_COLOR;
            else if (data.suspicionLevel < 5 && data.maxVineLength < 4 && data.maxKelpHeight < 3)
                                                                                baseColor = LOW_SUSPICION_PLATE_COLOR;
            else                                                                baseColor = CHUNK_PLATE_COLOR;

            float animProg = chunkAnimProgress.getOrDefault(data.chunkPos, 1.0f);

            // main plate: pulsing alpha
            int baseAlpha  = baseColor.getAlpha();
            int pulseAlpha = Math.min(255, Math.max(0,
                (int)(baseAlpha * animProg * (0.55f + pulse * 0.45f))));
            Color fillColor    = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), pulseAlpha);
            Color outlineColor = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                Math.min(255, (int)(220 * animProg * (0.6f + pulse * 0.4f))));

            // plate at y=30 — height pulses slightly
            double plateH = PLATE_HEIGHT * animProg * (0.8f + pulse * 0.4f);
            boxesToRender.add(new RenderEntry(
                new Box(sx, 30, sz, sx + 16, 30 + plateH, sz + 16),
                fillColor, outlineColor, false));

            // source/extreme: add a vertical pulsing pillar
            if (data.source || data.extreme) {
                double pillarH = 20.0 * animProg * (0.5f + pulse * 0.5f);
                int pillarAlpha = Math.min(255, Math.max(0,
                    (int)(160 * animProg * (0.3f + pulse * 0.7f))));
                Color pillarFill    = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), pillarAlpha);
                Color pillarOutline = new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(),
                    Math.min(255, pillarAlpha + 60));
                boxesToRender.add(new RenderEntry(
                    new Box(sx + 5, 30, sz + 5, sx + 11, 30 + pillarH, sz + 11),
                    pillarFill, pillarOutline, true));
            }
        }
    }

    @Subscribe
    public void onRender2D(RenderEvent event) {
        if (boxesToRender.isEmpty()) return;
        Renderer2D r = event.renderer();

        for (RenderEntry entry : boxesToRender) {
            Box b         = entry.box();
            Color fill    = entry.fill();
            Color outline = entry.outline();

            Vec3d[] corners = {
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

            // filled faces
            for (int f = 0; f < 6; f++) {
                int[] idx = FACE_INDICES[f];
                Vec3d p1 = sc[idx[0]], p2 = sc[idx[1]], p3 = sc[idx[2]], p4 = sc[idx[3]];
                if (p1 != null && p2 != null && p3 != null && p4 != null) {
                    float sh = FACE_SHADING[f];
                    Color s2 = new Color(
                        Math.min(255, (int)(fill.getRed()   * sh)),
                        Math.min(255, (int)(fill.getGreen() * sh)),
                        Math.min(255, (int)(fill.getBlue()  * sh)),
                        fill.getAlpha());
                    r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y,
                           (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, s2.getRGB());
                }
            }

            // outline
            int oc = outline.getRGB();
            dl(r,sc[0],sc[1],oc); dl(r,sc[1],sc[2],oc); dl(r,sc[2],sc[3],oc); dl(r,sc[3],sc[0],oc);
            dl(r,sc[4],sc[5],oc); dl(r,sc[5],sc[6],oc); dl(r,sc[6],sc[7],oc); dl(r,sc[7],sc[4],oc);
            dl(r,sc[0],sc[4],oc); dl(r,sc[1],sc[5],oc); dl(r,sc[2],sc[6],oc); dl(r,sc[3],sc[7],oc);

            // pillar: extra diagonal glow lines from center base to top corners
            if (entry.isPillar()) {
                Vec3d cBot = ProjectionUtil.toScreen(new Vec3d(
                    (b.minX + b.maxX) / 2.0, b.minY, (b.minZ + b.maxZ) / 2.0));
                Vec3d t0 = ProjectionUtil.toScreen(new Vec3d(b.minX, b.maxY, b.minZ));
                Vec3d t1 = ProjectionUtil.toScreen(new Vec3d(b.maxX, b.maxY, b.minZ));
                Vec3d t2 = ProjectionUtil.toScreen(new Vec3d(b.maxX, b.maxY, b.maxZ));
                Vec3d t3 = ProjectionUtil.toScreen(new Vec3d(b.minX, b.maxY, b.maxZ));
                if (cBot != null) {
                    if (t0 != null) dl(r, cBot, t0, oc);
                    if (t1 != null) dl(r, cBot, t1, oc);
                    if (t2 != null) dl(r, cBot, t2, oc);
                    if (t3 != null) dl(r, cBot, t3, oc);
                }
            }
        }
    }

    private void dl(Renderer2D r, Vec3d a, Vec3d b, int c) {
        if (a != null && b != null)
            r.line((float)a.x, (float)a.y, (float)b.x, (float)b.y, 1.5f, c);
    }

    // ── Inner types ───────────────────────────────────────────────────────

    private record RenderEntry(Box box, Color fill, Color outline, boolean isPillar) {}

    private static class SuspiciousGrowthData {
        final ChunkPos chunkPos;
        final int      suspicionLevel;
        final ChunkPos baseChunk;
        final boolean  extreme;
        final boolean  source;
        final int      maxVineLength;
        final int      maxKelpHeight;

        SuspiciousGrowthData(ChunkPos chunkPos, int suspicionLevel, ChunkPos baseChunk,
                              boolean extreme, boolean source, int maxVineLength, int maxKelpHeight) {
            this.chunkPos       = chunkPos;
            this.suspicionLevel = suspicionLevel;
            this.baseChunk      = baseChunk;
            this.extreme        = extreme;
            this.source         = source;
            this.maxVineLength  = maxVineLength;
            this.maxKelpHeight  = maxKelpHeight;
        }
    }

    private static class VineCluster {
        final int length;
        final List<BlockPos> positions;

        VineCluster(List<BlockPos> p) {
            this.positions = p;
            this.length    = p.size();
        }

        BlockPos centroid() {
            long x = 0L, y = 0L, z = 0L;
            for (BlockPos p : this.positions) { x += p.getX(); y += p.getY(); z += p.getZ(); }
            return new BlockPos(
                (int)(x / this.positions.size()),
                (int)(y / this.positions.size()),
                (int)(z / this.positions.size()));
        }
    }

    private static class WeightedEvidence {
        final BlockPos pos;
        final double   weight;
        WeightedEvidence(BlockPos pos, double weight) { this.pos = pos; this.weight = weight; }
    }

    private static class KelpColumn {
        final BlockPos base;
        final int      height;
        KelpColumn(BlockPos b, int h) { this.base = b; this.height = h; }
        BlockPos center() { return this.base.up(this.height / 2); }
    }

    private static class DripstoneCluster {
        final BlockPos center;
        DripstoneCluster(BlockPos c) { this.center = c; }
        BlockPos center() { return this.center; }
    }

    private static class BerryCluster {
        final BlockPos pos;
        BerryCluster(BlockPos p) { this.pos = p; }
    }
}
