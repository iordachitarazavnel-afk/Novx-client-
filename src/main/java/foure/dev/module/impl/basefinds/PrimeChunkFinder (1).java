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
import foure.dev.util.render.text.FontRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@ModuleInfo(
    name = "PrimeChunkFinder",
    category = Category.RENDER,
    desc = "Detects suspicious chunks that might contain bases"
)
public class PrimeChunkFinder extends Function {

    private static final boolean USE_THREADS         = true;
    private static final int     THREAD_COUNT        = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
    private static final int     SCAN_DELAY_MS       = 100;
    private static final int     MAX_CONCURRENT_SCANS = 3;
    private static final int     DEEPSLATE_THRESHOLD = 3;
    private static final int     ROTATED_THRESHOLD   = 1;
    private static final long    RESET_INTERVAL_MS   = 500L;

    private final Set<ChunkPos>                              flaggedChunks   = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<ChunkPos, ChunkAnalysis> chunkData       = new ConcurrentHashMap<>();
    private final Set<ChunkPos>                              scannedChunks   = ConcurrentHashMap.newKeySet();
    private final Queue<ChunkPos>                            scanQueue       = new ConcurrentLinkedQueue<>();
    private final AtomicLong                                 activeScans     = new AtomicLong(0L);

    private final BooleanSetting ignorePlayerChunk  = new BooleanSetting("Ignore Player Chunk",    true);
    private final BooleanSetting showReasons        = new BooleanSetting("Show Reasons",            true);
    private final BooleanSetting detectItems        = new BooleanSetting("Check Items",             true);
    private final NumberSetting  maxItems           = new NumberSetting("Max Items",           this,  3.0, 0.0, 100.0, 1.0);
    private final BooleanSetting detectXP           = new BooleanSetting("Check XP Orbs",          true);
    private final NumberSetting  maxXP              = new NumberSetting("Max XP Orbs",         this,  3.0, 0.0, 100.0, 1.0);
    private final Map<ChunkPos, Integer>                     chunkItemCounts = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Integer>                     chunkXPCounts   = new ConcurrentHashMap<>();
    private final BooleanSetting alertCoords        = new BooleanSetting("Alert Coordinates",       true);
    private final NumberSetting  deepslateThreshold = new NumberSetting("Deepslate Limit",         this,  3.0, 1.0,  50.0, 1.0);
    private final NumberSetting  rotatedThreshold   = new NumberSetting("Rotated Deepslate Limit", this,  1.0, 1.0,  20.0, 1.0);

    private final boolean detectDeepslate       = false;
    private final boolean detectRotatedDeepslate = true;
    private final boolean detectLongDripstone   = true;
    private final int     minDripstoneLength    = 7;
    private final boolean detectFullVines       = true;
    private final int     minVineLength         = 30;
    private final boolean detectFullKelp        = true;
    private final int     minKelpLength         = 6;
    private final boolean detectDioriteVeins    = true;
    private final int     minDioriteVeinLength  = 5;
    private final boolean detectObsidianVeins   = true;
    private final int     minObsidianVeinLength = 15;
    private final int     minScanY              = -5;
    private final int     maxScanY              = 25;
    private final int     scanRadius            = 16;
    private final boolean chatAlerts            = true;
    private final boolean soundAlerts           = true;
    private final boolean toastAlerts           = true;

    private ChunkPos        lastPlayerChunk = null;
    private ExecutorService pool;
    private volatile boolean scanning      = false;
    private long             lastResetTime = 0L;

    // render
    private final List<RenderEntry> boxesToRender = new ArrayList<>();
    private static final int[][]  FACE_INDICES = {{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}};
    private static final float[]  FACE_SHADING = {0.5f,1.0f,0.7f,0.8f,0.6f,0.9f};

    public PrimeChunkFinder() {
        this.addSettings(new Setting[]{
            this.alertCoords, this.ignorePlayerChunk, this.showReasons,
            this.detectItems, this.maxItems, this.detectXP, this.maxXP,
            this.deepslateThreshold, this.rotatedThreshold
        });
    }

    // ── Reset ─────────────────────────────────────────────────────────────

