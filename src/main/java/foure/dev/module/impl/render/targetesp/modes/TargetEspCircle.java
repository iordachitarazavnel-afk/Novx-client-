package foure.dev.module.impl.render.targetesp.modes;

import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.impl.render.targetesp.TargetEspMode;
import foure.dev.module.impl.render.targetesp.TargetEspModule;
import foure.dev.util.math.MathUtil;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class TargetEspCircle extends TargetEspMode {
   private float moving = 0.0F;
   private float prevMoving = 0.0F;
   private float verticalTime = 0.0F;
   private float prevVerticalTime = 0.0F;
   private float impactProgress = 0.0F;
   private int prevHurtTime = 0;

   public void onUpdate() {
      if (this.currentTarget != null && this.canDraw()) {
         TargetEspModule module = TargetEspModule.getInstance();
         this.prevMoving = this.moving;
         this.moving = (float)((double)this.moving + (Double)module.circleSpeed.getValue());
         this.prevVerticalTime = this.verticalTime;
         this.verticalTime = (float)((double)this.verticalTime + (Double)module.circleSpeed.getValue());
         this.updateImpactAnimation();
      }
   }

   private void updateImpactAnimation() {
      TargetEspModule module = TargetEspModule.getInstance();
      if ((Boolean)module.circleRedOnImpact.getValue() && this.currentTarget != null) {
         float fadeInSpeed = module.circleImpactFadeIn.getValueFloat();
         float fadeOutSpeed = module.circleImpactFadeOut.getValueFloat();
         float maxIntensity = module.circleImpactIntensity.getValueFloat();
         int currentHurtTime = this.currentTarget.hurtTime;
         if (currentHurtTime <= this.prevHurtTime && (currentHurtTime <= 0 || this.prevHurtTime != 0)) {
            if (currentHurtTime > 0) {
               this.impactProgress = Math.min(maxIntensity, this.impactProgress + fadeInSpeed * 0.5F);
            } else {
               this.impactProgress = Math.max(0.0F, this.impactProgress - fadeOutSpeed);
            }
         } else {
            this.impactProgress = Math.min(maxIntensity, this.impactProgress + fadeInSpeed);
         }

         this.prevHurtTime = currentHurtTime;
      } else {
         this.impactProgress = 0.0F;
         this.prevHurtTime = 0;
      }
   }

   public void onRender3D(Render3DEvent event) {
      if (this.currentTarget != null && this.canDraw()) {
         TargetEspModule module = TargetEspModule.getInstance();
      }
   }

   public void onRender2D(RenderEvent event) {
      if (this.currentTarget != null && this.canDraw()) {
         TargetEspModule module = TargetEspModule.getInstance();
         Renderer2D r = event.renderer();
         float alphaPC = module.showAnimation.getValueFloat();
         float tickDelta = MathUtil.getTickDelta();
         Vec3d vec = this.currentTarget.getLerpedPos(tickDelta);
         float width = this.currentTarget.getWidth() * 1.45F + (1.0F - alphaPC) / 2.5F;
         float baseVal = Math.max(0.5F, 0.7F - 0.1F * this.impactProgress + 0.1F - 0.1F * alphaPC);
         float movingAngle = MathUtil.interpolate(this.prevMoving, this.moving, tickDelta);
         int step = 4;
         float interpolatedVerticalTime = MathUtil.interpolate(this.prevVerticalTime, this.verticalTime, tickDelta);
         List<Vec3d> screenPoints = new ArrayList();
         List<Integer> colors = new ArrayList();

         for(int i = 0; i <= 360; i += step) {
            if ((int)((float)i / 45.0F) % 2 != 0) {
               double rad = Math.toRadians((double)((float)i + movingAngle));
               double xOff = Math.sin(rad) * (double)width * (double)baseVal;
               double zOff = Math.cos(rad) * (double)width * (double)baseVal;
               double radAngle = Math.toRadians((double)interpolatedVerticalTime);
               float waveValue = (float)((1.0D - Math.cos(radAngle)) / 2.0D);
               double yOff = (double)(this.currentTarget.getHeight() * waveValue);
               Vec3d worldPos = vec.add(xOff, yOff, zOff);
               Vec3d screen = ProjectionUtil.toScreen(worldPos);
               if (screen != null) {
                  screenPoints.add(screen);
                  int alpha = (int)(alphaPC * 255.0F);
                  Color color = gradient(i * 3, alpha);
                  if (this.impactProgress > 0.0F) {
                     Color impactColor = new Color(255, 32, 32, alpha);
                     color = this.interpolateColor(color, impactColor, this.impactProgress);
                  }

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

         float lineWidth = 2.0F;

         for(int i = 0; i < screenPoints.size() - 1; ++i) {
            Vec3d p1 = (Vec3d)screenPoints.get(i);
            Vec3d p2 = (Vec3d)screenPoints.get(i + 1);
            if (p1 != null && p2 != null) {
               if ((Boolean)module.circleBloom.getValue()) {
                  int col = (Integer)colors.get(i);
                  int alpha = col >> 24 & 255;
                  int bloomAlpha = (int)((float)alpha * 0.3F);
                  int bloomCol = col & 16777215 | bloomAlpha << 24;
                  r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, lineWidth * 3.0F, bloomCol);
               }

               r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, lineWidth, (Integer)colors.get(i));
            }
         }

      }
   }

   private Color interpolateColor(Color color1, Color color2, float progress) {
      progress = Math.max(0.0F, Math.min(1.0F, progress));
      int r1 = color1.getRed();
      int g1 = color1.getGreen();
      int b1 = color1.getBlue();
      int a1 = color1.getAlpha();
      int r2 = color2.getRed();
      int g2 = color2.getGreen();
      int b2 = color2.getBlue();
      int a2 = color2.getAlpha();
      float smoothProgress = progress * progress * (3.0F - 2.0F * progress);
      int r = (int)((float)r1 + (float)(r2 - r1) * smoothProgress);
      int g = (int)((float)g1 + (float)(g2 - g1) * smoothProgress);
      int b = (int)((float)b1 + (float)(b2 - b1) * smoothProgress);
      int a = (int)((float)a1 + (float)(a2 - a1) * smoothProgress);
      return new Color(r, g, b, a);
   }

   public static Color gradient(int speed, int alpha) {
      float[] hsb = Color.RGBtoHSB(255, 0, 255, (float[])null);
      float hue = (float)(System.currentTimeMillis() % 2000L) / 2000.0F;
      int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F);
      Color c = new Color(rgb);
      return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
   }
}
