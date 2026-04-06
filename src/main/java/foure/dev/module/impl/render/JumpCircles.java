package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.JumpEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.ProjectionUtil;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "JumpCircles",
   category = Category.RENDER,
   desc = "Spawns glowing circles when you jump"
)
public class JumpCircles extends Function {
   private final ModeSetting mode = new ModeSetting("Mode", this, "Both", new String[]{"Filled", "Outline", "Both"});
   private final ColorSetting color = new ColorSetting("Color", this, new Color(0, 255, 255));
   private final NumberSetting radius = new NumberSetting("Max Radius", this, 1.5D, 0.5D, 5.0D, 0.1D);
   private final NumberSetting speed = new NumberSetting("Speed", this, 2.0D, 0.1D, 5.0D, 0.1D);
   private final NumberSetting lineWidth = new NumberSetting("Width", this, 2.0D, 0.5D, 5.0D, 0.1D);
   private final NumberSetting lineAlpha = new NumberSetting("Line Alpha", this, 255.0D, 0.0D, 255.0D, 1.0D);
   private final List<JumpCircles.Circle> circles = new ArrayList();

   public JumpCircles() {
      this.addSettings(new Setting[]{this.mode, this.color, this.radius, this.speed, this.lineWidth, this.lineAlpha});
   }

   public void onEnable() {
      super.onEnable();
      this.circles.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.circles.clear();
   }

   @Subscribe
   public void onJump(JumpEvent event) {
      PlayerEntity p = event.getPlayer();
      this.circles.add(new JumpCircles.Circle(new Vec3d(p.getX(), p.getY(), p.getZ()), System.currentTimeMillis()));
   }

   @Subscribe
   public void onRender2D(RenderEvent event) {
      if (!this.circles.isEmpty()) {
         Renderer2D r = event.renderer();
         long now = System.currentTimeMillis();
         List<JumpCircles.Circle> toRemove = new ArrayList();
         Iterator var6 = this.circles.iterator();

         while(var6.hasNext()) {
            JumpCircles.Circle circle = (JumpCircles.Circle)var6.next();
            double age = (double)(now - circle.startTime) / 1000.0D;
            double currentRadius = age * (Double)this.speed.getValue();
            if (currentRadius > (Double)this.radius.getValue()) {
               toRemove.add(circle);
            } else {
               double alphaFactor = 1.0D - Math.pow(currentRadius / (Double)this.radius.getValue(), 2.0D);
               if (alphaFactor < 0.0D) {
                  alphaFactor = 0.0D;
               }

               Color c = (Color)this.color.getValue();
               int alpha = (int)((double)c.getAlpha() * alphaFactor);
               if (alpha > 255) {
                  alpha = 255;
               }

               Color renderColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
               int colorRgb = renderColor.getRGB();
               this.drawProjectedCircle(r, circle.pos, currentRadius, colorRgb);
            }
         }

         this.circles.removeAll(toRemove);
      }
   }

   private void drawProjectedCircle(Renderer2D r, Vec3d center, double radius, int color) {
      int segments = 100;
      double increment = 6.283185307179586D / (double)segments;
      List<Vec3d> screenPoints = new ArrayList();
      Vec3d screenCenter = ProjectionUtil.toScreen(center);

      Vec3d p1;
      for(int i = 0; i < segments; ++i) {
         double angle = (double)i * increment;
         p1 = new Vec3d(center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius);
         screenPoints.add(ProjectionUtil.toScreen(p1));
      }

      boolean filled = ((String)this.mode.getValue()).equals("Filled") || ((String)this.mode.getValue()).equals("Both");
      boolean outline = ((String)this.mode.getValue()).equals("Outline") || ((String)this.mode.getValue()).equals("Both");
      if (filled && screenCenter != null) {
         for(int i = 0; i < segments; ++i) {
            p1 = (Vec3d)screenPoints.get(i);
            Vec3d p2 = (Vec3d)screenPoints.get((i + 1) % segments);
            if (p1 != null && p2 != null) {
               r.quad((float)screenCenter.x, (float)screenCenter.y, (float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, (float)p2.x, (float)p2.y, color);
            }
         }
      }

      if (outline) {
         float width = ((Double)this.lineWidth.getValue()).floatValue();
         int lineAlphaVal = ((Double)this.lineAlpha.getValue()).intValue();
         int fadeAlpha = color >> 24 & 255;
         float fadeRatio = (float)fadeAlpha / 255.0F;
         int finalLineAlpha = (int)((float)lineAlphaVal * fadeRatio);
         if (finalLineAlpha > 255) {
            finalLineAlpha = 255;
         }

         if (finalLineAlpha < 0) {
            finalLineAlpha = 0;
         }

         int lineColor = color & 16777215 | finalLineAlpha << 24;

         for(int i = 0; i < segments; ++i) {
            p1 = (Vec3d)screenPoints.get(i);
            Vec3d p2 = (Vec3d)screenPoints.get((i + 1) % segments);
            if (p1 != null && p2 != null) {
               r.line((float)p1.x, (float)p1.y, (float)p2.x, (float)p2.y, width, lineColor);
            }
         }
      }

   }

   private static record Circle(Vec3d pos, long startTime) {
      private Circle(Vec3d pos, long startTime) {
         this.pos = pos;
         this.startTime = startTime;
      }

      public Vec3d pos() {
         return this.pos;
      }

      public long startTime() {
         return this.startTime;
      }
   }
}
