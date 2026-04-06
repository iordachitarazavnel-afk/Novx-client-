package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;
import java.util.Iterator;

public class ModeElement implements SettingElement {
   private final ModeSetting setting;
   private boolean open;
   private float expandAnimation = 0.0F;

   public ModeElement(ModeSetting s) {
      this.setting = s;
   }

   private void updateAnimation() {
      float target = this.open ? 1.0F : 0.0F;
      this.expandAnimation = this.lerp(this.expandAnimation, target, 0.15F);
   }

   public void render(Renderer2D r, FontObject f, float x, float y, float w, float a) {
      this.updateAnimation();
      r.text(f, x + 4.0F, y + 10.0F, 8.0F, this.setting.getName(), (new Color(255, 255, 255, (int)(255.0F * a))).getRGB(), "l");
      r.text(f, x + w - 4.0F, y + 10.0F, 8.0F, (String)this.setting.getValue(), (new Color(160, 160, 160, (int)(255.0F * a))).getRGB(), "r");
      if (this.expandAnimation > 0.01F) {
         float iy = y + 16.0F;

         for(int i = 0; i < this.setting.getModes().size(); ++i) {
            String m = (String)this.setting.getModes().get(i);
            boolean isSelected = m.equals(this.setting.getValue());
            float elementDelay = (float)i * 0.06F;
            float elementAnimation = Math.max(0.0F, Math.min(1.0F, (this.expandAnimation - elementDelay) / (1.0F - elementDelay)));
            elementAnimation = Easings.EASE_OUT_CUBIC.ease(elementAnimation);
            if (elementAnimation > 0.01F) {
               float yOffset = (1.0F - elementAnimation) * 5.0F;
               float elementAlpha = a * elementAnimation;
               int color = isSelected ? (new Color(120, 170, 220, (int)(255.0F * elementAlpha))).getRGB() : (new Color(128, 128, 128, (int)(255.0F * elementAlpha))).getRGB();
               r.text(f, x + 6.0F, iy - yOffset + 8.0F, 7.0F, m, color, "l");
            }

            iy += 12.0F * this.expandAnimation;
         }
      }

   }

   public boolean mouseClicked(double mx, double my, int b, float x, float y, float w) {
      if (this.isHovered(mx, my, x, y, w, 16.0F) && b == 1) {
         this.open = !this.open;
         return true;
      } else {
         if (this.open && this.expandAnimation > 0.5F) {
            float iy = y + 16.0F;

            for(Iterator var10 = this.setting.getModes().iterator(); var10.hasNext(); iy += 12.0F) {
               String m = (String)var10.next();
               if (mx >= (double)x && mx <= (double)(x + w) && my >= (double)iy && my <= (double)(iy + 12.0F) && b == 0) {
                  this.setting.setValue(m);
                  return true;
               }
            }
         }

         return false;
      }
   }

   public float getHeight() {
      return !this.open && this.expandAnimation < 0.01F ? 16.0F : 16.0F + (float)(this.setting.getModes().size() * 12) * this.expandAnimation;
   }

   private boolean isHovered(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   public void updateHover(double mouseX, double mouseY, float x, float y, float width) {
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public void mouseDragged(double mouseX, double mouseY, int button, float x, float y, float width) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, float x, float y, float width) {
      return false;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }
}
