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
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.ColorUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

@ModuleInfo(
   name = "ClusterESP",
   category = Category.RENDER,
   desc = "Highlights Amethyst Clusters"
)
public final class ClusterESP extends Function {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Filled", new String[]{"Box", "Filled", "Fade", "Full"});
   private final BooleanSetting outline = new BooleanSetting("Outline", true);
   private final NumberSetting alpha = new NumberSetting("Alpha", this, 125.0D, 1.0D, 255.0D, 1.0D);
   private final BooleanSetting tracers = new BooleanSetting("Tracers", true);
   private final Set<BlockPos> clusters = ConcurrentHashMap.newKeySet();
   private final Set<ChunkPos> scannedChunks = ConcurrentHashMap.newKeySet();
   private final ExecutorService scanExecutor = Executors.newFixedThreadPool(4);
   private static final int[][] FACE_INDICES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final float[] FACE_SHADING = new float[]{0.5F, 1.0F, 0.7F, 0.8F, 0.6F, 0.9F};

   public ClusterESP() {
      this.addSettings(new Setting[]{this.mode, this.outline, this.alpha, this.tracers});
   }

   public void onEnable() {
      super.onEnable();
      this.clusters.clear();
      this.scannedChunks.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.clusters.clear();
      this.scannedChunks.clear();
   }

   @Subscribe
   public void onTick(EventUpdate event) {
      if (mc.world != null && mc.player != null) {
         if (mc.player.age % 20 == 0) {
            List<WorldChunk> loadedChunks = this.getLoadedChunks();
            Set<ChunkPos> loadedPositions = (Set)loadedChunks.stream().map(Chunk::getPos).collect(Collectors.toSet());
            Iterator var4 = loadedChunks.iterator();

            while(var4.hasNext()) {
               WorldChunk chunk = (WorldChunk)var4.next();
               if (this.scannedChunks.add(chunk.getPos())) {
                  this.scanExecutor.submit(() -> {
                     this.scanChunk(chunk);
                  });
               }
            }

            this.clusters.removeIf((pos) -> {
               return !loadedPositions.contains(new ChunkPos(pos));
            });
            this.scannedChunks.removeIf((cp) -> {
               return !loadedPositions.contains(cp);
            });
         }

      }
   }

   private List<WorldChunk> getLoadedChunks() {
      List<WorldChunk> chunks = new ArrayList();
      int viewDist = (Integer)mc.options.getViewDistance().getValue();
      int px = mc.player.getChunkPos().x;
      int pz = mc.player.getChunkPos().z;

      for(int x = px - viewDist; x <= px + viewDist; ++x) {
         for(int z = pz - viewDist; z <= pz + viewDist; ++z) {
            WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(x, z);
            if (chunk != null) {
               chunks.add(chunk);
            }
         }
      }

      return chunks;
   }

