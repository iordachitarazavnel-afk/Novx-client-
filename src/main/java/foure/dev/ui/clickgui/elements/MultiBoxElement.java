package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;
import java.util.Iterator;

public class MultiBoxElement implements SettingElement {
   private final MultiBoxSetting setting;
   private boolean expanded;
   private float expandAnimation = 0.0F;
   private final float headerHeight = 16.0F;
   private final float itemHeight = 12.0F;

   public MultiBoxElement(MultiBoxSetting setting) {
      this.setting = setting;
   }

   private void updateAnimation() {
      float target = this.expanded ? 1.0F : 0.0F;
      this.expandAnimation = this.lerp(this.expandAnimation, target, 0.15F);
   }

   public void render(Renderer2D r, FontObject font, float x, float y, float width, float alpha) {
      this.updateAnimation();
      r.text(font, x + 4.0F, y + 12.0F, 8.0F, this.setting.getName(), (new Color(255, 255, 255, (int)(255.0F * alpha))).getRGB(), "l");
      String indicator = !this.expanded && !(this.expandAnimation > 0.5F) ? " " : " ";
      r.text(font, x + width - 6.0F, y + 12.0F, 8.0F, indicator, (new Color(160, 160, 160, (int)(255.0F * alpha))).getRGB(), "r");
      if (this.expandAnimation > 0.01F) {
         float cy = y + 16.0F;

         for(int i = 0; i < this.setting.getSettings().size(); ++i) {
            BooleanSetting bs = (BooleanSetting)this.setting.getSettings().get(i);
            float elementDelay = (float)i * 0.08F;
            float elementAnimation = Math.max(0.0F, Math.min(1.0F, (this.expandAnimation - elementDelay) / (1.0F - elementDelay)));
            elementAnimation = Easings.EASE_OUT_CUBIC.ease(elementAnimation);
            if (elementAnimation > 0.01F) {
               float yOffset = (1.0F - elementAnimation) * 6.0F;
               float elementAlpha = alpha * elementAnimation;
               int color = (Boolean)bs.getValue() ? (new Color(100, 160, 255, (int)(255.0F * elementAlpha))).getRGB() : (new Color(128, 128, 128, (int)(255.0F * elementAlpha))).getRGB();
               r.text(font, x + 8.0F, cy - yOffset + 9.0F, 7.0F, bs.getName(), color, "l");
            }

            cy += 12.0F * this.expandAnimation;
         }
      }

   }

   public boolean mouseClicked(double mx, double my, int button, float x, float y, float width) {
      if (this.hover(mx, my, x, y, width, 16.0F) && button == 1) {
         this.expanded = !this.expanded;
         return true;
      } else {
         if (this.expanded && this.expandAnimation > 0.5F) {
            float cy = y + 16.0F;

            for(Iterator var10 = this.setting.getSettings().iterator(); var10.hasNext(); cy += 12.0F) {
               BooleanSetting bs = (BooleanSetting)var10.next();
               if (this.hover(mx, my, x, cy, width, 12.0F) && button == 0) {
                  bs.toggle();
                  return true;
               }
            }
         }

         return false;
      }
   }

   public float getHeight() {
      return !this.expanded && this.expandAnimation < 0.01F ? 16.0F : 16.0F + (float)this.setting.getSettings().size() * 12.0F * this.expandAnimation;
   }

   private boolean hover(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   public void updateHover(double mx, double my, float x, float y, float w) {
   }

   public void mouseReleased(double mx, double my, int b) {
   }

   public void mouseDragged(double mx, double my, int b, float x, float y, float w) {
   }

   public boolean mouseClicked(double mx, double my, float x, float y, float w) {
      return false;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }
}
