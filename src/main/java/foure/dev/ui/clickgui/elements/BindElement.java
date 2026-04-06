package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;

public class BindElement implements SettingElement {
   private final BindSetting setting;
   private final float height = 14.0F;

   public BindElement(BindSetting setting) {
      this.setting = setting;
   }

   public void render(Renderer2D renderer, FontObject font, float x, float y, float width, float alpha) {
      renderer.text(font, x + 4.0F, y + 7.0F + 2.0F, 8.0F, this.setting.getName(), Color.WHITE.getRGB(), "l");
      String keyText = this.setting.getKeyName();
      float buttonWidth = 40.0F;
      float buttonX = x + width - buttonWidth - 4.0F;
      float buttonY = y + 2.0F;
      float buttonHeight = 10.0F;
      Color buttonColor = this.setting.isListening() ? new Color(100, 150, 200) : new Color(60, 60, 60);
      renderer.rect(buttonX, buttonY, buttonWidth, buttonHeight, 2.0F, buttonColor.getRGB());
      renderer.text(font, buttonX + buttonWidth / 2.0F, buttonY + buttonHeight / 2.0F + 2.0F, 7.0F, this.setting.isListening() ? "..." : keyText, Color.WHITE.getRGB(), "c");
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
      if (this.isHovered(mouseX, mouseY, x, y, width) && button == 0) {
         this.setting.setListening(!this.setting.isListening());
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseClicked(double mouseX, double mouseY, float x, float y, float width) {
      return false;
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

   public Setting<?> getSetting() {
      return this.setting;
   }

   private boolean isHovered(double mouseX, double mouseY, float x, float y, float width) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + 14.0F);
   }
}