   private void scanChunk(WorldChunk chunk) {
      Mutable mutable = new Mutable();
      int minY = Math.max(mc.world.getBottomY(), -64);
      int maxY = Math.min(mc.world.getBottomY() + mc.world.getHeight(), 64);

      for(int x = 0; x < 16; ++x) {
         for(int z = 0; z < 16; ++z) {
            for(int y = minY; y < maxY; ++y) {
               mutable.set(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
               if (chunk.getBlockState(mutable).isOf(Blocks.AMETHYST_CLUSTER)) {
                  this.clusters.add(mutable.toImmutable());
               }
            }
         }
      }

   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
   }

   @Subscribe
   public void onRender2D(RenderEvent event) {
      if (!this.clusters.isEmpty()) {
         Renderer2D r = event.renderer();
         float centerX = event.scaledWidth() / 2.0F;
         float centerY = event.scaledHeight() / 2.0F;
         Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
         Color baseColor = new Color(200, 100, 255);
         int tColor = (new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255)).getRGB();
         Iterator var8 = this.clusters.iterator();

         while(true) {
            BlockPos pos;
            Vec3d[] corners;
            Vec3d[] screenCorners;
            int onScreenCount;
            do {
               if (!var8.hasNext()) {
                  return;
               }

               pos = (BlockPos)var8.next();
               Box b = new Box(pos);
               corners = new Vec3d[]{new Vec3d(b.minX, b.minY, b.minZ), new Vec3d(b.maxX, b.minY, b.minZ), new Vec3d(b.maxX, b.minY, b.maxZ), new Vec3d(b.minX, b.minY, b.maxZ), new Vec3d(b.minX, b.maxY, b.minZ), new Vec3d(b.maxX, b.maxY, b.minZ), new Vec3d(b.maxX, b.maxY, b.maxZ), new Vec3d(b.minX, b.maxY, b.maxZ)};
               screenCorners = new Vec3d[8];
               onScreenCount = 0;

               for(int i = 0; i < 8; ++i) {
                  screenCorners[i] = ProjectionUtil.toScreen(corners[i]);
                  if (screenCorners[i] != null) {
                     ++onScreenCount;
                  }
               }
            } while(onScreenCount == 0);

            String currentMode = (String)this.mode.getValue();
            if (!"Box".equals(currentMode)) {
               List<Integer> visibleFaces = new ArrayList();

               int fillAlpha;
               for(fillAlpha = 0; fillAlpha < 6; ++fillAlpha) {
                  visibleFaces.add(fillAlpha);
               }

               final Vec3d[] finalCorners = corners;
               final Vec3d finalCamPos = camPos;

               visibleFaces.sort((f1, f2) -> {
                  double d1 = this.getFaceDistSq(finalCorners, FACE_INDICES[f1], finalCamPos);
                  double d2 = this.getFaceDistSq(finalCorners, FACE_INDICES[f2], finalCamPos);
                  return Double.compare(d2, d1);
               });
               fillAlpha = ((Double)this.alpha.getValue()).intValue();
               if ("Fade".equals(currentMode)) {
                  fillAlpha = ColorUtil.getDynamicFadeVal();
               } else if ("Full".equals(currentMode)) {
                  fillAlpha = 200;
               }

               Iterator var17 = visibleFaces.iterator();

               while(var17.hasNext()) {
                  int faceIdx = (Integer)var17.next();
                  int[] indices = FACE_INDICES[faceIdx];
                  Vec3d p1 = screenCorners[indices[0]];
                  Vec3d p2 = screenCorners[indices[1]];
                  Vec3d p3 = screenCorners[indices[2]];
                  Vec3d p4 = screenCorners[indices[3]];
                  if (p1 != null && p2 != null && p3 != null && p4 != null) {
                     float shading = FACE_SHADING[faceIdx];
                     Color shadedColor = new Color((int)((float)baseColor.getRed() * shading), (int)((float)baseColor.getGreen() * shading), (int)((float)baseColor.getBlue() * shading), fillAlpha);
                     r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, shadedColor.getRGB());
                  }
               }
            }

            if ((Boolean)this.outline.getValue()) {
               float thickness = 1.0F;
               this.drawSafeLine(r, screenCorners[0], screenCorners[1], thickness, tColor);
               this.drawSafeLine(r, screenCorners[1], screenCorners[2], thickness, tColor);
               this.drawSafeLine(r, screenCorners[2], screenCorners[3], thickness, tColor);
               this.drawSafeLine(r, screenCorners[3], screenCorners[0], thickness, tColor);
               this.drawSafeLine(r, screenCorners[4], screenCorners[5], thickness, tColor);
               this.drawSafeLine(r, screenCorners[5], screenCorners[6], thickness, tColor);
               this.drawSafeLine(r, screenCorners[6], screenCorners[7], thickness, tColor);
               this.drawSafeLine(r, screenCorners[7], screenCorners[4], thickness, tColor);
               this.drawSafeLine(r, screenCorners[0], screenCorners[4], thickness, tColor);
               this.drawSafeLine(r, screenCorners[1], screenCorners[5], thickness, tColor);
               this.drawSafeLine(r, screenCorners[2], screenCorners[6], thickness, tColor);
               this.drawSafeLine(r, screenCorners[3], screenCorners[7], thickness, tColor);
            }

            if ((Boolean)this.tracers.getValue()) {
               Vec3d center = new Vec3d((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
               Vec3d screenCenter = ProjectionUtil.toScreen(center);
               if (screenCenter != null) {
                  r.line(centerX, centerY, (float)screenCenter.x, (float)screenCenter.y, 1.0F, tColor);
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
}
