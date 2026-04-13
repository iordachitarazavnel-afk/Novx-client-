package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ModuleInfo(
    name = "ColonFinder",
    category = Category.RENDER,
    desc = "Pattern-based block column detector with beams"
)
public class ColonFinder extends Function {

    private final NumberSetting searchRadius    = new NumberSetting("Search Radius",    this, 3.0,  1.0, 8.0,   1.0);
    private final NumberSetting updateInterval  = new NumberSetting("Update Interval",  this, 20.0, 5.0, 100.0, 1.0);
    private final NumberSetting minColumnHeight = new NumberSetting("Min Column Height",this, 4.0,  2.0, 10.0,  1.0);
    private final NumberSetting beamHeight      = new NumberSetting("Beam Height",      this, 64.0, 8.0, 512.0, 1.0);
    private final BooleanSetting notifyEnabled  = new BooleanSetting("Notify",    true);
    private final BooleanSetting beamEnabled    = new BooleanSetting("Beam",      true);
    private final ColorSetting   fillColor      = new ColorSetting("Fill Color",   this, new Color(255, 105, 180,  80));
    private final ColorSetting   outlineColor   = new ColorSetting("Outline Color",this, new Color(255,  20, 147, 255));
    private final ColorSetting   beamColor      = new ColorSetting("Beam Color",   this, new Color(0,   220,   0,  60));

    // default patterns: stone/granite/diorite/andesite surrounded by their natural neighbors
    private static final List<BlockPattern> PATTERNS = List.of(
        new BlockPattern(Blocks.STONE,    Set.of(Blocks.ANDESITE, Blocks.GRANITE, Blocks.DIORITE, Blocks.GRAVEL, Blocks.DIRT)),
        new BlockPattern(Blocks.GRANITE,  Set.of(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRAVEL, Blocks.DIRT, Blocks.STONE)),
        new BlockPattern(Blocks.DIORITE,  Set.of(Blocks.GRANITE, Blocks.ANDESITE, Blocks.GRAVEL, Blocks.DIRT, Blocks.STONE)),
        new BlockPattern(Blocks.ANDESITE, Set.of(Blocks.GRANITE, Blocks.DIORITE, Blocks.GRAVEL, Blocks.DIRT, Blocks.STONE)),
        new BlockPattern(Blocks.GRAVEL,   Set.of(Blocks.GRANITE, Blocks.DIORITE, Blocks.DIRT, Blocks.ANDESITE, Blocks.STONE))
    );

