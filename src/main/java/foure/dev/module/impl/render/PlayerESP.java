package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.render.targetesp.modes.TargetEspCircle;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.MathUtil;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.ColorUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "PlayerESP",
   category = Category.RENDER,
   desc = "Highlights players (2D Pass)"
)
public class PlayerESP extends Function {
   private final List<PlayerESP.PlayerBox> boxesToRender = new ArrayList();
   private final ModeSetting mode = new ModeSetting("Mode", this, "Filled", new String[]{"Box", "Filled", "Fade", "Full", "Circle"});
   private final BooleanSetting outline = new BooleanSetting("Outline", this, true);
   private final BooleanSetting tracer = new BooleanSetting("Tracer", this, false);
   private final NumberSetting tracerAlpha = new NumberSetting("Tracer Alpha", this, 255.0D, 10.0D, 255.0D, 1.0D);
   private final NumberSetting range = new NumberSetting("Range", this, 128.0D, 0.0D, 256.0D, 1.0D);
   private final NumberSetting lineWidth = new NumberSetting("LineWidth", this, 2.0D, 0.5D, 5.0D, 0.1D);
   private final BooleanSetting self = new BooleanSetting("Self", this, false);
   private final ColorSetting playerColor = new ColorSetting("PlayerColor", this, new Color(255, 50, 50, 255));
   private final BooleanSetting debug = new BooleanSetting("Debug", this, false);
   private final NumberSetting circleSpeed = new NumberSetting("Circle Speed", this, 2.0D, 0.1D, 10.0D, 0.1D);
   private final BooleanSetting circleBloom = new BooleanSetting("Circle Bloom", this, true);
   private final NumberSetting circleBloomSize = new NumberSetting("Bloom Size", this, 0.5D, 0.1D, 2.0D, 0.1D);
   private float moving = 0.0F;
   private float prevMoving = 0.0F;
   private float verticalTime = 0.0F;
   private float prevVerticalTime = 0.0F;
   private static final int[][] FACE_INDICES = new int[][]{{0, 1, 2, 3}, {4, 5, 6, 7}, {0, 1, 5, 4}, {3, 2, 6, 7}, {0, 3, 7, 4}, {1, 2, 6, 5}};
   private static final byte[] FACE_DIRS = new byte[]{4, 2, 8, 16, 32, 64};
   private static final float[] FACE_SHADING = new float[]{0.5F, 1.0F, 0.7F, 0.8F, 0.6F, 0.9F};

   public PlayerESP() {
      this.addSettings(new Setting[]{this.mode, this.outline, this.tracer, this.tracerAlpha, this.lineWidth, this.range, this.self, this.debug, this.playerColor, this.circleSpeed, this.circleBloom, this.circleBloomSize});
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (this.mode.is("Circle")) {
         this.prevMoving = this.moving;
         this.moving += this.circleSpeed.getValueFloat();
         this.prevVerticalTime = this.verticalTime;
         this.verticalTime += this.circleSpeed.getValueFloat();
      }

   }

   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (!fullNullCheck()) {
         this.boxesToRender.clear();

         try {
            double rangeSq = (Double)this.range.getValue() * (Double)this.range.getValue();
            float tickDelta = MathUtil.getTickDelta();
            Iterator var5 = mc.world.getPlayers().iterator();

            while(true) {
               AbstractClientPlayerEntity player;
               do {
                  if (!var5.hasNext()) {
                     return;
                  }

                  player = (AbstractClientPlayerEntity)var5.next();
               } while(player == mc.player && !(Boolean)this.self.getValue());

               if (!player.isSpectator() && !(player.squaredDistanceTo(mc.player) > rangeSq)) {
                  double x = MathHelper.lerp((double)tickDelta, player.lastX, player.getX());
                  double y = MathHelper.lerp((double)tickDelta, player.lastY, player.getY());
                  double z = MathHelper.lerp((double)tickDelta, player.lastZ, player.getZ());
                  Box bb = player.getBoundingBox().offset(-player.getX(), -player.getY(), -player.getZ()).offset(x, y, z);
                  Color color = (Color)this.playerColor.getValue();
                  this.boxesToRender.add(new PlayerESP.PlayerBox(bb, color));
               }
            }
         } catch (Exception var15) {
            var15.printStackTrace();
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
            while(true) {
               PlayerESP.PlayerBox pBox;
               Box b;
               Color c;
               Vec3d[] corners;
               Vec3d[] screenCorners;
               int onScreenCount;
               do {
                  if (!var5.hasNext()) {
                     return;
                  }

                  pBox = (PlayerESP.PlayerBox)var5.next();
                  b = pBox.box;
                  c = pBox.color;
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
               if ("Circle".equals(currentMode)) {
                  this.drawCircle(r, pBox);
               } else {
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

   private void drawCircle(Renderer2D r, PlayerESP.PlayerBox pBox) {
      Box b = pBox.box;
      double x = b.minX + (b.maxX - b.minX) / 2.0D;
      double y = b.minY;
      double z = b.minZ + (b.maxZ - b.minZ) / 2.0D;
      float width = 0.8F;
      float baseVal = 0.7F;
      float tickDelta = MathUtil.getTickDelta();
      float movingAngle = MathUtil.interpolate(this.prevMoving, this.moving, tickDelta);
      float interpolatedVerticalTime = MathUtil.interpolate(this.prevVerticalTime, this.verticalTime, tickDelta);
      float size = 0.4F;
      List<Vec3d> screenPoints = new ArrayList();
      List<Integer> colors = new ArrayList();
      int step = 5;

      for(int i = 0; i <= 360; i += step) {
         if ((int)((float)i / 45.0F) % 2 != 0) {
            double rad = Math.toRadians((double)((float)i + movingAngle));
            double xOff = Math.sin(rad) * (double)width * (double)baseVal;
            double zOff = Math.cos(rad) * (double)width * (double)baseVal;
            double radAngle = Math.toRadians((double)interpolatedVerticalTime);
            float waveValue = (float)((1.0D - Math.cos(radAngle)) / 2.0D);
            double yOff = (b.maxY - b.minY) * (double)waveValue;
            Vec3d worldPos = new Vec3d(x + xOff, y + yOff, z + zOff);
            Vec3d screen = ProjectionUtil.toScreen(worldPos);
            if (screen != null) {
               screenPoints.add(screen);
               int alpha = 255;
               Color color = TargetEspCircle.gradient(i * 3, alpha);
               colors.add(color.getRGB());
            } else {
               screenPoints.add((Vec3d) null);
               colors.add(0);
            }
         } else {
            screenPoints.add((Vec3d) null);
            colors.add(0);
         }
      }

      float lWidth = this.lineWidth.getValueFloat();

      for(int i = 0; i < screenPoints.size() - 1; ++i) {
         Vec3d p1 = (Vec3d)screenPoints.get(i);
         Vec3d p2 = (Vec3d)screenPoints.get(i + 1);
         if (p1 != null && p2 != null) {
            if ((Boolean)this.circleBloom.getValue()) {
               int col = (Integer)colors.get(i);
               int alpha = col >> 24 & 255;
               int bloomAlpha = (int)((float)alpha * 0.3F);
               int bloomCol = col & 16777215 | bloomAlpha << 24;
               r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, lWidth * 3.0F, bloomCol);
            }

            r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, lWidth, (Integer)colors.get(i));
         }
      }

   }

   private static record PlayerBox(Box box, Color color) {
      private PlayerBox(Box box, Color color) {
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
