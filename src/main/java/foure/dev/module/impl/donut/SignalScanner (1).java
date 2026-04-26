package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ModuleInfo(
    name = "SignalScanner",
    category = Category.DONUT,
    desc = "Detectează semnale anomale în chunk-uri depărtate pentru a găsi baze. Suportă auto-discover, high-entity highlight și search area."
)
public class SignalScanner extends Function {

    // General
    private final NumberSetting  serverRadius  = new NumberSetting("Server Render Radius", this, 4,  1, 16, 1);
    private final NumberSetting  scanRadius    = new NumberSetting("Scan Radius",          this, 12, 5, 32, 1);
    private final NumberSetting  scanInterval  = new NumberSetting("Scan Interval",        this, 20, 1, 100, 1);
    private final BooleanSetting chatNotify    = new BooleanSetting("Chat Notify",         true);
    private final BooleanSetting playSound     = new BooleanSetting("Play Sound",          true);
    private final BooleanSetting autoDiscover  = new BooleanSetting("Auto Discover",       false);
    // Render
    private final BooleanSetting renderSignals = new BooleanSetting("Render",              true);
    private final BooleanSetting fullHeight    = new BooleanSetting("Full Height",         true);
    private final NumberSetting  yPadding      = new NumberSetting("Y Padding",  this, 2,  0, 32, 1);
    private final BooleanSetting highEntity    = new BooleanSetting("High Entity Chunk",   false);
    private final NumberSetting  highEntityRadius = new NumberSetting("Entity Radius", this, 2, 1, 5, 1);
    private final NumberSetting  highEntityY   = new NumberSetting("Entity Y",    this, 0, -64, 320, 1);
    private final BooleanSetting renderSearchArea = new BooleanSetting("Render Search Area", false);
    private final BooleanSetting clearOnEntry  = new BooleanSetting("Clear On Entry",      true);

    // Vanilla blocks care sunt ascunse de Anti-Xray (target principal)
    private static final Block[] AUTO_DISCOVER_CANDIDATES = {
        Blocks.ANCIENT_DEBRIS, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
        Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE, Blocks.GOLD_ORE,
        Blocks.DEEPSLATE_GOLD_ORE, Blocks.IRON_ORE, Blocks.COPPER_ORE,
        Blocks.SPAWNER, Blocks.CHEST
    };

    private volatile Map<ChunkPos, Signal> signals = Collections.emptyMap();
    private final Set<Long> announcedSignalKeys  = ConcurrentHashMap.newKeySet();
    private final Set<Long> announcedDiscoverKeys = ConcurrentHashMap.newKeySet();
    private final Set<Long> seenNearby           = ConcurrentHashMap.newKeySet();
    private final Set<Long> clearedSearchChunks  = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean scanRunning      = new AtomicBoolean(false);
    private ExecutorService executor;
    private int tickCounter = 0;

    public SignalScanner() {
        this.addSettings(new Setting[]{
            serverRadius, scanRadius, scanInterval, chatNotify, playSound, autoDiscover,
            renderSignals, fullHeight, yPadding,
            highEntity, highEntityRadius, highEntityY,
            renderSearchArea, clearOnEntry
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        signals = Collections.emptyMap();
        announcedSignalKeys.clear();
        announcedDiscoverKeys.clear();
        seenNearby.clear();
        clearedSearchChunks.clear();
        tickCounter = 0;
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "SignalScanner");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (executor != null) { executor.shutdownNow(); executor = null; }
        signals = Collections.emptyMap();
        announcedSignalKeys.clear();
        announcedDiscoverKeys.clear();
        seenNearby.clear();
        clearedSearchChunks.clear();
    }

    // ─── tick ────────────────────────────────────────────────────────────────

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        int serverR = serverRadius.getValueInt();
        int pcx = mc.player.getChunkPos().x;
        int pcz = mc.player.getChunkPos().z;

        if ((Boolean) clearOnEntry.getValue())
            clearedSearchChunks.add(chunkKey(pcx, pcz));

        for (int cx = -serverR; cx <= serverR; cx++)
            for (int cz = -serverR; cz <= serverR; cz++)
                seenNearby.add(chunkKey(pcx + cx, pcz + cz));

        if (++tickCounter >= scanInterval.getValueInt()) {
            tickCounter = 0;
            dispatchScan();
        }
    }

