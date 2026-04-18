package foure.dev.ui.clickgui.elements;

import foure.dev.FourEClient;
import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.ui.clickgui.NovxClickGui;
import foure.dev.util.render.animation.AnimationSystem;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;

public class BooleanElement implements SettingElement {
   private final BooleanSetting setting;
   private final float height = 14.0F;
   private float progress;
   private BooleanElement.ToggleAnimation animation;

   public BooleanElement(BooleanSetting setting) {
      this.setting = setting;
      this.progress = (Boolean)setting.getValue() ? 1.0F : 0.0F;
   }

   public void render(Renderer2D renderer, FontObject font, float x, float y, float width, float alpha) {
      if (this.animation == null) {
         this.progress = (Boolean)this.setting.getValue() ? 1.0F : 0.0F;
      }

      renderer.text(font, x + 4.0F, y + 7.0F + 2.0F, 8.0F, this.setting.getName(), Color.WHITE.getRGB(), "l");
      float switchWidth = 16.0F;
      float switchHeight = 8.0F;
      float sx = x + width - switchWidth - 4.0F;
      float sy = y + (14.0F - switchHeight) / 2.0F;
      Color offColor = new Color(60, 60, 60);
      Color onColor = new Color(100, 150, 200);
      Color bg = lerp(offColor, onColor, this.progress);
      renderer.rect(sx, sy, switchWidth, switchHeight, 4.0F, bg.getRGB());
      float knobX = sx + 1.0F + (switchWidth - switchHeight) * this.progress;
      float knobY = sy + 1.0F;
      renderer.circle(knobX + 3.0F, knobY + 3.0F, 3.0F, 1.0F, 1.0F, Color.WHITE.getRGB());
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
      if (this.isHovered(mouseX, mouseY, x, y, width)) {
         if (button == 0) {
            this.setting.toggle();
            this.startAnimation((Boolean)this.setting.getValue());
            return true;
         }

         if (button == 1) {
            Function clickGui = FourEClient.getInstance().getFunctionManager().getModule(NovxClickGui.class);
            if (clickGui instanceof NovxClickGui) {
               NovxClickGui dashboard = (NovxClickGui)clickGui;
               dashboard.getBindPopup().open(this.setting, (float)mouseX - 50.0F, (float)mouseY - 10.0F);
            }

            return true;
         }
      }

      return false;
   }

   private void startAnimation(boolean enabled) {
      float target = enabled ? 1.0F : 0.0F;
      if (this.animation != null) {
         AnimationSystem.getInstance().unregister(this.animation);
      }

      this.animation = new BooleanElement.ToggleAnimation(this.progress, target);
      AnimationSystem.getInstance().ensureRegistered(this.animation);
   }

   public void updateHover(double mouseX, double mouseY, float x, float y, float width) {
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
   }

   public void mouseDragged(double mouseX, double mouseY, int button, float x, float y, float width) {
   }

   public float getHeight() {
      return 14.0F;
   }

   public boolean mouseClicked(double mouseX, double mouseY, float x, float y, float width) {
      return false;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + 14.0F);
   }

   private static Color lerp(Color a, Color b, float t) {
      t = Math.max(0.0F, Math.min(1.0F, t));
      int r = (int)((float)a.getRed() + (float)(b.getRed() - a.getRed()) * t);
      int g = (int)((float)a.getGreen() + (float)(b.getGreen() - a.getGreen()) * t);
      int bl = (int)((float)a.getBlue() + (float)(b.getBlue() - a.getBlue()) * t);
      return new Color(r, g, bl);
   }

   private class ToggleAnimation implements AnimationSystem.Animated {
      private float value;
      private final float start;
      private final float target;
      private float time;
      private static final float DURATION = 0.18F;

      ToggleAnimation(float start, float target) {
         this.start = start;
         this.target = target;
         this.value = start;
         this.time = 0.0F;
      }

      public boolean update(float deltaSeconds) {
         this.time += deltaSeconds;
         float t = Math.min(this.time / 0.18F, 1.0F);
         float eased = Easings.EASE_OUT_CUBIC.ease(t);
         this.value = this.start + (this.target - this.start) * eased;
         BooleanElement.this.progress = this.value;
         if (t >= 1.0F) {
            BooleanElement.this.animation = null;
            return false;
         } else {
            return true;
         }
      }
   }
}
