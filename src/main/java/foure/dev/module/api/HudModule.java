package foure.dev.module.api;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;

public abstract class HudModule extends Function {
   protected final NumberSetting xSetting = new NumberSetting("X", this, 10.0D, 0.0D, 2000.0D, 1.0D);
   protected final NumberSetting ySetting = new NumberSetting("Y", this, 10.0D, 0.0D, 1500.0D, 1.0D);
   protected float width = 100.0F;
   protected float height = 50.0F;

   public HudModule() {
      this.addSettings(new Setting[]{this.xSetting, this.ySetting});
   }

   public float getX() {
      return this.xSetting.getValueFloat();
   }

   public void setX(float x) {
      this.xSetting.setValueNumber((double)x);
   }

   public float getY() {
      return this.ySetting.getValueFloat();
   }

   public void setY(float y) {
      this.ySetting.setValueNumber((double)y);
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float w) {
      this.width = w;
   }

   public float getHeight() {
      return this.height;
   }

   public void setHeight(float h) {
      this.height = h;
   }

   public boolean isRightSide() {
      return this.getX() > (float)mc.getWindow().getScaledWidth() / 2.0F;
   }
}
