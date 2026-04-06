package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;

public class NumberElement implements SettingElement {
   private final NumberSetting setting;
   private final float height = 16.0F;
   private boolean dragging;

   public NumberElement(NumberSetting setting) {
      this.setting = setting;
   }

   public void render(Renderer2D renderer, FontObject font, float x, float y, float width, float alpha) {
      String var10000 = this.setting.getName();
      String display = var10000 + ": " + String.valueOf(((Double)this.setting.getValue()).floatValue()).substring(0, Math.min(String.valueOf(((Double)this.setting.getValue()).floatValue()).length(), 3));
      renderer.text(font, x + 4.0F, y + 7.0F, 8.0F, display, Color.GRAY.getRGB(), "l");
      float sliderHeight = 4.0F;
      float sliderY = y + 10.0F;
      float sliderWidth = width - 8.0F;
      float sliderX = x + 4.0F;
      renderer.rect(sliderX, sliderY, sliderWidth, sliderHeight, 2.0F, (new Color(40, 40, 45)).getRGB());
      double percent = ((Double)this.setting.getValue() - this.setting.getMin()) / (this.setting.getMax() - this.setting.getMin());
      percent = Math.max(0.0D, Math.min(1.0D, percent));
      float fillWidth = (float)((double)sliderWidth * percent);
      renderer.rect(sliderX, sliderY, fillWidth, sliderHeight, 2.0F, (new Color(100, 150, 200)).getRGB());
      renderer.circle(sliderX + fillWidth - 2.0F, sliderY + 2.0F, 3.0F, 2.0F, 2.0F, Color.WHITE.getRGB());
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
      if (this.isHovered(mouseX, mouseY, x, y, width) && button == 0) {
         this.dragging = true;
         this.setValueFromMouse(mouseX, x, width);
         return true;
      } else {
         return false;
      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
      this.dragging = false;
   }

   public void mouseDragged(double mouseX, double mouseY, int button, float x, float y, float width) {
      if (this.dragging) {
         this.setValueFromMouse(mouseX, x, width);
      }

   }

   private void setValueFromMouse(double mouseX, float x, float width) {
      double sliderEffectiveWidth = (double)(width - 8.0F);
      double sliderStartX = (double)(x + 4.0F);
      double percent = (mouseX - sliderStartX) / sliderEffectiveWidth;
      percent = Math.max(0.0D, Math.min(1.0D, percent));
      double value = this.setting.getMin() + (this.setting.getMax() - this.setting.getMin()) * percent;
      this.setting.setValueNumber(value);
   }

   public float getHeight() {
      return 16.0F;
   }

   public void updateHover(double mouseX, double mouseY, float x, float y, float width) {
   }

   public boolean mouseClicked(double mouseX, double mouseY, float x, float y, float width) {
      return false;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + 16.0F);
   }
}