    public void Reset() {
        this.scanning = false;
        if (this.pool != null) {
            this.pool.shutdownNow();
            this.pool = null;
        }
        this.scannedChunks.clear();
        this.chunkData.clear();
        this.scanQueue.clear();
        this.lastPlayerChunk = null;
        this.scanning = true;
        this.scannedChunks.clear();
        this.chunkData.clear();
        this.scanQueue.clear();
        this.lastPlayerChunk = null;
        this.pool = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public void onEnable() {
        super.onEnable();
        this.scanning = true;
        this.scannedChunks.clear();
        this.flaggedChunks.clear();
        this.chunkData.clear();
        this.scanQueue.clear();
        this.lastPlayerChunk = null;
        this.pool = Executors.newFixedThreadPool(THREAD_COUNT);
    }

    public void onDisable() {
        super.onDisable();
        this.scanning = false;
        if (this.pool != null) {
            this.pool.shutdownNow();
            this.pool = null;
        }
        this.scannedChunks.clear();
        this.flaggedChunks.clear();
        this.chunkData.clear();
        this.scanQueue.clear();
        this.lastPlayerChunk = null;
    }

    // ── Update (replaces onTick + onGameRender) ───────────────────────────

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();

        // count items and XP per chunk (was onTick)
        if (mc.world != null) {
            this.chunkItemCounts.clear();
            this.chunkXPCounts.clear();
            for (Entity entity : mc.world.getEntities()) {
                if (entity instanceof ItemEntity) {
                    this.chunkItemCounts.merge(entity.getChunkPos(), 1, Integer::sum);
                } else if (entity instanceof ExperienceOrbEntity) {
                    this.chunkXPCounts.merge(entity.getChunkPos(), 1, Integer::sum);
                }
            }
        }

        if (currentTime - this.lastResetTime >= RESET_INTERVAL_MS) {
            this.Reset();
            this.lastResetTime = currentTime;
        }

        if (mc.world == null) {
            this.scanning = false;
            if (this.pool != null) { this.pool.shutdownNow(); this.pool = null; }
            this.scannedChunks.clear(); this.flaggedChunks.clear();
            this.chunkData.clear(); this.scanQueue.clear();
            this.scanning = true;
            this.scannedChunks.clear(); this.flaggedChunks.clear();
            this.chunkData.clear(); this.scanQueue.clear();
            this.lastPlayerChunk = null;
            this.pool = Executors.newFixedThreadPool(THREAD_COUNT);
        }

        // was onGameRender
        if (mc.player != null && mc.world != null) {
            if (this.lastPlayerChunk == null && this.scanning) {
                int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
                int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
                this.lastPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
                this.buildBFSScanQueue(this.lastPlayerChunk);
                this.tryStartScans();
            }
            this.updateScanQueue();
            this.tryStartScans();
        }
    }

    // ── Scan queue ────────────────────────────────────────────────────────

    private void updateScanQueue() {
        if (mc.player == null) return;
        int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
        int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
        ChunkPos currentPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
        if (!currentPlayerChunk.equals(this.lastPlayerChunk)) {
            this.lastPlayerChunk = currentPlayerChunk;
            this.cleanupDistantChunks(currentPlayerChunk);
            this.scanQueue.clear();
            this.buildBFSScanQueue(currentPlayerChunk);
        }
    }

    private void cleanupDistantChunks(ChunkPos center) {
        int radius = 16;
        int cleanupRadius = radius + 2;
        this.scannedChunks.removeIf((chunk) -> {
            int dx = Math.abs(chunk.x - center.x);
            int dz = Math.abs(chunk.z - center.z);
            return dx > cleanupRadius || dz > cleanupRadius;
        });
    }

    private void buildBFSScanQueue(ChunkPos center) {
        int radius = 16;
        Set<ChunkPos> visited = new HashSet<>();
        Queue<ChunkPos> bfsQueue = new LinkedList<>();
        bfsQueue.offer(center);
        visited.add(center);
        while (!bfsQueue.isEmpty()) {
            ChunkPos current = bfsQueue.poll();
            if (!this.scannedChunks.contains(current)) {
                this.scanQueue.offer(current);
            }
            int[][] offsets = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
            for (int[] offset : offsets) {
                ChunkPos neighbor = new ChunkPos(current.x + offset[0], current.z + offset[1]);
                int dx = Math.abs(neighbor.x - center.x);
                int dz = Math.abs(neighbor.z - center.z);
                if (dx <= radius && dz <= radius && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    bfsQueue.offer(neighbor);
                }
            }
        }
    }