    // ─── scan dispatch ───────────────────────────────────────────────────────

    private void dispatchScan() {
        if (executor == null || executor.isShutdown()) return;
        if (!scanRunning.compareAndSet(false, true)) return;

        ChunkPos playerChunk = mc.player.getChunkPos();
        int serverR = serverRadius.getValueInt();
        int scanR   = Math.max(scanRadius.getValueInt(), serverR + 1);
        int worldBottom = mc.world.getBottomY();

        Set<Block> signalSet = buildSignalSet();
        Set<Block> scanSet   = buildScanSet(signalSet);

        List<WorldChunk> chunks = new ArrayList<>();
        for (int cx = -scanR; cx <= scanR; cx++) {
            for (int cz = -scanR; cz <= scanR; cz++) {
                if (Math.max(Math.abs(cx), Math.abs(cz)) > serverR) {
                    int wx = playerChunk.x + cx;
                    int wz = playerChunk.z + cz;
                    if (!seenNearby.contains(chunkKey(wx, wz))) {
                        WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(wx, wz);
                        if (chunk != null && !chunk.isEmpty()) chunks.add(chunk);
                    }
                }
            }
        }

        executor.submit(() -> {
            try {
                runScan(chunks, playerChunk, worldBottom, signalSet, scanSet);
            } finally {
                scanRunning.set(false);
            }
        });
    }

    // ─── scan logic ──────────────────────────────────────────────────────────

    private void runScan(List<WorldChunk> chunks, ChunkPos playerChunk, int worldBottom,
                         Set<Block> signalSet, Set<Block> scanSet) {
        Map<ChunkPos, Signal> signalChunks = new HashMap<>();
        Map<ChunkPos, List<Block>> discoverHits = new HashMap<>();

        for (WorldChunk chunk : chunks) {
            ChunkSection[] sections = chunk.getSectionArray();
            Map<Block, Integer> counts = new HashMap<>();
            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

            for (int i = 0; i < sections.length; i++) {
                ChunkSection sec = sections[i];
                if (sec == null || sec.isEmpty()) continue;
                // quick palette check
                boolean hasCandidate = false;
                for (Block b : scanSet) {
                    if (sec.hasAny(s -> s.getBlock() == b)) { hasCandidate = true; break; }
                }
                if (!hasCandidate) continue;

                int baseY = worldBottom + (i << 4);
                for (int dy = 0; dy < 16; dy++)
                    for (int dx = 0; dx < 16; dx++)
                        for (int dz = 0; dz < 16; dz++) {
                            Block b = sec.getBlockState(dx, dy, dz).getBlock();
                            if (scanSet.contains(b)) {
                                int wy = baseY + dy;
                                counts.merge(b, 1, Integer::sum);
                                if (wy < minY) minY = wy;
                                if (wy > maxY) maxY = wy;
                            }
                        }
            }

            if (counts.isEmpty()) continue;

            ChunkPos cp = chunk.getPos();
            int cdx = cp.x - playerChunk.x;
            int cdz = cp.z - playerChunk.z;
            int chunkDist = Math.max(Math.abs(cdx), Math.abs(cdz));

            boolean hasSignal = counts.keySet().stream().anyMatch(signalSet::contains);
            if (hasSignal) {
                Map<Block, Integer> signalCounts = new HashMap<>(counts);
                signalCounts.keySet().retainAll(signalSet);
                signalChunks.put(cp, new Signal(cp, minY, maxY, chunkDist, signalCounts));
            }

            if ((Boolean) autoDiscover.getValue()) {
                List<Block> discovered = new ArrayList<>();
                for (Block b : counts.keySet())
                    if (!signalSet.contains(b)) discovered.add(b);
                if (!discovered.isEmpty()) discoverHits.put(cp, discovered);
            }
        }

        // merge and notify on main thread
        Map<ChunkPos, Signal> merged = new HashMap<>(signals);
        List<Signal> newSignals = new ArrayList<>();
        for (Signal s : signalChunks.values()) {
            if (merged.put(s.pos, s) == null && announcedSignalKeys.add(chunkKey(s.pos.x, s.pos.z)))
                newSignals.add(s);
        }
        signals = merged;

        List<String[]> newDiscoveries = new ArrayList<>();
        for (Map.Entry<ChunkPos, List<Block>> entry : discoverHits.entrySet()) {
            ChunkPos cp = entry.getKey();
            for (Block b : entry.getValue()) {
                long key = chunkKey(cp.x, cp.z) ^ Registries.BLOCK.getRawId(b);
                if (announcedDiscoverKeys.add(key)) {
                    String blockId = Registries.BLOCK.getId(b).toString();
                    newDiscoveries.add(new String[]{"[" + cp.x + ", " + cp.z + "]", blockId});
                }
            }
        }

        if ((Boolean) chatNotify.getValue() && (!newSignals.isEmpty() || !newDiscoveries.isEmpty())) {
            mc.execute(() -> announce(newSignals, newDiscoveries));
        }
    }

