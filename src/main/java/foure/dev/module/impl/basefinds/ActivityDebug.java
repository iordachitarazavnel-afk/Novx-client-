package foure.dev.module.impl.basefinds;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ModuleInfo(
    name = "ActivityDebug",
    category = Category.BASEFINDS,
    desc = "Highlights suspicious chunk load/unload activity"
)
public class ActivityDebug extends Function {

    private final NumberSetting renderRadius = new NumberSetting("Render Radius", this, 8.0, 1.0, 32.0, 1.0);
    private final NumberSetting minCycles = new NumberSetting("Min Cycles", this, 4.0, 1.0, 20.0, 1.0);
    private final NumberSetting purpleAt = new NumberSetting("Purple At", this, 19.0, 5.0, 50.0, 1.0);

    private static final long WINDOW_MS = 120_000L;
    private static final long FADE_MS = 600_000L;
    private static final int MIN_UNLOADS = 2;
    private static final float FIXED_Y = 60.0f;

    private final ConcurrentHashMap<ChunkPos, ChunkActivity> activity = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkPos, Long> flagged = new ConcurrentHashMap<>();
    private final Set<ChunkPos> currentlyLoaded = ConcurrentHashMap.newKeySet();

    private final List<RenderEntry> boxesToRender = new ArrayList<>();

    private static final int[][] FACE_INDICES = new int[][]{{0,1,2,3},{4,5,6,7},{0,1,5,4},{3,2,6,7},{0,3,7,4},{1,2,6,5}};
    private static final byte[] FACE_DIRS = new byte[]{4,2,8,16,32,64};
    private static final float[] FACE_SHADING = new float[]{0.5f,1.0f,0.7f,0.8f,0.6f,0.9f};

    public ActivityDebug() {
        this.addSettings(new Setting[]{this.renderRadius, this.minCycles, this.purpleAt});
    }

    public void onEnable() {
        super.onEnable();
        activity.clear();
        flagged.clear();
        currentlyLoaded.clear();
    }

    public void onDisable() {
        super.onDisable();
        activity.clear();
        flagged.clear();
        currentlyLoaded.clear();
    }

    // Apelat din mixin-ul de chunk load
    public void onChunkLoad(int x, int z) {
        ChunkPos cp = new ChunkPos(x, z);
        activity.computeIfAbsent(cp, k -> new ChunkActivity()).addLoad();
        currentlyLoaded.add(cp);
        checkAndFlag(cp);
    }

    // Apelat din mixin-ul de chunk unload
    public void onChunkUnload(int x, int z) {
        ChunkPos cp = new ChunkPos(x, z);
        activity.computeIfAbsent(cp, k -> new ChunkActivity()).addUnload();
        currentlyLoaded.remove(cp);
        checkAndFlag(cp);
    }

    private void checkAndFlag(ChunkPos cp) {
        ChunkActivity act = activity.get(cp);
        if (act == null) return;
        int cycles = Math.min(act.recentLoads(), act.recentUnloads());
        if (cycles >= (int) minCycles.getValueFloat() && act.recentUnloads() >= MIN_UNLOADS) {
            flagged.put(cp, System.currentTimeMillis());
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!fullNullCheck()) {
            long now = System.currentTimeMillis();
            flagged.entrySet().removeIf(e -> now - e.getValue() > FADE_MS);
        }
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        if (fullNullCheck()) return;

        boxesToRender.clear();

        int radius = (int) renderRadius.getValueFloat();
        int px = mc.player.getChunkPos().x;
        int pz = mc.player.getChunkPos().z;

        Set<ChunkPos> toRender = ConcurrentHashMap.newKeySet();
        toRender.addAll(flagged.keySet());
        toRender.addAll(currentlyLoaded);

        int purpleAtVal = (int) purpleAt.getValueFloat();
        int minCyclesVal = (int) minCycles.getValueFloat();

        for (ChunkPos cp : toRender) {
            if (Math.abs(cp.x - px) > radius || Math.abs(cp.z - pz) > radius) continue;

            ChunkActivity act = activity.computeIfAbsent(cp, k -> new ChunkActivity());
            int loads = act.recentLoads();
            int unloads = act.recentUnloads();
            int cycles = Math.min(loads, unloads);
            boolean detected = cycles >= minCyclesVal && unloads >= MIN_UNLOADS;

            float r, g, b, a;
            if (!detected) {
                r = 0f; g = 0f; b = 0f;
                a = 0.137f;
            } else {
                int total = loads + unloads;
                a = Math.min(200, 80 + total * 12) / 255.0f;
                if (total >= purpleAtVal) {
                    r = 192f / 255f; g = 0f; b = 255f / 255f;
                } else {
                    r = 1.0f;
                    g = Math.max(0, 150 - total * 15) / 255.0f;
                    b = 0f;
                }
            }

            Box box = new Box(
                cp.getStartX(), FIXED_Y, cp.getStartZ(),
                cp.getStartX() + 16, FIXED_Y + 0.5, cp.getStartZ() + 16);
            boxesToRender.add(new RenderEntry(box, new Color(r, g, b, a)));
        }
    }

    @Subscribe
    public void onRender2D(RenderEvent event) {
        if (boxesToRender.isEmpty()) return;

        Renderer2D r = event.renderer();

        for (RenderEntry entry : boxesToRender) {
            Box b = entry.box;
            Color fill = entry.color;
            Color outline = new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 200);

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
                        Math.min(255, (int)(fill.getRed() * shading)),
                        Math.min(255, (int)(fill.getGreen() * shading)),
                        Math.min(255, (int)(fill.getBlue() * shading)),
                        fill.getAlpha());
                    r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y,
                        (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, shaded.getRGB());
                }
            }

            // outline
            int oc = outline.getRGB();
            drawLine(r, sc[0], sc[1], 1.5f, oc); drawLine(r, sc[1], sc[2], 1.5f, oc);
            drawLine(r, sc[2], sc[3], 1.5f, oc); drawLine(r, sc[3], sc[0], 1.5f, oc);
            drawLine(r, sc[4], sc[5], 1.5f, oc); drawLine(r, sc[5], sc[6], 1.5f, oc);
            drawLine(r, sc[6], sc[7], 1.5f, oc); drawLine(r, sc[7], sc[4], 1.5f, oc);
            drawLine(r, sc[0], sc[4], 1.5f, oc); drawLine(r, sc[1], sc[5], 1.5f, oc);
            drawLine(r, sc[2], sc[6], 1.5f, oc); drawLine(r, sc[3], sc[7], 1.5f, oc);
        }
    }

    private void drawLine(Renderer2D r, Vec3d p1, Vec3d p2, float thickness, int color) {
        if (p1 != null && p2 != null)
            r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, thickness, color);
    }

    private record RenderEntry(Box box, Color color) {}

    private static class ChunkActivity {
        final CopyOnWriteArrayList<Long> loads = new CopyOnWriteArrayList<>();
        final CopyOnWriteArrayList<Long> unloads = new CopyOnWriteArrayList<>();

        void addLoad() {
            long n = System.currentTimeMillis();
            loads.add(n);
            loads.removeIf(t -> n - t > FADE_MS);
        }

        void addUnload() {
            long n = System.currentTimeMillis();
            unloads.add(n);
            unloads.removeIf(t -> n - t > FADE_MS);
        }

        int recentLoads() {
            long n = System.currentTimeMillis();
            return (int) loads.stream().filter(t -> n - t < WINDOW_MS).count();
        }

        int recentUnloads() {
            long n = System.currentTimeMillis();
            return (int) unloads.stream().filter(t -> n - t < WINDOW_MS).count();
        }
    }
}
