package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.player.InteractBlockEvent;
import foure.dev.event.impl.render.Render3DEvent;
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
import foure.dev.util.render.ColorUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.world.Dir;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;

@ModuleInfo(
   name = "StorageESP",
   category = Category.RENDER,
   desc = "Highlights storage blocks (2D Pass)"
)
public class StorageESP extends Function {
   private final List<StorageESP.StorageBox> boxesToRender = new ArrayList();
   private final ModeSetting mode = new ModeSetting("Mode", this, "Filled", new String[]{"Box", "Filled", "Fade", "Full"});
   private final BooleanSetting outline = new BooleanSetting("Outline", true);
   private final BooleanSetting tracer = new BooleanSetting("Tracer", false);
   private final NumberSetting tracerAlpha = new NumberSetting("Tracer Alpha", this, 255.0D, 10.0D, 255.0D, 1.0D);
   private final BooleanSetting optimize = new BooleanSetting("OptimizeRender", true);
   private final NumberSetting range = new NumberSetting("Range", this, 128.0D, 0.0D, 256.0D, 1.0D);
   private final NumberSetting lineWidth = new NumberSetting("LineWidth", this, 2.0D, 0.5D, 5.0D, 0.1D);
   private final BooleanSetting depthTest = new BooleanSetting("DepthTest", false);
   private final ColorSetting chestColor = new ColorSetting("ChestColor", this, new Color(255, 160, 0, 255));
   private final ColorSetting trappedChestColor = new ColorSetting("TrappedColor", this, new Color(255, 0, 0, 255));
   private final ColorSetting enderChestColor = new ColorSetting("EnderColor", this, new Color(120, 0, 255, 255));
   private final ColorSetting shulkerColor = new ColorSetting("ShulkerColor", this, new Color(255, 160, 0, 255));
   private final ColorSetting barrelColor = new ColorSetting("BarrelColor", this, new Color(255, 160, 0, 255));
   private final ColorSetting spawnerColor = new ColorSetting("SpawnerColor", this, new Color(0, 255, 0, 255));
   private final ColorSetting otherColor = new ColorSetting("OtherColor", this, new Color(140, 140, 140, 255));
   private final BooleanSetting hideOpened = new BooleanSetting("HideOpened", false);
   private final ColorSetting openedColor = new ColorSetting("OpenedColor", this, new Color(100, 100, 100, 150));
   private final BooleanSetting debug = new BooleanSetting("Debug", false);
   private final Set<BlockPos> interactedBlocks = new HashSet();
   private int lastFrameChestCount = 0;
   private boolean hasLoggedOnce = false;
   private static final int[][] FACE_INDICES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final byte[] FACE_DIRS = new byte[]{4, 2, 8, 16, 32, 64};
   private static final float[] FACE_SHADING = new float[]{0.5F, 1.0F, 0.7F, 0.8F, 0.6F, 0.9F};

   public StorageESP() {
      this.addSettings(new Setting[]{this.mode, this.outline, this.tracer, this.tracerAlpha, this.lineWidth, this.depthTest, this.optimize, this.range, this.debug, this.chestColor, this.trappedChestColor, this.enderChestColor, this.shulkerColor, this.barrelColor, this.spawnerColor, this.otherColor, this.hideOpened, this.openedColor});
   }

   public void onEnable() {
      super.onEnable();
      this.interactedBlocks.clear();
      this.hasLoggedOnce = false;
      if ((Boolean)this.debug.getValue()) {
         System.out.println("[StorageESP] Module enabled");
      }

   }

