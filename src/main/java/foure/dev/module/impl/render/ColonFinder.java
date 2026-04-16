package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
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

    private final NumberSetting searchRadius    = new NumberSetting("Search Radius",     this,  3.0,  1.0,   8.0, 1.0);
    private final NumberSetting updateInterval  = new NumberSetting("Update Interval",   this, 20.0,  5.0, 100.0, 1.0);
    private final NumberSetting minColumnHeight = new NumberSetting("Min Column Height", this,  4.0,  2.0,  10.0, 1.0);
    private final NumberSetting beamHeight      = new NumberSetting("Beam Height",       this, 64.0,  8.0, 512.0, 1.0);
    private final BooleanSetting notifyEnabled  = new BooleanSetting("Notify",  true);
    private final BooleanSetting beamEnabled    = new BooleanSetting("Beam",    true);
    private final ColorSetting   fillColor      = new ColorSetting("Fill Color",    this, new Color(255, 105, 180,  80));
    private final ColorSetting   outlineColor   = new ColorSetting("Outline Color", this, new Color(255,  20, 147, 255));
    private final ColorSetting   beamColor      = new ColorSetting("Beam Color",    this, new Color(  0, 220,   0,  60));

    private static final List<BlockPattern> PATTERNS = List.of(
        new BlockPattern(Blocks.STONE,    Set.of(Blocks.ANDESITE, Blocks.GRANITE, Blocks.DIORITE, Blocks.GRAVEL, Blocks.DIRT)),
        new BlockPattern(Blocks.GRANITE,  Set.of(Blocks.ANDESITE, Blocks.DIORITE, Blocks.GRAVEL,  Blocks.DIRT,   Blocks.STONE)),
        new BlockPattern(Blocks.DIORITE,  Set.of(Blocks.GRANITE,  Blocks.ANDESITE,Blocks.GRAVEL,  Blocks.DIRT,   Blocks.STONE)),
        new BlockPattern(Blocks.ANDESITE, Set.of(Blocks.GRANITE,  Blocks.DIORITE, Blocks.GRAVEL,  Blocks.DIRT,   Blocks.STONE)),
        new BlockPattern(Blocks.GRAVEL,   Set.of(Blocks.GRANITE,  Blocks.DIORITE, Blocks.DIRT,    Blocks.ANDESITE, Blocks.STONE))
    );

    private volatile List<BlockPos> renderList  = new ArrayList<>();
    private volatile List<BlockPos> beamPosList = new ArrayList<>();
    private final    List<BlockPos> scanResult  = new ArrayList<>();
    private final Set<Long>         notified    = new HashSet<>();
    private final ExecutorService   executor    = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ColonScanner"); t.setDaemon(true); return t;
    });
    private final AtomicBoolean scanning  = new AtomicBoolean(false);
    private Future<?>  scanFuture;
    private int        tickCounter = 0;
    private int        snapPCX     = 0;
    private int        snapPCZ     = 0;

    public ColonFinder() {
        this.addSettings(new Setting[]{
            searchRadius, updateInterval, minColumnHeight, beamHeight,
            notifyEnabled, beamEnabled, fillColor, outlineColor, beamColor
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
            snapPCX = mc.player.getChunkPos().x;
            snapPCZ = mc.player.getChunkPos().z;
            startAsyncScan();
        }
    }

    private void startAsyncScan() {
        scanning.set(true);
        int pcx = snapPCX, pcz = snapPCZ, radius = (int) searchRadius.getValueFloat();
        scanFuture = executor.submit(() -> {
            try { doScan(pcx, pcz, radius); }
            finally { scanning.set(false); }
        });
    }

    private void doScan(int pcx, int pcz, int radius) {
        if (mc.world == null) return;
        scanResult.clear();
        BlockPos.Mutable mp = new BlockPos.Mutable();
        BlockPos.Mutable mn = new BlockPos.Mutable();
        BlockPos.Mutable ms = new BlockPos.Mutable();
        BlockPos.Mutable me = new BlockPos.Mutable();
        BlockPos.Mutable mw = new BlockPos.Mutable();
        int worldBottom = mc.world.getBottomY();

        for (int cx = pcx - radius; cx <= pcx + radius; cx++) {
            for (int cz = pcz - radius; cz <= pcz + radius; cz++) {
                if (Thread.currentThread().isInterrupted()) return;
                WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null || chunk.isEmpty()) continue;
                int sx = cx << 4, sz = cz << 4;
                for (int x = sx; x < sx + 16; x++) {
                    for (int z = sz; z < sz + 16; z++) {
                        int top = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - sx, z - sz);
                        for (int y = worldBottom; y < top; y++) {
                            mp.set(x, y, z);
                            Block center = mc.world.getBlockState(mp).getBlock();
                            for (BlockPattern p : PATTERNS) {
                                if (p.center != center) continue;
                                mn.set(x,y,z-1); ms.set(x,y,z+1);
                                me.set(x+1,y,z); mw.set(x-1,y,z);
                                Block n = mc.world.getBlockState(mn).getBlock();
                                Block s = mc.world.getBlockState(ms).getBlock();
                                Block e = mc.world.getBlockState(me).getBlock();
                                Block w = mc.world.getBlockState(mw).getBlock();
                                if (n == s && n == e && n == w && p.neighbors.contains(n)) {
                                    scanResult.add(new BlockPos(x, y, z)); break;
                                }
                            }
                        }
                    }
                }
            }
        }
        List<BlockPos> copy = new ArrayList<>(scanResult);
        mc.execute(() -> {
            renderList = copy;
            if ((Boolean) notifyEnabled.getValue()) checkColumns(copy);
        });
    }

    private void checkColumns(List<BlockPos> blocks) {
        if (blocks.isEmpty()) return;
        Map<Long, int[]> colMap = new HashMap<>();
        for (BlockPos pos : blocks) {
            long key = packXZ(pos.getX(), pos.getZ());
            int y = pos.getY();
            int[] d = colMap.get(key);
            if (d == null) colMap.put(key, new int[]{1, y, y});
            else { d[0]++; if (y < d[1]) d[1] = y; if (y > d[2]) d[2] = y; }
        }
        int minH = (int) minColumnHeight.getValueFloat();
        List<BlockPos> newBeams = new ArrayList<>(beamPosList);
        for (Map.Entry<Long, int[]> entry : colMap.entrySet()) {
            int[] d = entry.getValue();
            if (d[0] >= minH) {
                long nk = entry.getKey() ^ ((long) d[1] << 20);
                if (notified.add(nk)) {
                    int x = unpackX(entry.getKey()), z = unpackZ(entry.getKey());
                    if (mc.player != null) mc.player.sendMessage(Text.literal(
                        "§e[ColonFinder] §fColumn at §bX=" + x + " Z=" + z +
                        " §7Y:" + d[1] + "→" + d[2] + " (" + d[0] + " blocks)"), false);
                    newBeams.add(new BlockPos(x, d[1], z));
                }
            }
        }
        beamPosList = newBeams;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (renderList.isEmpty() && beamPosList.isEmpty()) return;

        Camera cam = RenderUtils.getCamera();
        Vec3d camPos = cam.getPos();
        MatrixStack matrices = event.matrixStack;

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cam.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cam.getYaw() + 180.0F));
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();

        Color fill    = (Color) fillColor.getValue();
        Color outline = (Color) outlineColor.getValue();

        for (BlockPos pos : renderList) {
            float rx = (float)(pos.getX() - camPos.x);
            float ry = (float)(pos.getY() - camPos.y);
            float rz = (float)(pos.getZ() - camPos.z);
            RenderUtils.renderFilledBox(matrices,  rx, ry, rz, rx+1, ry+1, rz+1, fill);
            RenderUtils.renderBoxOutline(matrices, rx, ry, rz, rx+1, ry+1, rz+1, outline);
        }

        if ((Boolean) beamEnabled.getValue()) {
            Color bc       = (Color) beamColor.getValue();
            Color beamFill = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), Math.max(1,   bc.getAlpha() / 3));
            Color beamEdge = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), Math.min(255, bc.getAlpha() + 60));
            int   bh       = (int) beamHeight.getValueFloat();
            float half     = 0.075f;

            for (BlockPos base : beamPosList) {
                float cx   = (float)(base.getX() + 0.5 - camPos.x);
                float cz   = (float)(base.getZ() + 0.5 - camPos.z);
                float yBot = (float)(base.getY() - camPos.y);
                float yTop = yBot + bh;
                RenderUtils.renderFilledBox(matrices, cx-half, yBot, cz-half, cx+half, yTop, cz+half, beamFill);
                RenderUtils.drawLine(matrices, cx-half, yBot, cz-half, cx-half, yTop, cz-half, beamEdge);
                RenderUtils.drawLine(matrices, cx+half, yBot, cz-half, cx+half, yTop, cz-half, beamEdge);
                RenderUtils.drawLine(matrices, cx+half, yBot, cz+half, cx+half, yTop, cz+half, beamEdge);
                RenderUtils.drawLine(matrices, cx-half, yBot, cz+half, cx-half, yTop, cz+half, beamEdge);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        matrices.pop();
    }

    private static long packXZ(int x, int z)  { return (long)x << 32 | (long)z & 0xFFFFFFFFL; }
    private static int  unpackX(long key)      { return (int)(key >> 32); }
    private static int  unpackZ(long key)      { return (int)(key & 0xFFFFFFFFL); }

    public record BlockPattern(Block center, Set<Block> neighbors) {}
}