    private volatile List<BlockPos> renderList  = new ArrayList<>();
    private volatile List<BlockPos> beamPosList = new ArrayList<>();
    private final    List<BlockPos> scanResult  = new ArrayList<>();
    private final Set<Long>         notified    = new HashSet<>();
    private final ExecutorService   executor    = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ColonScanner"); t.setDaemon(true); return t;
    });
    private final AtomicBoolean scanning = new AtomicBoolean(false);
    private Future<?>  scanFuture;
    private int        tickCounter      = 0;
    private int        snapPlayerChunkX = 0;
    private int        snapPlayerChunkZ = 0;

    public ColonFinder() {
        this.addSettings(new Setting[]{
            this.searchRadius, this.updateInterval, this.minColumnHeight, this.beamHeight,
            this.notifyEnabled, this.beamEnabled,
            this.fillColor, this.outlineColor, this.beamColor
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        renderList.clear(); beamPosList.clear(); notified.clear(); tickCounter = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (scanFuture != null) scanFuture.cancel(true);
        scanning.set(false);
        renderList.clear(); beamPosList.clear(); notified.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (++tickCounter < (int) updateInterval.getValueFloat()) return;
        tickCounter = 0;
        if (!scanning.get()) {
            snapPlayerChunkX = mc.player.getChunkPos().x;
            snapPlayerChunkZ = mc.player.getChunkPos().z;
            startAsyncScan();
        }
    }

    private void startAsyncScan() {
        scanning.set(true);
        int pcx = snapPlayerChunkX, pcz = snapPlayerChunkZ;
        int radius = (int) searchRadius.getValueFloat();
        scanFuture = executor.submit(() -> {
            try { doScan(pcx, pcz, radius); }
            finally { scanning.set(false); }
        });
    }

    private void doScan(int pcx, int pcz, int radius) {
        if (mc.world == null) return;
        scanResult.clear();
        BlockPos.Mutable mPos   = new BlockPos.Mutable();
        BlockPos.Mutable mNorth = new BlockPos.Mutable();
        BlockPos.Mutable mSouth = new BlockPos.Mutable();
        BlockPos.Mutable mEast  = new BlockPos.Mutable();
        BlockPos.Mutable mWest  = new BlockPos.Mutable();
        int worldBottom = mc.world.getBottomY();

        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                if (Thread.currentThread().isInterrupted()) return;
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;
                int startX = cx << 4, startZ = cz << 4;
                for (int x = startX; x < startX + 16; x++) {
                    for (int z = startZ; z < startZ + 16; z++) {
                        int worldTop = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - startX, z - startZ);
                        for (int y = worldBottom; y < worldTop; y++) {
                            mPos.set(x, y, z);
                            Block center = mc.world.getBlockState(mPos).getBlock();
                            for (BlockPattern p : PATTERNS) {
                                if (p.center != center) continue;
                                mNorth.set(x, y, z - 1); mSouth.set(x, y, z + 1);
                                mEast.set(x + 1, y, z);  mWest.set(x - 1, y, z);
                                Block n = mc.world.getBlockState(mNorth).getBlock();
                                Block s = mc.world.getBlockState(mSouth).getBlock();
                                Block e = mc.world.getBlockState(mEast).getBlock();
                                Block w = mc.world.getBlockState(mWest).getBlock();
                                // all 4 neighbors same and in pattern
                                if (n == s && n == e && n == w && p.neighbors.contains(n)) {
                                    scanResult.add(new BlockPos(x, y, z));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        List<BlockPos> newList = new ArrayList<>(scanResult);
        mc.execute(() -> {
            renderList = newList;
            if ((Boolean) notifyEnabled.getValue()) checkColumns(newList);
        });
    }

    private void checkColumns(List<BlockPos> blocks) {
        if (blocks.isEmpty()) return;
        Map<Long, int[]> colMap = new HashMap<>();
        for (BlockPos pos : blocks) {
            long key  = packXZ(pos.getX(), pos.getZ());
            int  y    = pos.getY();
            int[] data = colMap.get(key);
            if (data == null) colMap.put(key, new int[]{1, y, y});
            else { data[0]++; if (y < data[1]) data[1] = y; if (y > data[2]) data[2] = y; }
        }
        int minH = (int) minColumnHeight.getValueFloat();
        List<BlockPos> newBeams = new ArrayList<>(beamPosList);
        for (Map.Entry<Long, int[]> entry : colMap.entrySet()) {
            int[] data = entry.getValue();
            if (data[0] >= minH) {
                long notifyKey = entry.getKey() ^ ((long) data[1] << 20);
                if (notified.add(notifyKey)) {
                    int x = unpackX(entry.getKey()), z = unpackZ(entry.getKey());
                    if (mc.player != null) mc.player.sendMessage(Text.literal(
                        "§e[ColonFinder] §fColumn at §bX=" + x + " Z=" + z +
                        " §7Y:" + data[1] + "→" + data[2] + " (" + data[0] + " blocks)"), false);
                    newBeams.add(new BlockPos(x, data[1], z));
                }
            }
        }
        beamPosList = newBeams;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        Color fill    = (Color) fillColor.getValue();
        Color outline = (Color) outlineColor.getValue();
        Color beam    = (Color) beamColor.getValue();

        // render matched blocks as filled boxes
        for (BlockPos pos : renderList) {
            event.renderFilledBox(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                fill);
            event.renderBoxOutline(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1,
                outline);
        }

        // render beams
        if ((Boolean) beamEnabled.getValue()) {
            int bh = (int) beamHeight.getValueFloat();
            Color fillBeam = new Color(beam.getRed(), beam.getGreen(), beam.getBlue(), Math.max(1, beam.getAlpha() / 3));
            Color edgeBeam = new Color(beam.getRed(), beam.getGreen(), beam.getBlue(), Math.min(255, beam.getAlpha() + 60));
            double half = 0.075;
            for (BlockPos base : beamPosList) {
                double cx   = base.getX() + 0.5;
                double cz   = base.getZ() + 0.5;
                double yBot = base.getY();
                double yTop = base.getY() + bh;
                double x1 = cx - half, x2 = cx + half;
                double z1 = cz - half, z2 = cz + half;
                // 4 faces
                event.renderQuad(x1, yBot, z2, x2, yBot, z2, x2, yTop, z2, x1, yTop, z2, fillBeam);
                event.renderQuad(x2, yBot, z1, x1, yBot, z1, x1, yTop, z1, x2, yTop, z1, fillBeam);
                event.renderQuad(x2, yBot, z2, x2, yBot, z1, x2, yTop, z1, x2, yTop, z2, fillBeam);
                event.renderQuad(x1, yBot, z1, x1, yBot, z2, x1, yTop, z2, x1, yTop, z1, fillBeam);
                // edges
                event.renderLine(x1, yBot, z1, x1, yTop, z1, edgeBeam);
                event.renderLine(x2, yBot, z1, x2, yTop, z1, edgeBeam);
                event.renderLine(x2, yBot, z2, x2, yTop, z2, edgeBeam);
                event.renderLine(x1, yBot, z2, x1, yTop, z2, edgeBeam);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private static long packXZ(int x, int z)  { return (long)x << 32 | (long)z & 0xFFFFFFFFL; }
    private static int  unpackX(long key)      { return (int)(key >> 32); }
    private static int  unpackZ(long key)      { return (int)(key & 0xFFFFFFFFL); }

    public record BlockPattern(Block center, Set<Block> neighbors) {}
}