   public void onDisable() {
      super.onDisable();
      if ((Boolean)this.debug.getValue()) {
         System.out.println("[StorageESP] Module disabled");
      }

   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && mc.player != null) {
         this.boxesToRender.clear();

         try {
            int viewDist = (Integer)mc.options.getViewDistance().getValue();
            int px = mc.player.getChunkPos().x;
            int pz = mc.player.getChunkPos().z;
            double rangeSq = (Double)this.range.getValue() * (Double)this.range.getValue();

            for(int x = px - viewDist; x <= px + viewDist; ++x) {
               label104:
               for(int z = pz - viewDist; z <= pz + viewDist; ++z) {
                  WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(x, z);
                  if (chunk != null) {
                     Iterator var10 = chunk.getBlockEntities().values().iterator();

                     while(true) {
                        BlockEntity be;
                        BlockPos pos;
                        boolean interacted;
                        do {
                           double distSq;
                           do {
                              do {
                                 if (!var10.hasNext()) {
                                    continue label104;
                                 }

                                 be = (BlockEntity)var10.next();
                              } while(!this.isStorage(be));

                              pos = be.getPos();
                              distSq = mc.player.squaredDistanceTo((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D);
                           } while(distSq > rangeSq);

                           interacted = this.interactedBlocks.contains(pos);
                        } while(interacted && (Boolean)this.hideOpened.getValue());

                        Color baseColor = this.getColor(be);
                        if (interacted && this.openedColor.getValue() != null) {
                           Color openColor = (Color)this.openedColor.getValue();
                           if (openColor.getAlpha() > 0) {
                              baseColor = openColor;
                           }
                        }

                        int excludeDir = 0;
                        if (be instanceof ChestBlockEntity) {
                           BlockState state = mc.world.getBlockState(pos);
                           if (state.getBlock() instanceof ChestBlock && state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
                              Direction facing = (Direction)state.get(ChestBlock.FACING);
                              ChestType type = (ChestType)state.get(ChestBlock.CHEST_TYPE);
                              Direction neighborDir = type == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise();
                              excludeDir = Dir.getName(neighborDir);
                           }
                        }

                        double x1 = (double)pos.getX();
                        double y1 = (double)pos.getY();
                        double z1 = (double)pos.getZ();
                        double x2 = (double)(pos.getX() + 1);
                        double y2 = (double)(pos.getY() + 1);
                        double z2 = (double)(pos.getZ() + 1);
                        if (be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity) {
                           double offset = 0.0625D;
                           if (Dir.isNot(excludeDir, (byte)32)) {
                              x1 += offset;
                           }

                           if (Dir.isNot(excludeDir, (byte)8)) {
                              z1 += offset;
                           }

                           if (Dir.isNot(excludeDir, (byte)64)) {
                              x2 -= offset;
                           }

                           y2 -= offset * 2.0D;
                           if (Dir.isNot(excludeDir, (byte)16)) {
                              z2 -= offset;
                           }
                        }

                        this.boxesToRender.add(new StorageESP.StorageBox(new Box(x1, y1, z1, x2, y2, z2), baseColor, excludeDir));
                     }
                  }
               }
            }
         } catch (Exception var32) {
            var32.printStackTrace();
         }

      }
   }

