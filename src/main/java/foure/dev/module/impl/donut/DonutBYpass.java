package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent; // Dacă ai nevoie de event-uri de packet
import foure.dev.event.impl.render.EventRender3D;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.block.Blocks;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import java.awt.Color;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ModuleInfo(
   name = "DonutBYpass",
   category = Category.DONUT,
   desc = "Finds far budding amethyst chunks and marks them using background threads."
)
public class DonutBYpass extends Function {

    // --- SETTINGS (Adaptate din Meteor) ---
    private final NumberSetting minDistance = new NumberSetting("Min Distance", this, 100.0D, 0.0D, 512.0D, 1.0D);
    private final NumberSetting workerThreads = new NumberSetting("Worker Threads", this, 1.0D, 1.0D, 4.0D, 1.0D);
    
    private final BooleanSetting tracers = new BooleanSetting("Tracers", true);
    private final BooleanSetting markNotFound = new BooleanSetting("Mark Not Found", false);
    
    private final BooleanSetting chatNotify = new BooleanSetting("Chat Notify", true);
    private final BooleanSetting soundNotify = new BooleanSetting("Sound Notify", true);

    // --- DATA ---
    private final Set<Long> foundChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> scannedChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> notFoundChunks = ConcurrentHashMap.newKeySet();
    private final Set<Long> notifiedChunks = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Long> notifyQueue = new ConcurrentLinkedQueue<>();

    private ExecutorService scanExecutor;
    private RegistryKey<World> lastWorldKey;

    public DonutBYpass() {
        this.addSettings(new Setting[]{
            this.minDistance, this.workerThreads, this.tracers, 
            this.markNotFound, this.chatNotify, this.soundNotify
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        // Inițializare Thread Pool similar cu Meteor
        scanExecutor = Executors.newFixedThreadPool((int) workerThreads.getValueFloat());
        
        clearData();
        if (mc.world != null) {
            lastWorldKey = mc.world.getRegistryKey();
            // Scanăm chunk-urile deja încărcate la activare
            // Notă: În FourEClient va trebui să te asiguri că ai acces la chunk-urile încărcate
            // Aici simulăm scanarea inițială
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (scanExecutor != null) {
            scanExecutor.shutdownNow();
            scanExecutor = null;
        }
    }

    private void clearData() {
        foundChunks.clear();
        scannedChunks.clear();
        notFoundChunks.clear();
        notifiedChunks.clear();
        notifyQueue.clear();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        // Reset dacă schimbăm dimensiunea (Nether/End/Overworld)
        if (lastWorldKey != mc.world.getRegistryKey()) {
            clearData();
            lastWorldKey = mc.world.getRegistryKey();
        }

        // Scanare continuă pentru chunk-uri noi (bazat pe render distance)
        int radius = mc.options.getClampedViewDistance();
        int pX = mc.player.getChunkPos().x;
        int pZ = mc.player.getChunkPos().z;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                ChunkPos pos = new ChunkPos(pX + x, pZ + z);
                long packed = pos.toLong();
                
                if (!scannedChunks.contains(packed)) {
                    WorldChunk chunk = mc.world.getChunk(pos.x, pos.z);
                    if (chunk != null) {
                        queueChunkScan(chunk);
                    }
                }
            }
        }

        processNotifications();
        pruneUnloadedChunks();
    }

    private void queueChunkScan(WorldChunk chunk) {
        if (scanExecutor == null || chunk == null) return;
        long packed = chunk.getPos().toLong();
        if (scannedChunks.add(packed)) {
            scanExecutor.execute(() -> scanChunkAsync(chunk));
        }
    }

    private void scanChunkAsync(WorldChunk chunk) {
        try {
            long packedChunk = chunk.getPos().toLong();
            int bottomY = mc.world.getBottomY();

            if (chunkHasBuddingAmethyst(chunk, bottomY)) {
                notFoundChunks.remove(packedChunk);
                if (foundChunks.add(packedChunk)) {
                    notifyQueue.add(packedChunk);
                }
            } else {
                foundChunks.remove(packedChunk);
                notFoundChunks.add(packedChunk);
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
        Long packedChunk;
        while ((packedChunk = notifyQueue.poll()) != null) {
            if (notifiedChunks.add(packedChunk)) {
                if ((Boolean) chatNotify.getValue()) {
                    mc.player.sendMessage(Text.literal("§d[Donut] §fFound Budding Amethyst: §7" + 
                        (ChunkPos.getPackedX(packedChunk) << 4) + ", " + (ChunkPos.getPackedZ(packedChunk) << 4)), false);
                }
                if ((Boolean) soundNotify.getValue()) {
                    mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                }
            }
        }
    }

    private void pruneUnloadedChunks() {
        if (mc.world == null) return;
        foundChunks.removeIf(this::isChunkUnloaded);
        notFoundChunks.removeIf(this::isChunkUnloaded);
        scannedChunks.removeIf(this::isChunkUnloaded);
    }

    private boolean isChunkUnloaded(long packedChunk) {
        int x = ChunkPos.getPackedX(packedChunk);
        int z = ChunkPos.getPackedZ(packedChunk);
        return !(mc.world.getChunk(x, z, ChunkStatus.FULL, false) instanceof WorldChunk);
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        if (fullNullCheck()) return;

        // Culori (Poți să le faci setări de tip Color dacă framework-ul tău suportă)
        Color foundColor = new Color(0, 255, 0, 100);
        Color notFoundColor = new Color(255, 0, 0, 50);

        for (long packedChunk : foundChunks) {
            if (isChunkFarEnough(packedChunk)) {
                renderChunk(event, packedChunk, foundColor, (Boolean) tracers.getValue());
            }
        }

        if ((Boolean) markNotFound.getValue()) {
            for (long packedChunk : notFoundChunks) {
                if (isChunkFarEnough(packedChunk)) {
                    renderChunk(event, packedChunk, notFoundColor, false);
                }
            }
        }
    }

    private boolean isChunkFarEnough(long packedChunk) {
        double px = mc.player.getX();
        double pz = mc.player.getZ();
        double cx = (ChunkPos.getPackedX(packedChunk) << 4) + 8;
        double cz = (ChunkPos.getPackedZ(packedChunk) << 4) + 8;
        
        double distSq = Math.pow(px - cx, 2) + Math.pow(pz - cz, 2);
        return distSq >= Math.pow(minDistance.getValueFloat(), 2);
    }

    // --- METODA DE RENDER (Aici adaptezi la renderer-ul tau) ---
    private void renderChunk(EventRender3D event, long packedChunk, Color color, boolean tracer) {
        int chunkX = ChunkPos.getPackedX(packedChunk);
        int chunkZ = ChunkPos.getPackedZ(packedChunk);

        double x1 = chunkX << 4;
        double y1 = 64; // Randăm la nivelul mării pentru vizibilitate
        double z1 = chunkZ << 4;
        double x2 = x1 + 16;
        double y2 = y1 + 1;
        double z2 = z1 + 16;

        Box box = new Box(x1, y1, z1, x2, y2, z2);

        // ATENȚIE: Inlocuieste 'event.renderer' cu clasa de randare din FourEClient (ex. RenderUtils)
        // event.renderer.box(box, color, color, ShapeMode.Both, 0); 

        if (tracer && mc.player != null) {
            // event.renderer.line(mc.player.getX(), mc.player.getEyeY(), mc.player.getZ(), x1+8, y1+0.5, z1+8, color);
        }
    }
}