    private void announce(List<Signal> newSignals, List<String[]> newDiscoveries) {
        if (!isToggled() || mc.world == null || mc.player == null) return;

        if (!newSignals.isEmpty() && (Boolean) playSound.getValue())
            mc.world.playSound(mc.player, mc.player.getBlockPos(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.MASTER, 1f, 1f);

        for (Signal s : newSignals) {
            int intensity = s.blockCounts.values().stream().mapToInt(Integer::intValue).sum();
            sendMessage("§cSignal §7» §fchunk §7[" + s.pos.x + ", " + s.pos.z + "] §e— Intensity: " + intensity + " §7(" + s.chunkDist + " chunks out)");
        }
        for (String[] d : newDiscoveries) {
            int freq = d[1].hashCode() & 0xFFFF;
            sendMessage("§eNew trace: §f#" + String.format("%04X", freq) + " §7in chunk §f" + d[0]);
        }
    }

    // ─── render ──────────────────────────────────────────────────────────────

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (mc.world == null) return;
        Map<ChunkPos, Signal> current = signals;
        if (current.isEmpty()) return;

        double camX = event.getCamera().getPos().x;
        double camY = event.getCamera().getPos().y;
        double camZ = event.getCamera().getPos().z;
        Matrix4f matrix = event.getMatrix();

        int worldBottom = mc.world.getBottomY();
        int worldTop    = worldBottom + mc.world.getHeight();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tess = Tessellator.getInstance();

        // ── signal boxes ──
        if ((Boolean) renderSignals.getValue()) {
            BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            boolean full = (Boolean) fullHeight.getValue();
            double pad = yPadding.getValueFloat();

            for (Signal s : current.values()) {
                double x1 = (s.pos.x << 4) - camX;
                double z1 = (s.pos.z << 4) - camZ;
                double x2 = x1 + 16;
                double z2 = z1 + 16;
                double y1 = (full ? worldBottom : s.minY - pad) - camY;
                double y2 = (full ? worldTop    : s.maxY + 1 + pad) - camY;
                drawBoxEdges(buf, matrix, x1, y1, z1, x2, y2, z2, 255, 50, 50, 220);
            }
            BuiltBuffer built = buf.endNullable();
            if (built != null) BufferRenderer.drawWithGlobalProgram(built);
        }

        // ── high entity chunk ──
        if ((Boolean) highEntity.getValue()) {
            Map<ChunkPos, Integer> entityCounts = new HashMap<>();
            for (Entity e : mc.world.getEntities())
                entityCounts.merge(e.getChunkPos(), 1, Integer::sum);

            int eRadius = highEntityRadius.getValueInt();
            double ey = highEntityY.getValueFloat() - camY;
            Set<ChunkPos> uniqueHighChunks = new HashSet<>();

            for (Signal s : current.values()) {
                ChunkPos best = null;
                int maxCount = -1;
                for (int cx = -eRadius; cx <= eRadius; cx++) {
                    for (int cz = -eRadius; cz <= eRadius; cz++) {
                        ChunkPos p = new ChunkPos(s.pos.x + cx, s.pos.z + cz);
                        int count = entityCounts.getOrDefault(p, 0);
                        if (count > maxCount) { maxCount = count; best = p; }
                    }
                }
                if (best != null) uniqueHighChunks.add(best);
            }

            BufferBuilder buf2 = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (ChunkPos cp : uniqueHighChunks) {
                double x1 = (cp.x << 4) - camX, z1 = (cp.z << 4) - camZ;
                drawBoxEdges(buf2, matrix, x1, ey, z1, x1 + 16, ey + 0.1, z1 + 16, 120, 0, 255, 200);
            }
            BuiltBuffer built2 = buf2.endNullable();
            if (built2 != null) BufferRenderer.drawWithGlobalProgram(built2);
        }

        // ── search area ──
        if ((Boolean) renderSearchArea.getValue()) {
            int eRadius = highEntityRadius.getValueInt();
            double ey = highEntityY.getValueFloat() - camY;
            Set<ChunkPos> searchChunks = new HashSet<>();
            for (Signal s : current.values())
                for (int cx = -eRadius; cx <= eRadius; cx++)
                    for (int cz = -eRadius; cz <= eRadius; cz++) {
                        ChunkPos cp = new ChunkPos(s.pos.x + cx, s.pos.z + cz);
                        if (!(Boolean) clearOnEntry.getValue() || !clearedSearchChunks.contains(chunkKey(cp.x, cp.z)))
                            searchChunks.add(cp);
                    }

            BufferBuilder buf3 = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            for (ChunkPos cp : searchChunks) {
                double x1 = (cp.x << 4) - camX, z1 = (cp.z << 4) - camZ;
                double sy1 = (double) worldBottom - camY;
                double sy2 = (double) worldTop - camY;
                drawBoxEdges(buf3, matrix, x1, sy1, z1, x1 + 16, sy2, z1 + 16, 200, 200, 200, 60);
            }
            BuiltBuffer built3 = buf3.endNullable();
            if (built3 != null) BufferRenderer.drawWithGlobalProgram(built3);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private Set<Block> buildSignalSet() {
        Set<Block> set = new HashSet<>();
        set.add(Blocks.ANCIENT_DEBRIS); // target principal pe anarchy
        return set;
    }

    private Set<Block> buildScanSet(Set<Block> signalSet) {
        if (!(Boolean) autoDiscover.getValue()) return signalSet;
        Set<Block> full = new HashSet<>(signalSet);
        full.addAll(Arrays.asList(AUTO_DISCOVER_CANDIDATES));
        return full;
    }

    private static long chunkKey(int x, int z) {
        return (long) x << 32 | (long) z & 0xFFFFFFFFL;
    }

    public int getSignalCount() { return signals.size(); }

    // ─── render util ─────────────────────────────────────────────────────────

    private static void drawBoxEdges(BufferBuilder buf, Matrix4f mat,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     int r, int g, int b, int a) {
        float ax = (float)x1, bx = (float)x2;
        float ay = (float)y1, by = (float)y2;
        float az = (float)z1, bz = (float)z2;
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
        final ChunkPos pos;
        final int minY, maxY, chunkDist;
        final Map<Block, Integer> blockCounts;
        Signal(ChunkPos pos, int minY, int maxY, int chunkDist, Map<Block, Integer> blockCounts) {
            this.pos = pos; this.minY = minY; this.maxY = maxY;
            this.chunkDist = chunkDist; this.blockCounts = blockCounts;
        }
    }
}