   @Subscribe
   public void onBlockInteract(InteractBlockEvent event) {
      try {
         BlockPos pos = event.getPos();
         if (pos == null) {
            return;
         }

         BlockEntity blockEntity = mc.world.getBlockEntity(pos);
         if (blockEntity == null) {
            return;
         }

         this.interactedBlocks.add(pos);
         if ((Boolean)this.debug.getValue()) {
            System.out.println("[StorageESP] Interacted with block at " + String.valueOf(pos));
         }

         if (blockEntity instanceof ChestBlockEntity) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.getBlock() instanceof ChestBlock) {
               ChestType chestType = (ChestType)state.get(ChestBlock.CHEST_TYPE);
               if (chestType == ChestType.LEFT || chestType == ChestType.RIGHT) {
                  Direction facing = (Direction)state.get(ChestBlock.FACING);
                  BlockPos otherPartPos = pos.offset(chestType == ChestType.LEFT ? facing.rotateYClockwise() : facing.rotateYCounterclockwise());
                  this.interactedBlocks.add(otherPartPos);
                  if ((Boolean)this.debug.getValue()) {
                     System.out.println("[StorageESP] Added double chest other half at " + String.valueOf(otherPartPos));
                  }
               }
            }
         }
      } catch (Exception var8) {
         System.err.println("[StorageESP] Error during block interaction:");
         var8.printStackTrace();
      }

   }

   @Subscribe
   public void onRender2D(RenderEvent event) {
      if (!this.boxesToRender.isEmpty()) {
         Renderer2D r = event.renderer();
         float thickness = ((Double)this.lineWidth.getValue()).floatValue();
         Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
         Iterator var5 = this.boxesToRender.iterator();

         while(true) {
            Box b;
            Color c;
            int exclude;
            Vec3d[] corners;
            Vec3d[] screenCorners;
            int onScreenCount;
            do {
               if (!var5.hasNext()) {
                  return;
               }

               StorageESP.StorageBox storage = (StorageESP.StorageBox)var5.next();
               b = storage.box;
               c = storage.color;
               exclude = storage.excludeDir;
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
                  if (Dir.isNot(exclude, FACE_DIRS[fillAlpha])) {
                     visibleFaces.add(fillAlpha);
                  }
               }

               final Vec3d[] finalCorners = corners;
               final Vec3d finalCamPos = camPos;

               visibleFaces.sort((f1, f2) -> {
                  double d1 = this.getFaceDistSq(finalCorners, FACE_INDICES[f1], finalCamPos);
                  double d2 = this.getFaceDistSq(finalCorners, FACE_INDICES[f2], finalCamPos);
                  return Double.compare(d2, d1);
               });
               fillAlpha = 100;
               if ("Fade".equals(currentMode)) {
                  fillAlpha = ColorUtil.getDynamicFadeVal();
               } else if ("Full".equals(currentMode)) {
                  fillAlpha = 200;
               }

               Iterator var16 = visibleFaces.iterator();

               while(var16.hasNext()) {
                  int faceIdx = (Integer)var16.next();
                  int[] indices = FACE_INDICES[faceIdx];
                  Vec3d p1 = screenCorners[indices[0]];
                  Vec3d p2 = screenCorners[indices[1]];
                  Vec3d p3 = screenCorners[indices[2]];
                  Vec3d p4 = screenCorners[indices[3]];
                  if (p1 != null && p2 != null && p3 != null && p4 != null) {
                     float shading = FACE_SHADING[faceIdx];
                     Color shadedColor = new Color((int)((float)c.getRed() * shading), (int)((float)c.getGreen() * shading), (int)((float)c.getBlue() * shading), fillAlpha);
                     r.quad((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, (float)p3.x, (float)p3.y, (float)p4.x, (float)p4.y, shadedColor.getRGB());
                  }
               }
            }

            if ((Boolean)this.outline.getValue()) {
               int color = c.getRGB();
               if (Dir.isNot(exclude, (byte)4)) {
                  this.drawSafeLine(r, screenCorners[0], screenCorners[1], thickness, color);
                  this.drawSafeLine(r, screenCorners[1], screenCorners[2], thickness, color);
                  this.drawSafeLine(r, screenCorners[2], screenCorners[3], thickness, color);
                  this.drawSafeLine(r, screenCorners[3], screenCorners[0], thickness, color);
               }

               if (Dir.isNot(exclude, (byte)2)) {
                  this.drawSafeLine(r, screenCorners[4], screenCorners[5], thickness, color);
                  this.drawSafeLine(r, screenCorners[5], screenCorners[6], thickness, color);
                  this.drawSafeLine(r, screenCorners[6], screenCorners[7], thickness, color);
                  this.drawSafeLine(r, screenCorners[7], screenCorners[4], thickness, color);
               }

               this.drawSafeLine(r, screenCorners[0], screenCorners[4], thickness, color);
               this.drawSafeLine(r, screenCorners[1], screenCorners[5], thickness, color);
               this.drawSafeLine(r, screenCorners[2], screenCorners[6], thickness, color);
               this.drawSafeLine(r, screenCorners[3], screenCorners[7], thickness, color);
            }

            if ((Boolean)this.tracer.getValue()) {
               Vec3d center = new Vec3d((b.minX + b.maxX) / 2.0D, (b.minY + b.maxY) / 2.0D, (b.minZ + b.maxZ) / 2.0D);
               Vec3d screenCenter = ProjectionUtil.toScreen(center);
               if (screenCenter != null) {
                  int tColor = ColorUtil.changeAlpha(c, ((Double)this.tracerAlpha.getValue()).intValue()).getRGB();
                  r.line(event.scaledWidth() / 2.0F, event.scaledHeight() / 2.0F, (float)screenCenter.x, (float)screenCenter.y, 1.0F, tColor);
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

   private boolean isStorage(BlockEntity be) {
      return be instanceof ChestBlockEntity || be instanceof EnderChestBlockEntity || be instanceof ShulkerBoxBlockEntity || be instanceof BarrelBlockEntity || be instanceof AbstractFurnaceBlockEntity || be instanceof BrewingStandBlockEntity || be instanceof DispenserBlockEntity || be instanceof HopperBlockEntity || be instanceof MobSpawnerBlockEntity;
   }

   private Color getColor(BlockEntity be) {
      if (be instanceof TrappedChestBlockEntity) {
         return (Color)this.trappedChestColor.getValue();
      } else if (be instanceof ChestBlockEntity) {
         return (Color)this.chestColor.getValue();
      } else if (be instanceof BarrelBlockEntity) {
         return (Color)this.barrelColor.getValue();
      } else if (be instanceof ShulkerBoxBlockEntity) {
         return (Color)this.shulkerColor.getValue();
      } else if (be instanceof EnderChestBlockEntity) {
         return (Color)this.enderChestColor.getValue();
      } else if (be instanceof MobSpawnerBlockEntity) {
         return (Color)this.spawnerColor.getValue();
      } else {
         return !(be instanceof AbstractFurnaceBlockEntity) && !(be instanceof BrewingStandBlockEntity) && !(be instanceof DispenserBlockEntity) && !(be instanceof HopperBlockEntity) ? new Color(255, 255, 255, 255) : (Color)this.otherColor.getValue();
      }
   }

   private static record StorageBox(Box box, Color color, int excludeDir) {
      private StorageBox(Box box, Color color, int excludeDir) {
         this.box = box;
         this.color = color;
         this.excludeDir = excludeDir;
      }

      public Box box() {
         return this.box;
      }

      public Color color() {
         return this.color;
      }

      public int excludeDir() {
         return this.excludeDir;
      }
   }
}
