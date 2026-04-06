package foure.dev.module.setting.api;

import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;

public interface SettingElement {
   void render(Renderer2D var1, FontObject var2, float var3, float var4, float var5, float var6);

   float getHeight();

   boolean mouseClicked(double var1, double var3, float var5, float var6, float var7);

   default boolean mouseClicked(double mouseX, double mouseY, int button, float x, float y, float width) {
      return this.mouseClicked(mouseX, mouseY, x, y, width);
   }

   void updateHover(double var1, double var3, float var5, float var6, float var7);

   void mouseReleased(double var1, double var3, int var5);

   void mouseDragged(double var1, double var3, int var5, float var6, float var7, float var8);

   Setting<?> getSetting();
}
