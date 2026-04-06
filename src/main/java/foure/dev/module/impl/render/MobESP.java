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
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.ui.notification.NotificationType;
import foure.dev.util.math.MathUtil;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.ColorUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.mob.SpiderEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "MobESP",
   category = Category.RENDER,
   desc = "Highlights mobs and notifies on detection"
)
public class MobESP extends Function {
   private final List<MobESP.MobBox> boxesToRender = new ArrayList();
   private final Set<Integer> notifiedEntities = new HashSet();
   private final MultiBoxSetting mobs = new MultiBoxSetting("Mobs", new BooleanSetting[]{new BooleanSetting("Zombie", true), new BooleanSetting("Skeleton", true), new BooleanSetting("Creeper", true), new BooleanSetting("Enderman", true), new BooleanSetting("Spider", true), new BooleanSetting("Animal", false)});
   private final BooleanSetting notifier = new BooleanSetting("Notifier", this, true);
   private final ModeSetting mode = new ModeSetting("Mode", this, "Filled", new String[]{"Box", "Filled", "Fade", "Full"});
   private final BooleanSetting outline = new BooleanSetting("Outline", this, true);
   private final BooleanSetting tracer = new BooleanSetting("Tracer", this, false);
   private final NumberSetting tracerAlpha = new NumberSetting("Tracer Alpha", this, 255.0D, 10.0D, 255.0D, 1.0D);
   private final NumberSetting range = new NumberSetting("Range", this, 128.0D, 0.0D, 256.0D, 1.0D);
   private final NumberSetting lineWidth = new NumberSetting("LineWidth", this, 2.0D, 0.5D, 5.0D, 0.1D);
   private final ColorSetting mobColor = new ColorSetting("MobColor", this, new Color(255, 100, 100, 255));
   private final BooleanSetting debug = new BooleanSetting("Debug", this, false);
   private static final int[][] FACE_INDICES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final float[] FACE_SHADING = new float[]{0.5F, 1.0F, 0.7F, 0.8F, 0.6F, 0.9F};

   public MobESP() {
      this.addSettings(new Setting[]{this.mobs, this.notifier, this.mode, this.outline, this.tracer, this.tracerAlpha, this.range, this.lineWidth, this.mobColor, this.debug});
   }

   public void onEnable() {
      super.onEnable();
      this.notifiedEntities.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.notifiedEntities.clear();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         ;
      }
   }

   private boolean isValid(Entity entity) {
      if (entity != null && entity.isAlive()) {
         if (entity instanceof ZombieEntity && this.mobs.is("Zombie")) {
            return true;
         } else if (entity instanceof SkeletonEntity && this.mobs.is("Skeleton")) {
            return true;
         } else if (entity instanceof CreeperEntity && this.mobs.is("Creeper")) {
            return true;
         } else if (entity instanceof EndermanEntity && this.mobs.is("Enderman")) {
            return true;
         } else if (entity instanceof SpiderEntity && this.mobs.is("Spider")) {
            return true;
         } else {
            return entity instanceof AnimalEntity && this.mobs.is("Animal");
         }
      } else {
         return false;
      }
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck()) {
         this.boxesToRender.clear();

         try {
            double rangeSq = (Double)this.range.getValue() * (Double)this.range.getValue();
            float tickDelta = MathUtil.getTickDelta();
            Iterator var5 = mc.world.getEntities().iterator();

            while(var5.hasNext()) {
               Entity entity = (Entity)var5.next();
               if (entity != mc.player && this.isValid(entity)) {
                  double distSq = entity.squaredDistanceTo(mc.player);
                  if (!(distSq > rangeSq)) {
                     if ((Boolean)this.notifier.getValue() && !this.notifiedEntities.contains(entity.getId())) {
                        NotificationManager.add("Mob Detected", "Found " + entity.getType().getName().getString(), NotificationType.WARNING);
                        this.notifiedEntities.add(entity.getId());
                     }

                     double x = MathHelper.lerp((double)tickDelta, entity.lastX, entity.getX());
                     double y = MathHelper.lerp((double)tickDelta, entity.lastY, entity.getY());
                     double z = MathHelper.lerp((double)tickDelta, entity.lastZ, entity.getZ());
                     Box bb = entity.getBoundingBox().offset(-entity.getX(), -entity.getY(), -entity.getZ()).offset(x, y, z);
                     Color color = (Color)this.mobColor.getValue();
                     this.boxesToRender.add(new MobESP.MobBox(bb, color));
                  }
               }
            }
         } catch (Exception var17) {
            var17.printStackTrace();
         }

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
            Vec3d[] corners;
            Vec3d[] screenCorners;
            int onScreenCount;
            do {
               if (!var5.hasNext()) {
                  return;
               }

               MobESP.MobBox mBox = (MobESP.MobBox)var5.next();
               b = mBox.box;
               c = mBox.color;
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
               fillAlpha = 100;
               if ("Fade".equals(currentMode)) {
                  fillAlpha = ColorUtil.getDynamicFadeVal();
               } else if ("Full".equals(currentMode)) {
                  fillAlpha = 200;
               }

               Iterator var15 = visibleFaces.iterator();

               while(var15.hasNext()) {
                  int faceIdx = (Integer)var15.next();
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
               this.drawSafeLine(r, screenCorners[0], screenCorners[1], thickness, color);
               this.drawSafeLine(r, screenCorners[1], screenCorners[2], thickness, color);
               this.drawSafeLine(r, screenCorners[2], screenCorners[3], thickness, color);
               this.drawSafeLine(r, screenCorners[3], screenCorners[0], thickness, color);
               this.drawSafeLine(r, screenCorners[4], screenCorners[5], thickness, color);
               this.drawSafeLine(r, screenCorners[5], screenCorners[6], thickness, color);
               this.drawSafeLine(r, screenCorners[6], screenCorners[7], thickness, color);
               this.drawSafeLine(r, screenCorners[7], screenCorners[4], thickness, color);
               this.drawSafeLine(r, screenCorners[0], screenCorners[4], thickness, color);
               this.drawSafeLine(r, screenCorners[1], screenCorners[5], thickness, color);
               this.drawSafeLine(r, screenCorners[2], screenCorners[6], thickness, color);
               this.drawSafeLine(r, screenCorners[3], screenCorners[7], thickness, color);
            }

            if ((Boolean)this.tracer.getValue()) {
               Vec3d center = new Vec3d((b.minX + b.maxX) / 2.0D, b.minY, (b.minZ + b.maxZ) / 2.0D);
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

   private static record MobBox(Box box, Color color) {
      private MobBox(Box box, Color color) {
         this.box = box;
         this.color = color;
      }

      public Box box() {
         return this.box;
      }

      public Color color() {
         return this.color;
      }
   }
}
