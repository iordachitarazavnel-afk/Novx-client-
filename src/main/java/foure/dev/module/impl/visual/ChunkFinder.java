package foure.dev.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.Camera;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.world.Heightmap.Type;

@ModuleInfo(
   name = "ChunkFinder",
   category = Category.RENDER,
   desc = "Detects suspicious chunks that might contain bases."
)
public class ChunkFinder extends Function {
   private static final boolean USE_THREADS = true;
   private static final int THREAD_COUNT = Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
   private static final int SCAN_DELAY_MS = 100;
   private static final int MAX_CONCURRENT_SCANS = 3;
   private static final int DEEPSLATE_THRESHOLD = 3;
   private static final int ROTATED_THRESHOLD = 1;
   private static final int AMETHYST_THRESHOLD = 1;
   private final Set<ChunkPos> flaggedChunks = ConcurrentHashMap.newKeySet();
   private final ConcurrentHashMap<ChunkPos, ChunkFinder.ChunkAnalysis> chunkData = new ConcurrentHashMap();
   private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
   private final Queue<ChunkPos> scanQueue = new ConcurrentLinkedQueue();
   private final AtomicLong activeScans = new AtomicLong(0L);
   private final boolean detectDeepslate = false;
   private final boolean detectRotatedDeepslate = true;
   private final boolean detectLongDripstone = true;
   private final int minDripstoneLength = 7;
   private final boolean detectFullVines = true;
   private final int minVineLength = 30;
   private final boolean detectFullKelp = true;
   private final int minKelpLength = 6;
   private final boolean detectDioriteVeins = true;
   private final int minDioriteVeinLength = 5;
   private final boolean detectObsidianVeins = true;
   private final int minObsidianVeinLength = 15;
   private final boolean detectAmethystClusters = true;
   private final int minScanY = -5;
   private final int maxScanY = 25;
   private final int scanRadius = 16;
   private final boolean chatAlerts = true;
   private final boolean soundAlerts = true;
   private final boolean toastAlerts = true;
   private ChunkPos lastPlayerChunk = null;
   private ExecutorService pool;
   private volatile boolean scanning = false;
   private long lastResetTime = 0L;
   private long lastQueueRebuild = 0L;
   private final BooleanSetting alertCoorrds = new BooleanSetting("Alert Coordinates", true);
   private final ModeSetting mode = new ModeSetting("Mode", this, "Filled", new String[]{"Box", "Filled", "Fade", "Full"});
   private final ColorSetting chunkColor = new ColorSetting("Chunk Color", this, new Color(46, 0, 79, 100));
   private final BooleanSetting outline = new BooleanSetting("Outline", true);
   private final NumberSetting lineWidth = new NumberSetting("Outline Width", this, 2.0D, 0.5D, 5.0D, 0.1D);
   private final BooleanSetting tracer = new BooleanSetting("Tracer", true);
   private static final int[][] FACE_INDICES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final float[] FACE_SHADING = new float[]{0.5F, 1.0F, 0.7F, 0.8F, 0.6F, 0.9F};

   public ChunkFinder() {
      this.addSettings(new Setting[]{this.alertCoorrds, this.mode, this.chunkColor, this.outline, this.lineWidth, this.tracer});
   }

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