    private void tryStartScans() {
        if (this.scanning && mc.world != null && mc.player != null) {
            while (this.activeScans.get() < MAX_CONCURRENT_SCANS && !this.scanQueue.isEmpty()) {
                ChunkPos pos = this.scanQueue.poll();
                if (pos != null && !this.scannedChunks.contains(pos)) {
                    this.scannedChunks.add(pos);
                    Runnable task = () -> this.analyzeChunk(pos);
                    if (this.pool != null) {
                        this.pool.submit(this.wrapScanTask(task));
                    } else {
                        this.wrapScanTask(task).run();
                    }
                }
            }
        }
    }

    private Runnable wrapScanTask(Runnable task) {
        return () -> {
            this.activeScans.incrementAndGet();
            try {
                task.run();
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                this.activeScans.decrementAndGet();
            }
        };
    }

    // ── Chunk analysis ────────────────────────────────────────────────────

    private void analyzeChunk(ChunkPos pos) {
        if (mc.world == null) return;
        int startX = pos.getStartX();
        int startZ = pos.getStartZ();
        int minY = Math.max(minScanY, mc.world.getBottomY());
        int maxY = Math.min(maxScanY, mc.world.getBottomY() + mc.world.getHeight() - 1);
        ChunkAnalysis analysis = new ChunkAnalysis();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (!this.scanning) return;
                    BlockPos bp = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(bp);
                    this.analyzeBlock(bp, state, y, analysis);
                }
            }
        }

        BlockPos result;

        result = this.checkHasLongDripstone(pos);
        if (result != null) {
            analysis.hasLongDripstone = true;
            if (analysis.susBlockPos == null) analysis.susBlockPos = result;
        }

        result = this.checkHasLongVine(pos);
        if (result != null) {
            analysis.hasLongVine = true;
            if (analysis.susBlockPos == null) analysis.susBlockPos = result;
        }

        result = this.checkAllKelpFullyGrown(pos);
        if (result != null) {
            analysis.allKelpFull = true;
            if (analysis.susBlockPos == null) analysis.susBlockPos = result;
        }

        result = this.checkHasDioriteVein(pos);
        if (result != null) {
            analysis.hasDioriteVein = true;
            if (analysis.susBlockPos == null) analysis.susBlockPos = result;
        }

        result = this.checkHasObsidianVein(pos);
        if (result != null) {
            analysis.hasObsidianVein = true;
            if (analysis.susBlockPos == null) analysis.susBlockPos = result;
        }

        this.chunkData.put(pos, analysis);
        this.evaluateChunk(pos, analysis);
    }

    private void analyzeBlock(BlockPos pos, BlockState state, int worldY, ChunkAnalysis analysis) {
        if (this.isRotatedDeepslate(state)) {
            analysis.rotatedCount++;
            if (analysis.susBlockPos == null) analysis.susBlockPos = pos;
        }
    }

    // ── Detection helpers ─────────────────────────────────────────────────

    private BlockPos checkHasDioriteVein(ChunkPos chunkPos) {
        if (mc.world == null) return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int yMin = Math.max(mc.world.getBottomY(), -64);
        int yMax = mc.world.getBottomY() + mc.world.getHeight();
        Set<BlockPos> visited = new HashSet<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yMin; y < yMax; y++) {
                    if (!this.scanning) return null;
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (!visited.contains(pos)) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (this.isTargetBlock(state)) {
                            int lenUp   = this.countVerticalRun(pos, Direction.UP);
                            int lenDown = this.countVerticalRun(pos, Direction.DOWN);
                            int total   = lenUp + 1 + lenDown;
                            if (total >= minDioriteVeinLength) {
                                BlockPos start = pos.offset(Direction.DOWN, lenDown);
                                boolean enclosed = true;
                                for (int i = 0; i < total; i++) {
                                    BlockPos bp = start.offset(Direction.UP, i);
                                    visited.add(bp);
                                    if (!this.isEnclosedByStone(bp)) enclosed = false;
                                }
                                if (enclosed) return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos checkHasObsidianVein(ChunkPos chunkPos) {
        if (mc.world == null) return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int yMin = 15;
        int yMax = 63;
        Set<BlockPos> visited = new HashSet<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yMin; y <= yMax; y++) {
                    if (!this.scanning) return null;
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (!visited.contains(pos)) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (state.isOf(Blocks.OBSIDIAN)) {
                            int lenUp   = this.countVerticalRunObsidian(pos, Direction.UP);
                            int lenDown = this.countVerticalRunObsidian(pos, Direction.DOWN);
                            int total   = lenUp + 1 + lenDown;
                            if (total >= minObsidianVeinLength) {
                                BlockPos start = pos.offset(Direction.DOWN, lenDown);
                                boolean enclosed = true;
                                for (int i = 0; i < total; i++) {
                                    BlockPos bp = start.offset(Direction.UP, i);
                                    visited.add(bp);
                                    if (!this.isEnclosedByNonObsidian(bp)) enclosed = false;
                                }
                                if (enclosed) return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isTargetBlock(BlockState state) {
        return state.isOf(Blocks.DIORITE) || state.isOf(Blocks.POLISHED_DIORITE) || state.isOf(Blocks.GRANITE);
    }

    private int countVerticalRun(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        do {
            m.move(dir);
            if (!this.isTargetBlock(mc.world.getBlockState(m))) break;
            count++;
        } while (count <= 20);
        return count;
    }

    private int countVerticalRunObsidian(BlockPos from, Direction dir) {
        int count = 0;
        BlockPos.Mutable m = new BlockPos.Mutable(from.getX(), from.getY(), from.getZ());
        do {
            m.move(dir);
            if (!mc.world.getBlockState(m).isOf(Blocks.OBSIDIAN)) break;
            count++;
        } while (count <= 20);
        return count;
    }

    private boolean isEnclosedByStone(BlockPos pos) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontalDirections) {
            BlockPos adj = pos.offset(d);
            BlockState st = mc.world.getBlockState(adj);
            if (!st.isOf(Blocks.STONE)) return false;
        }
        return true;
    }

    private boolean isEnclosedByNonObsidian(BlockPos pos) {
        Direction[] horizontalDirections = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontalDirections) {
            BlockPos adj = pos.offset(d);
            BlockState st = mc.world.getBlockState(adj);
            if (st.isOf(Blocks.OBSIDIAN)) return false;
        }
        return true;
    }

    private BlockPos checkHasLongDripstone(ChunkPos chunkPos) {
        if (mc.world == null) return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getBottomY() + mc.world.getHeight() - 1;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMaxY; y >= worldMinY; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    BlockState state = mc.world.getBlockState(pos);
                    if (state.isOf(Blocks.POINTED_DRIPSTONE) && this.isTopOfDripstone(pos, state)) {
                        int length = 1;
                        BlockPos current = pos.down();
                        while (current.getY() >= worldMinY && length < 50) {
                            BlockState currentState = mc.world.getBlockState(current);
                            if (!currentState.isOf(Blocks.POINTED_DRIPSTONE)
                                    || !currentState.contains(Properties.VERTICAL_DIRECTION)
                                    || currentState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
                                break;
                            }
                            length++;
                            current = current.down();
                        }
                        if (length >= minDripstoneLength) return pos;
                    }
                }
            }
        }
        return null;
    }

    private boolean isTopOfDripstone(BlockPos pos, BlockState state) {
        if (!state.isOf(Blocks.POINTED_DRIPSTONE)) return false;
        if (!state.contains(Properties.VERTICAL_DIRECTION)) return false;
        if (state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) return false;
        BlockPos above = pos.up();
        BlockState aboveState = mc.world.getBlockState(above);
        return !aboveState.isOf(Blocks.POINTED_DRIPSTONE);
    }

    private int measureDripstoneLength(BlockPos startPos) {
        if (mc.world == null) return 0;
        BlockState startState = mc.world.getBlockState(startPos);
        if (!startState.isOf(Blocks.POINTED_DRIPSTONE)) return 0;
        if (!startState.contains(Properties.VERTICAL_DIRECTION)) return 0;
        if (startState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) return 0;
        int length = 1;
        BlockPos current = startPos.down();
        while (length < 30) {
            BlockState state = mc.world.getBlockState(current);
            if (!state.isOf(Blocks.POINTED_DRIPSTONE)
                    || !state.contains(Properties.VERTICAL_DIRECTION)
                    || state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
                break;
            }
            length++;
            current = current.down();
        }
        return length;
    }

    private BlockPos checkHasLongVine(ChunkPos chunkPos) {
        if (mc.world == null) return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        Set<BlockPos> processedVineTops = ConcurrentHashMap.newKeySet();
        int scanTopY = Math.min(mc.world.getBottomY() + mc.world.getHeight() - 1, 320);

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = scanTopY; y >= 40; y--) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (!processedVineTops.contains(pos)) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (state.isOf(Blocks.VINE)) {
                            BlockPos topPos = pos.up();
                            BlockState topState = mc.world.getBlockState(topPos);
                            boolean isVineTop = !topState.isOf(Blocks.VINE)
                                && (topState.isSolidBlock(mc.world, topPos) || !topState.isOpaque());
                            if (isVineTop) {
                                processedVineTops.add(pos);
                                int vineLength = 1;
                                BlockPos current = pos.down();
                                while (current.getY() >= Math.max(mc.world.getBottomY(), 40)) {
                                    BlockState currentState = mc.world.getBlockState(current);
                                    if (!currentState.isOf(Blocks.VINE)) break;
                                    vineLength++;
                                    current = current.down();
                                }
                                if (vineLength >= minVineLength) return pos;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private BlockPos checkAllKelpFullyGrown(ChunkPos chunkPos) {
        if (mc.world == null) return null;
        int startX = chunkPos.getStartX();
        int startZ = chunkPos.getStartZ();
        int kelpPlantsFound = 0;
        int fullKelpPlants = 0;
        BlockPos firstKelpPos = null;
        Set<BlockPos> processedKelpBases = ConcurrentHashMap.newKeySet();
        int worldMinY = mc.world.getBottomY();
        int worldMaxY = mc.world.getBottomY() + mc.world.getHeight() - 1;

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = worldMinY; y <= worldMaxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    if (!processedKelpBases.contains(pos)) {
                        BlockState state = mc.world.getBlockState(pos);
                        if (state.isOf(Blocks.KELP) || state.isOf(Blocks.KELP_PLANT)) {
                            BlockPos belowPos = pos.down();
                            BlockState belowState = mc.world.getBlockState(belowPos);
                            boolean isKelpBase = !belowState.isOf(Blocks.KELP) && !belowState.isOf(Blocks.KELP_PLANT);
                            if (isKelpBase) {
                                processedKelpBases.add(pos);
                                if (firstKelpPos == null) firstKelpPos = pos;
                                BlockPos current = pos.up();
                                boolean reachedWaterSurface = false;
                                int kelpLength = 1;
                                while (current.getY() <= worldMaxY) {
                                    BlockState currentState = mc.world.getBlockState(current);
                                    if (!currentState.isOf(Blocks.KELP) && !currentState.isOf(Blocks.KELP_PLANT)) {
                                        if (currentState.getFluidState().isEmpty()) reachedWaterSurface = true;
                                        break;
                                    }
                                    kelpLength++;
                                    current = current.up();
                                }
                                if (kelpLength >= minKelpLength || !reachedWaterSurface) {
                                    kelpPlantsFound++;
                                    if (reachedWaterSurface) fullKelpPlants++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (kelpPlantsFound < 10) return null;
        boolean allFull = kelpPlantsFound == fullKelpPlants;
        return allFull ? firstKelpPos : null;
    }

    private void evaluateChunk(ChunkPos pos, ChunkAnalysis analysis) {
        boolean suspicious = false;
        List<String> reasonList = new ArrayList<>();

        if ((Boolean) this.ignorePlayerChunk.getValue() && pos.equals(this.lastPlayerChunk)) {
            this.flaggedChunks.remove(pos);
            return;
        }

        if ((Boolean) this.detectItems.getValue()) {
            int itemCount = this.chunkItemCounts.getOrDefault(pos, 0);
            if (itemCount > (int) this.maxItems.getValueFloat()) {
                this.flaggedChunks.remove(pos);
                return;
            }
        }

        if ((Boolean) this.detectXP.getValue()) {
            int xpCount = this.chunkXPCounts.getOrDefault(pos, 0);
            if (xpCount > (int) this.maxXP.getValueFloat()) {
                this.flaggedChunks.remove(pos);
                return;
            }
        }

        if (analysis.rotatedCount >= (int) this.rotatedThreshold.getValueFloat()) {
            suspicious = true;
            reasonList.add("Rotated: " + analysis.rotatedCount);
        }
        if (analysis.hasLongDripstone) { suspicious = true; reasonList.add("Long Dripstone"); }
        if (analysis.hasLongVine)      { suspicious = true; reasonList.add("Long Vine"); }
        if (analysis.allKelpFull)      { suspicious = true; reasonList.add("Grown Kelp"); }
        if (analysis.hasDioriteVein)   { suspicious = true; reasonList.add("Diorite Vein"); }
        if (analysis.hasObsidianVein)  { suspicious = true; reasonList.add("Obsidian Vein"); }

        analysis.reasons = reasonList;

        if (suspicious) {
            if (this.flaggedChunks.add(pos)) {
                StringBuilder reasons = new StringBuilder();
                for (String reason : reasonList) reasons.append(reason).append(" ");

                int susBlockX = 0;
                int susBlockZ = 0;
                if (analysis.susBlockPos != null) {
                    susBlockX = analysis.susBlockPos.getX();
                    susBlockZ = analysis.susBlockPos.getZ();
                }

                final int finalX = susBlockX;
                final int finalZ = susBlockZ;

                if (mc.player != null) {
                    mc.player.sendMessage(Text.literal("§e[PrimeChunkFinder] §fSuspicious Chunk - " + reasons), false);
                }

                mc.execute(() -> {
                    if (mc.player == null) return;
                    if ((Boolean) this.alertCoords.getValue()) {
                        mc.player.sendMessage(Text.literal("§e[PrimeChunkFinder] §fX: " + finalX + " Z: " + finalZ), false);
                    } else {
                        mc.player.sendMessage(Text.literal("§e[PrimeChunkFinder] §fSuspicious Chunk Detected"), false);
                    }
                });
            }
        } else {
            this.flaggedChunks.remove(pos);
        }
    }

    private boolean isNormalDeepslate(BlockState state) {
        return state.isOf(Blocks.DEEPSLATE);
    }

    private boolean isRotatedDeepslate(BlockState state) {
        if (!state.isOf(Blocks.DEEPSLATE)) return false;
        if (!state.contains(Properties.AXIS)) return false;
        Direction.Axis axis = state.get(Properties.AXIS);
        if (axis == Direction.Axis.Y) return false;
        return true;
    }

    // ── Render ────────────────────────────────────────────────────────────

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        boxesToRender.clear();
        if (this.flaggedChunks.isEmpty()) return;
        int rendered = 0;
        int renderY = 63;
        for (ChunkPos pos : this.flaggedChunks) {
            if (rendered++ >= 50) break;
            double startX = pos.getStartX();
            double startZ = pos.getStartZ();
            double endX   = pos.getEndX() + 1;
            double endZ   = pos.getEndZ() + 1;
            Color c = new Color(0, 255, 0, 70);
            ChunkAnalysis analysis = this.chunkData.get(pos);
            boxesToRender.add(new RenderEntry(new Box(startX, renderY, startZ, endX, renderY + 0.5, endZ), c, analysis));
        }
    }

    @Subscribe
    public void onRender2D(RenderEvent event) {
        if (boxesToRender.isEmpty()) return;
        Renderer2D r = event.renderer();
        for (RenderEntry entry : boxesToRender) {
            Box b = entry.box();
            Color fill    = entry.color();
            Color outline = new Color(0, 255, 0, 200);

            Vec3d[] corners = {
                new Vec3d(b.minX,b.minY,b.minZ), new Vec3d(b.maxX,b.minY,b.minZ),
                new Vec3d(b.maxX,b.minY,b.maxZ), new Vec3d(b.minX,b.minY,b.maxZ),
                new Vec3d(b.minX,b.maxY,b.minZ), new Vec3d(b.maxX,b.maxY,b.minZ),
                new Vec3d(b.maxX,b.maxY,b.maxZ), new Vec3d(b.minX,b.maxY,b.maxZ)
            };
            Vec3d[] sc = new Vec3d[8]; int on = 0;
            for (int i = 0; i < 8; i++) { sc[i] = ProjectionUtil.toScreen(corners[i]); if (sc[i] != null) on++; }
            if (on == 0) continue;

            for (int f = 0; f < 6; f++) {
                int[] idx = FACE_INDICES[f];
                Vec3d p1=sc[idx[0]],p2=sc[idx[1]],p3=sc[idx[2]],p4=sc[idx[3]];
                if (p1!=null&&p2!=null&&p3!=null&&p4!=null) {
                    float sh = FACE_SHADING[f];
                    Color s2 = new Color(
                        Math.min(255,(int)(fill.getRed()*sh)),
                        Math.min(255,(int)(fill.getGreen()*sh)),
                        Math.min(255,(int)(fill.getBlue()*sh)),
                        fill.getAlpha());
                    r.quad((float)p1.x,(float)p1.y,(float)p2.x,(float)p2.y,
                           (float)p3.x,(float)p3.y,(float)p4.x,(float)p4.y,s2.getRGB());
                }
            }
            int oc = outline.getRGB();
            dl(r,sc[0],sc[1],oc); dl(r,sc[1],sc[2],oc); dl(r,sc[2],sc[3],oc); dl(r,sc[3],sc[0],oc);
            dl(r,sc[4],sc[5],oc); dl(r,sc[5],sc[6],oc); dl(r,sc[6],sc[7],oc); dl(r,sc[7],sc[4],oc);
            dl(r,sc[0],sc[4],oc); dl(r,sc[1],sc[5],oc); dl(r,sc[2],sc[6],oc); dl(r,sc[3],sc[7],oc);

            // show reasons
            if ((Boolean) this.showReasons.getValue() && entry.analysis() != null
                    && entry.analysis().reasons != null && !entry.analysis().reasons.isEmpty()) {
                Vec3d center3D = new Vec3d(
                    (b.minX + b.maxX) / 2.0,
                    b.maxY + 2.0,
                    (b.minZ + b.maxZ) / 2.0);
                Vec3d screenCenter = ProjectionUtil.toScreen(center3D);
                if (screenCenter != null) {
                    float ty = (float) screenCenter.y;
                    for (String reason : entry.analysis().reasons) {
                        float tw = r.getStringWidth(FontRegistry.INTER_MEDIUM, reason, 8f);
                        r.text(FontRegistry.INTER_MEDIUM,
                            (float) screenCenter.x - tw / 2f, ty,
                            8f, reason, new Color(255, 255, 255, 220).getRGB());
                        ty += 10f;
                    }
                }
            }
        }
    }

    private void dl(Renderer2D r, Vec3d a, Vec3d b, int c) {
        if (a != null && b != null) r.line((float)a.x,(float)a.y,(float)b.x,(float)b.y,1.5f,c);
    }

    // ── Inner types ───────────────────────────────────────────────────────

    private record RenderEntry(Box box, Color color, ChunkAnalysis analysis) {}

    private static class ChunkAnalysis {
        int  deepslateCount    = 0;
        int  rotatedCount      = 0;
        boolean hasLongDripstone = false;
        boolean hasLongVine      = false;
        boolean allKelpFull      = false;
        boolean hasDioriteVein   = false;
        boolean hasObsidianVein  = false;
        List<String> reasons     = new ArrayList<>();
        BlockPos     susBlockPos = null;
    }

    private enum SuspiciousType {
        DEEPSLATE,
        ROTATED_DEEPSLATE,
        LONG_DRIPSTONE,
        LONG_VINE,
        FULL_KELP,
        DIORITE_VEIN,
        OBSIDIAN_VEIN
    }
}
