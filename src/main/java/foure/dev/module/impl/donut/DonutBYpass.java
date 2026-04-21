package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.EventRender3D;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.render.Render3D;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(
    name = "DonutBYpass",
    category = Category.DONUT,
    desc = "Gaseste chunk-urile cu budding amethyst la distanta si le marcheaza."
)
public class DonutBYpass extends Function {

    private final NumberSetting minDistance = new NumberSetting("Min Distance", this, 100.0, 0.0, 512.0, 1.0);
    private final NumberSetting workerThreads = new NumberSetting("Worker Threads", this, 1.0, 1.0, 4.0, 1.0);
    private final BooleanSetting tracers = new BooleanSetting("Tracers", true);
    private final BooleanSetting markNotFound = new BooleanSetting("Mark Not Found", false);
    private final ColorSetting foundColor = new ColorSetting("Found Color", this, new Color(0, 255, 0, 65));
    private final ColorSetting notFoundColor = new ColorSetting("Not Found Color", this, new Color(255, 0, 0, 40));
    private final BooleanSetting chatNotify = new BooleanSetting("Chat Notify", true);
    private final BooleanSetting soundNotify = new BooleanSetting("Sound Notify", true);

    private final Set<Long> foundChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> notFoundChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> notifiedChunks = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Long> notifyQueue = new ConcurrentLinkedQueue<>();

    private ExecutorService scanExecutor;
    private RegistryKey<World> lastWorldKey;

    public DonutBYpass() {
        this.addSettings(new Setting[]{
            minDistance, workerThreads,
            tracers, markNotFound, foundColor, notFoundColor,
            chatNotify, soundNotify
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        scanExecutor = Executors.newFixedThreadPool((int) workerThreads.getValueFloat());
        foundChunks.clear();
        scannedChunks.clear();
        notFoundChunks.clear();
        notifiedChunks.clear();
        notifyQueue.clear();
        lastWorldKey = mc.world != null ? mc.world.getRegistryKey() : null;
        scanAllLoadedChunks();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
            scanExecutor = null;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        if (lastWorldKey != mc.world.getRegistryKey()) {
            resetForCurrentWorld();
        }

        processNotifications();
        pruneUnloadedChunks();
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (fullNullCheck()) return;

        for (long packed : foundChunks) {
            if (!isChunkFarEnough(packed)) continue;
            renderChunkFill(packed, (Color) foundColor.getValue());
            renderChunkOutline(packed, (Color) foundColor.getValue());
            if ((Boolean) tracers.getValue()) renderTracer(packed, (Color) foundColor.getValue());
        }

        if ((Boolean) markNotFound.getValue()) {
            for (long packed : notFoundChunks) {
                if (!isChunkFarEnough(packed)) continue;
                renderChunkFill(packed, (Color) notFoundColor.getValue());
                renderChunkOutline(packed, (Color) notFoundColor.getValue());
            }
        }
    }

    // ─── internals ────────────────────────────────────────────────────────────

    private void scanAllLoadedChunks() {
        if (mc.world == null || mc.player == null) return;
        int r = mc.options.getViewDistance().getValue();
        int cx = (int) mc.player.getX() >> 4;
        int cz = (int) mc.player.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                net.minecraft.world.chunk.Chunk c = mc.world.getChunkManager()
                    .getChunk(x, z, ChunkStatus.FULL, false);
                if (c instanceof WorldChunk wc) queueChunkScan(wc);
            }
        }
    }

    private void resetForCurrentWorld() {
        foundChunks.clear();
        scannedChunks.clear();
        notFoundChunks.clear();
        notifiedChunks.clear();
        notifyQueue.clear();
        lastWorldKey = mc.world.getRegistryKey();
        scanAllLoadedChunks();
    }

    private void queueChunkScan(WorldChunk chunk) {
        if (scanExecutor == null || scanExecutor.isShutdown()) return;
        scanExecutor.execute(() -> scanChunkAsync(chunk));
    }