   @Subscribe
   public void onRenderTick(RenderEvent event) {
      if (this.isToggled()) {
         long currentTime = System.currentTimeMillis();
         if (mc.world == null) {
            if (this.scanning) {
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
         } else if (!this.scanning) {
            this.scanning = true;
            if (this.pool == null) {
               this.pool = Executors.newFixedThreadPool(THREAD_COUNT);
            }
         }

      }
   }

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

   @Subscribe
   public void onRender(RenderEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.lastPlayerChunk == null && this.scanning) {
            int playerChunkX = (int)Math.floor(mc.player.getX() / 16.0D);
            int playerChunkZ = (int)Math.floor(mc.player.getZ() / 16.0D);
            this.lastPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
            this.buildBFSScanQueue(this.lastPlayerChunk);
            this.tryStartScans();
         }

         this.updateScanQueue();
         this.tryStartScans();
         this.renderFlaggedChunks(event.renderer(), event.scaledWidth(), event.scaledHeight());
      }
   }

   private void renderFlaggedChunks(Renderer2D r, float sw, float sh) {
      if (!this.flaggedChunks.isEmpty()) {
         Camera cam = mc.gameRenderer.getCamera();
         if (cam != null) {
            Vec3d camPos = cam.getCameraPos();
            Color baseColor = (Color)this.chunkColor.getValue();
            float thickness = ((Double)this.lineWidth.getValue()).floatValue();
            int maxRender = 50;
            int rendered = 0;
            Iterator var10 = this.flaggedChunks.iterator();

            while(var10.hasNext()) {
               ChunkPos pos = (ChunkPos)var10.next();
               double distSq = mc.player.squaredDistanceTo((double)(pos.getStartX() + 8), mc.player.getY(), (double)(pos.getStartZ() + 8));
               if (!(distSq > 62500.0D)) {
                  if (rendered++ >= maxRender) {
                     break;
                  }

                  ChunkFinder.ChunkAnalysis analysis = (ChunkFinder.ChunkAnalysis)this.chunkData.get(pos);
                  if (analysis != null && analysis.susBlockPos != null) {
                     BlockPos var37 = analysis.susBlockPos;
                  } else {
                     new BlockPos(pos.getStartX() + 8, 64, pos.getStartZ() + 8);
                  }

                  double x1 = (double)pos.getStartX();
                  double z1 = (double)pos.getStartZ();
                  double x2 = x1 + 16.0D;
                  double z2 = z1 + 16.0D;
                  double y = (double)mc.world.getTopY(Type.WORLD_SURFACE, pos.getStartX() + 8, pos.getStartZ() + 8) + 0.05D;
                  Vec3d[] corners = new Vec3d[]{new Vec3d(x1, y, z1), new Vec3d(x2, y, z1), new Vec3d(x2, y, z2), new Vec3d(x1, y, z2)};
                  Vec3d p1 = ProjectionUtil.toScreen(corners[0]);
                  Vec3d p2 = ProjectionUtil.toScreen(corners[1]);
                  Vec3d p3 = ProjectionUtil.toScreen(corners[2]);
                  Vec3d p4 = ProjectionUtil.toScreen(corners[3]);
                  if (p1 != null && p2 != null && p3 != null && p4 != null) {
                     int fillColor = (new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 100)).getRGB();
                     r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, fillColor);
                     int outlineColor = baseColor.getRGB();
                     r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, thickness, outlineColor);
                     r.line((float)p2.x, (float)p2.y, (float)p3.x, (float)p3.y, thickness, outlineColor);
                     r.line((float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, thickness, outlineColor);
                     r.line((float)p4.x, (float)p4.y, (float)p1.x, (float)p1.y, thickness, outlineColor);
                     float cx = (float)((p1.x + p2.x + p3.x + p4.x) / 4.0D);
                     float cy = (float)((p1.y + p2.y + p3.y + p4.y) / 4.0D) + 20.0F;
                     String text = "Sus Chunk";
                     float tw = r.getStringWidth(FontRegistry.INTER_MEDIUM, text, 8.0F);
                     r.rect(cx - tw / 2.0F - 2.0F, cy - 5.0F, tw + 4.0F, 10.0F, 4.0F, (new Color(0, 0, 0, 150)).getRGB());
                     r.text(FontRegistry.INTER_MEDIUM, cx, cy, 8.0F, (String)text, -1, "c");
                  }
               }
            }

         }
      }
   }

   private double getFaceDistSq(Vec3d[] corners, int[] indices, Vec3d camPos) {
      double x = 0.0D;
      double y = 0.0D;
      double z = 0.0D;
      int[] var10 = indices;
      int var11 = indices.length;

      for(int var12 = 0; var12 < var11; ++var12) {
         int i = var10[var12];
         x += corners[i].x;
         y += corners[i].y;
         z += corners[i].z;
      }

      return camPos.squaredDistanceTo(x / 4.0D, y / 4.0D, z / 4.0D);
   }

   private void drawSafeLine(Renderer2D r, Vec3d p1, Vec3d p2, float thickness, int color) {
      if (p1 != null && p2 != null) {
         r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, thickness, color);
      }
   }

   private void updateScanQueue() {
      if (mc.player != null && mc.world != null) {
         long now = System.currentTimeMillis();
         int playerChunkX = (int)Math.floor(mc.player.getX() / 16.0D);
         int playerChunkZ = (int)Math.floor(mc.player.getZ() / 16.0D);
         ChunkPos currentPlayerChunk = new ChunkPos(playerChunkX, playerChunkZ);
         boolean moved = !currentPlayerChunk.equals(this.lastPlayerChunk);
         boolean timeout = now - this.lastQueueRebuild > 2000L;
         if (moved || timeout && this.scanQueue.isEmpty()) {
            if (moved) {
               this.lastPlayerChunk = currentPlayerChunk;
               this.cleanupDistantChunks(currentPlayerChunk);
            }

            this.lastQueueRebuild = now;
            this.buildBFSScanQueue(currentPlayerChunk);
         }

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
      Set<ChunkPos> visited = new HashSet();
      Queue<ChunkPos> bfsQueue = new LinkedList();
      bfsQueue.offer(center);
      visited.add(center);

      while(!bfsQueue.isEmpty()) {
         ChunkPos current = (ChunkPos)bfsQueue.poll();
         if (!this.scannedChunks.contains(current)) {
            this.scanQueue.offer(current);
         }

         int[][] offsets = new int[][]{{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
         int[][] var7 = offsets;
         int var8 = offsets.length;

         for(int var9 = 0; var9 < var8; ++var9) {
            int[] offset = var7[var9];
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
         while(this.activeScans.get() < 3L && !this.scanQueue.isEmpty()) {
            ChunkPos pos = (ChunkPos)this.scanQueue.poll();
            if (pos != null && !this.scannedChunks.contains(pos) && mc.world.getChunkManager().isChunkLoaded(pos.x, pos.z)) {
               this.scannedChunks.add(pos);
               Runnable task = () -> {
                  this.analyzeChunk(pos);
               };
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
         } catch (InterruptedException var6) {
            Thread.currentThread().interrupt();
         } finally {
            this.activeScans.decrementAndGet();
         }

      };
   }

   private void analyzeChunk(ChunkPos pos) {
      if (mc.world != null) {
         int startX = pos.getStartX();
         int startZ = pos.getStartZ();
         int minY = Math.max(-5, mc.world.getBottomY());
         int maxY = 319;
         ChunkFinder.ChunkAnalysis analysis = new ChunkFinder.ChunkAnalysis();

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = minY; y <= maxY; ++y) {
                  if (!this.scanning) {
                     return;
                  }

                  BlockPos bp = new BlockPos(startX + x, y, startZ + z);
                  BlockState state = mc.world.getBlockState(bp);
                  this.analyzeBlock(bp, state, y, analysis);
               }
            }
         }

         BlockPos obsidianVeinPos = this.checkHasLongDripstone(pos);
         if (obsidianVeinPos != null) {
            analysis.hasLongDripstone = true;
            if (analysis.susBlockPos == null) {
               analysis.susBlockPos = obsidianVeinPos;
            }
         }

         obsidianVeinPos = this.checkHasLongVine(pos);
         if (obsidianVeinPos != null) {
            analysis.hasLongVine = true;
            if (analysis.susBlockPos == null) {
               analysis.susBlockPos = obsidianVeinPos;
            }
         }

         obsidianVeinPos = this.checkAllKelpFullyGrown(pos);
         if (obsidianVeinPos != null) {
            analysis.allKelpFull = true;
            if (analysis.susBlockPos == null) {
               analysis.susBlockPos = obsidianVeinPos;
            }
         }

         obsidianVeinPos = this.checkHasDioriteVein(pos);
         if (obsidianVeinPos != null) {
            analysis.hasDioriteVein = true;
            if (analysis.susBlockPos == null) {
               analysis.susBlockPos = obsidianVeinPos;
            }
         }

         obsidianVeinPos = this.checkHasObsidianVein(pos);
         if (obsidianVeinPos != null) {
            analysis.hasObsidianVein = true;
            if (analysis.susBlockPos == null) {
               analysis.susBlockPos = obsidianVeinPos;
            }
         }

         this.chunkData.put(pos, analysis);
         this.evaluateChunk(pos, analysis);
      }
   }

   private void analyzeBlock(BlockPos pos, BlockState state, int worldY, ChunkFinder.ChunkAnalysis analysis) {
      ChunkFinder.SuspiciousType type = null;
      Block block = state.getBlock();
      if (this.isRotatedDeepslate(state)) {
         ++analysis.rotatedCount;
         type = ChunkFinder.SuspiciousType.ROTATED_DEEPSLATE;
         if (analysis.susBlockPos == null) {
            analysis.susBlockPos = pos;
         }
      }

      if (block == Blocks.AMETHYST_CLUSTER) {
         ++analysis.amethystCount;
         type = ChunkFinder.SuspiciousType.AMETHYST_CLUSTER;
         if (analysis.susBlockPos == null) {
            analysis.susBlockPos = pos;
         }
      }

   }

   private BlockPos checkHasDioriteVein(ChunkPos chunkPos) {
      if (mc.world == null) {
         return null;
      } else {
         int startX = chunkPos.getStartX();
         int startZ = chunkPos.getStartZ();
         int yMin = Math.max(mc.world.getBottomY(), -64);
         int yMax = 319;
         Set<BlockPos> visited = new HashSet();

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = yMin; y < yMax; ++y) {
                  if (!this.scanning) {
                     return null;
                  }

                  BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                  if (!visited.contains(pos)) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (this.isTargetBlock(state)) {
                        int lenUp = this.countVerticalRun(pos, Direction.UP);
                        int lenDown = this.countVerticalRun(pos, Direction.DOWN);
                        int total = lenUp + 1 + lenDown;
                        if (total >= 5) {
                           BlockPos start = pos.offset(Direction.DOWN, lenDown);
                           boolean enclosed = true;

                           for(int i = 0; i < total; ++i) {
                              BlockPos bp = start.offset(Direction.UP, i);
                              visited.add(bp);
                              if (!this.isEnclosedByStone(bp)) {
                                 enclosed = false;
                              }
                           }

                           if (enclosed) {
                              return pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private BlockPos checkHasObsidianVein(ChunkPos chunkPos) {
      if (mc.world == null) {
         return null;
      } else {
         int startX = chunkPos.getStartX();
         int startZ = chunkPos.getStartZ();
         int yMin = 15;
         int yMax = 63;
         Set<BlockPos> visited = new HashSet();

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = yMin; y <= yMax; ++y) {
                  if (!this.scanning) {
                     return null;
                  }

                  BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                  if (!visited.contains(pos)) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.isOf(Blocks.OBSIDIAN)) {
                        int lenUp = this.countVerticalRunObsidian(pos, Direction.UP);
                        int lenDown = this.countVerticalRunObsidian(pos, Direction.DOWN);
                        int total = lenUp + 1 + lenDown;
                        if (total >= 15) {
                           BlockPos start = pos.offset(Direction.DOWN, lenDown);
                           boolean enclosed = true;

                           for(int i = 0; i < total; ++i) {
                              BlockPos bp = start.offset(Direction.UP, i);
                              visited.add(bp);
                              if (!this.isEnclosedByNonObsidian(bp)) {
                                 enclosed = false;
                              }
                           }

                           if (enclosed) {
                              return pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private boolean isTargetBlock(BlockState state) {
      return state.isOf(Blocks.GRANITE) || state.isOf(Blocks.DIORITE) || state.isOf(Blocks.ANDESITE);
   }

   private int countVerticalRun(BlockPos from, Direction dir) {
      int count = 0;
      Mutable m = new Mutable(from.getX(), from.getY(), from.getZ());

      do {
         m.move(dir);
         if (!this.isTargetBlock(mc.world.getBlockState(m))) {
            break;
         }

         ++count;
      } while(count <= 20);

      return count;
   }

   private int countVerticalRunObsidian(BlockPos from, Direction dir) {
      int count = 0;
      Mutable m = new Mutable(from.getX(), from.getY(), from.getZ());

      do {
         m.move(dir);
         if (!mc.world.getBlockState(m).isOf(Blocks.OBSIDIAN)) {
            break;
         }

         ++count;
      } while(count <= 20);

      return count;
   }

   private boolean isEnclosedByStone(BlockPos pos) {
      Direction[] horizontalDirections = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
      Direction[] var3 = horizontalDirections;
      int var4 = horizontalDirections.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Direction d = var3[var5];
         BlockPos adj = pos.offset(d);
         BlockState st = mc.world.getBlockState(adj);
         if (!st.isOf(Blocks.STONE)) {
            return false;
         }
      }

      return true;
   }

   private boolean isEnclosedByNonObsidian(BlockPos pos) {
      Direction[] horizontalDirections = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
      Direction[] var3 = horizontalDirections;
      int var4 = horizontalDirections.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Direction d = var3[var5];
         BlockPos adj = pos.offset(d);
         BlockState st = mc.world.getBlockState(adj);
         if (st.isOf(Blocks.OBSIDIAN)) {
            return false;
         }
      }

      return true;
   }

   private BlockPos checkHasLongDripstone(ChunkPos chunkPos) {
      if (mc.world == null) {
         return null;
      } else {
         int startX = chunkPos.getStartX();
         int startZ = chunkPos.getStartZ();
         int worldMinY = mc.world.getBottomY();
         int worldMaxY = 319;

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = worldMaxY; y >= worldMinY; --y) {
                  BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                  BlockState state = mc.world.getBlockState(pos);
                  if (state.getBlock() == Blocks.POINTED_DRIPSTONE && this.isTopOfDripstone(pos, state)) {
                     int length = 1;

                     for(BlockPos current = pos.down(); current.getY() >= worldMinY && length < 50; current = current.down()) {
                        BlockState currentState = mc.world.getBlockState(current);
                        if (currentState.getBlock() != Blocks.POINTED_DRIPSTONE || !currentState.contains(Properties.VERTICAL_DIRECTION) || currentState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
                           break;
                        }

                        ++length;
                     }

                     if (length >= 7) {
                        return pos;
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private boolean isTopOfDripstone(BlockPos pos, BlockState state) {
      if (mc.world == null) {
         return false;
      } else if (state.getBlock() != Blocks.POINTED_DRIPSTONE) {
         return false;
      } else if (!state.contains(Properties.VERTICAL_DIRECTION)) {
         return false;
      } else if (state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
         return false;
      } else {
         BlockPos above = pos.up();
         BlockState aboveState = mc.world.getBlockState(above);
         return aboveState.getBlock() != Blocks.POINTED_DRIPSTONE;
      }
   }

   private int measureDripstoneLength(BlockPos startPos) {
      if (mc.world == null) {
         return 0;
      } else {
         BlockState startState = mc.world.getBlockState(startPos);
         if (startState.getBlock() != Blocks.POINTED_DRIPSTONE) {
            return 0;
         } else if (!startState.contains(Properties.VERTICAL_DIRECTION)) {
            return 0;
         } else if (startState.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
            return 0;
         } else {
            int length = 1;

            for(BlockPos current = startPos.down(); length < 30; current = current.down()) {
               BlockState state = mc.world.getBlockState(current);
               if (state.getBlock() != Blocks.POINTED_DRIPSTONE || !state.contains(Properties.VERTICAL_DIRECTION) || state.get(Properties.VERTICAL_DIRECTION) != Direction.DOWN) {
                  break;
               }

               ++length;
            }

            return length;
         }
      }
   }

   private BlockPos checkHasLongVine(ChunkPos chunkPos) {
      if (mc.world == null) {
         return null;
      } else {
         int startX = chunkPos.getStartX();
         int startZ = chunkPos.getStartZ();
         Set<BlockPos> processedVineTops = ConcurrentHashMap.newKeySet();
         int scanTopY = 319;

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = scanTopY; y >= 40; --y) {
                  BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                  if (!processedVineTops.contains(pos)) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() == Blocks.VINE) {
                        BlockPos topPos = pos.up();
                        BlockState topState = mc.world.getBlockState(topPos);
                        boolean isVineTop = topState.getBlock() != Blocks.VINE && (topState.isSolidBlock(mc.world, topPos) || !topState.isAir());
                        if (isVineTop) {
                           processedVineTops.add(pos);
                           int vineLength = 1;

                           for(BlockPos current = pos.down(); current.getY() >= Math.max(mc.world.getBottomY(), 40); current = current.down()) {
                              BlockState currentState = mc.world.getBlockState(current);
                              if (currentState.getBlock() != Blocks.VINE) {
                                 break;
                              }

                              ++vineLength;
                           }

                           if (vineLength >= 30) {
                              return pos;
                           }
                        }
                     }
                  }
               }
            }
         }

         return null;
      }
   }

   private BlockPos checkAllKelpFullyGrown(ChunkPos chunkPos) {
      if (mc.world == null) {
         return null;
      } else {
         int startX = chunkPos.getStartX();
         int startZ = chunkPos.getStartZ();
         int kelpPlantsFound = 0;
         int fullKelpPlants = 0;
         BlockPos firstKelpPos = null;
         Set<BlockPos> processedKelpBases = ConcurrentHashMap.newKeySet();
         int worldMinY = mc.world.getBottomY();
         int worldMaxY = 319;

         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               for(int y = worldMinY; y <= worldMaxY; ++y) {
                  BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                  if (!processedKelpBases.contains(pos)) {
                     BlockState state = mc.world.getBlockState(pos);
                     if (state.getBlock() == Blocks.KELP || state.getBlock() == Blocks.KELP_PLANT) {
                        BlockPos belowPos = pos.down();
                        BlockState belowState = mc.world.getBlockState(belowPos);
                        boolean isKelpBase = belowState.getBlock() != Blocks.KELP && belowState.getBlock() != Blocks.KELP_PLANT;
                        if (isKelpBase) {
                           processedKelpBases.add(pos);
                           if (firstKelpPos == null) {
                              firstKelpPos = pos;
                           }

                           BlockPos current = pos.up();
                           boolean reachedWaterSurface = false;

                           int kelpLength;
                           for(kelpLength = 1; current.getY() <= worldMaxY; current = current.up()) {
                              BlockState currentState = mc.world.getBlockState(current);
                              if (currentState.getBlock() != Blocks.KELP && currentState.getBlock() != Blocks.KELP_PLANT) {
                                 if (currentState.getFluidState().isEmpty()) {
                                    reachedWaterSurface = true;
                                 }
                                 break;
                              }

                              ++kelpLength;
                           }

                           if (kelpLength >= 6 || !reachedWaterSurface) {
                              ++kelpPlantsFound;
                              if (reachedWaterSurface) {
                                 ++fullKelpPlants;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         if (kelpPlantsFound < 10) {
            return null;
         } else {
            boolean allFull = kelpPlantsFound > 0 && kelpPlantsFound == fullKelpPlants;
            return allFull ? firstKelpPos : null;
         }
      }
   }

   private void evaluateChunk(ChunkPos pos, ChunkFinder.ChunkAnalysis analysis) {
      boolean suspicious = false;
      StringBuilder reasons = new StringBuilder();
      if (analysis.rotatedCount >= 1) {
         suspicious = true;
         reasons.append("Rotated Deepslate: ").append(analysis.rotatedCount).append(" ");
      }

      if (analysis.hasLongDripstone) {
         suspicious = true;
         reasons.append("Long Dripstone ");
      }

      if (analysis.hasLongVine) {
         suspicious = true;
         reasons.append("Long Vine ");
      }

      if (analysis.allKelpFull) {
         suspicious = true;
         reasons.append("Grown Kelp ");
      }

      if (analysis.hasDioriteVein) {
         suspicious = true;
         reasons.append("Diorite Vein ");
      }

      if (analysis.hasObsidianVein) {
         suspicious = true;
         reasons.append("Obsidian Vein ");
      }

      if (analysis.amethystCount >= 1) {
         suspicious = true;
         reasons.append("Amethyst Cluster ");
      }

      if (suspicious) {
         if (this.flaggedChunks.add(pos)) {
           int susBlockX;
           int susBlockZ;
            if (analysis.susBlockPos != null) {
               susBlockX = analysis.susBlockPos.getX();
               susBlockZ = analysis.susBlockPos.getZ();
            } else {
                return;
            }

             if (mc.player != null && mc.player != null) {
               mc.player.sendMessage(Text.of("§cSuspicious Chunk: " + reasons.toString()), false);
            }

            try {
               mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F));
            } catch (Throwable var10) {
            }

            try {
               if ((Boolean)this.alertCoorrds.getValue()) {
                  mc.execute(() -> {
                     mc.getToastManager().add(new SystemToast(net.minecraft.client.toast.SystemToast.Type.WORLD_ACCESS_FAILURE, Text.of("ChunkFinder"), Text.of("X: " + susBlockX + " Z: " + susBlockZ)));
                  });
               } else {
                  mc.execute(() -> {
                     mc.getToastManager().add(new SystemToast(net.minecraft.client.toast.SystemToast.Type.WORLD_ACCESS_FAILURE, Text.of("ChunkFinder"), Text.of("Suspicious Chunk Detected")));
                  });
               }
            } catch (Throwable var9) {
            }
         }
      } else {
         this.flaggedChunks.remove(pos);
      }

   }

   private boolean isNormalDeepslate(BlockState state) {
      return state.getBlock() == Blocks.DEEPSLATE;
   }

   private boolean isRotatedDeepslate(BlockState state) {
      if (!state.contains(Properties.AXIS)) {
         return false;
      } else {
         Axis axis = (Axis)state.get(Properties.AXIS);
         if (axis == Axis.Y) {
            return false;
         } else {
            Block block = state.getBlock();
            return block == Blocks.DEEPSLATE;
         }
      }
   }

   private static class ChunkAnalysis {
      int deepslateCount = 0;
      int rotatedCount = 0;
      boolean hasLongDripstone = false;
      boolean hasLongVine = false;
      boolean allKelpFull = false;
      boolean hasDioriteVein = false;
      boolean hasObsidianVein = false;
      int amethystCount = 0;
      BlockPos susBlockPos = null;
   }

   private static enum SuspiciousType {
      DEEPSLATE,
      ROTATED_DEEPSLATE,
      LONG_DRIPSTONE,
      LONG_VINE,
      FULL_KELP,
      DIORITE_VEIN,
      OBSIDIAN_VEIN,
      AMETHYST_CLUSTER;

      // $FF: synthetic method
      private static ChunkFinder.SuspiciousType[] $values() {
         return new ChunkFinder.SuspiciousType[]{DEEPSLATE, ROTATED_DEEPSLATE, LONG_DRIPSTONE, LONG_VINE, FULL_KELP, DIORITE_VEIN, OBSIDIAN_VEIN, AMETHYST_CLUSTER};
      }
   }
}