    private void scanChunkAsync(WorldChunk chunk) {
        if (!isToggled() || mc.world == null) return;
        try {
            long packed = chunk.getPos().toLong();
            int bottomY = mc.world.getBottomY();
            scannedChunks.add(packed);

            if (chunkHasBuddingAmethyst(chunk, bottomY)) {
                notFoundChunks.remove(packed);
                if (foundChunks.add(packed)) notifyQueue.add(packed);
            } else {
                foundChunks.remove(packed);
                notFoundChunks.add(packed);
            }
        } catch (Throwable ignored) {}
    }

    private boolean chunkHasBuddingAmethyst(WorldChunk chunk, int bottomY) {
        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int x = startX; x < startX + 16; x++) {
            for (int z = startZ; z < startZ + 16; z++) {
                int topY = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x - startX, z - startZ);
                for (int y = bottomY; y < topY; y++) {
                    pos.set(x, y, z);
                    if (chunk.getBlockState(pos).isOf(Blocks.BUDDING_AMETHYST)) return true;
                }
            }
        }
        return false;
    }

    private void processNotifications() {
        Long packed;
        while ((packed = notifyQueue.poll()) != null) {
            notifyFound(packed);
        }
    }

    private void notifyFound(long packed) {
        if (!notifiedChunks.add(packed)) return;
        int cx = ChunkPos.getPackedX(packed);
        int cz = ChunkPos.getPackedZ(packed);

        if ((Boolean) chatNotify.getValue()) {
            mc.player.sendMessage(Text.literal("[DonutBYpass] Gasit budding amethyst in chunk [" + cx + ", " + cz + "]."), false);
        }
        if ((Boolean) soundNotify.getValue()) {
            mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        }
    }

    private void pruneUnloadedChunks() {
        if (mc.world == null) return;
        foundChunks.removeIf(this::isChunkUnloaded);
        notFoundChunks.removeIf(this::isChunkUnloaded);
        scannedChunks.removeIf(this::isChunkUnloaded);
    }

    private boolean isChunkUnloaded(long packed) {
        int x = ChunkPos.getPackedX(packed);
        int z = ChunkPos.getPackedZ(packed);
        return !(mc.world.getChunkManager().getChunk(x, z, ChunkStatus.FULL, false) instanceof WorldChunk);
    }

    private boolean isChunkFarEnough(long packed) {
        if (mc.player == null) return false;
        int chunkX = ChunkPos.getPackedX(packed);
        int chunkZ = ChunkPos.getPackedZ(packed);
        double minX = chunkX << 4, maxX = minX + 16;
        double minZ = chunkZ << 4, maxZ = minZ + 16;
        double px = mc.player.getX(), pz = mc.player.getZ();
        double dx = px < minX ? minX - px : (px > maxX ? px - maxX : 0.0);
        double dz = pz < minZ ? minZ - pz : (pz > maxZ ? pz - maxZ : 0.0);
        double dist = minDistance.getValueFloat();
        return (dx * dx + dz * dz) >= dist * dist;
    }

    private Box getChunkBox(long packed) {
        int chunkX = ChunkPos.getPackedX(packed);
        int chunkZ = ChunkPos.getPackedZ(packed);
        double x1 = chunkX << 4, z1 = chunkZ << 4;
        return new Box(x1, 64, z1, x1 + 16, 65, z1 + 16);
    }

    private void renderChunkFill(long packed, Color color) {
        Render3D.drawBoxFill(getChunkBox(packed), color);
    }

    private void renderChunkOutline(long packed, Color color) {
        Render3D.drawBoxOutline(getChunkBox(packed), color);
    }

    private void renderTracer(long packed, Color color) {
        if (mc.player == null) return;
        Box box = getChunkBox(packed);
        Render3D.drawTracer(
            new org.joml.Vector3d(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ()),
            box,
            color
        );
    }
}
