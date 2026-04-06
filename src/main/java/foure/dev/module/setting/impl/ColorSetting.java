package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import java.awt.Color;

public class ColorSetting extends Setting<Color> {
   private float[] hsb = new float[3];

   public ColorSetting(String name, Function parent, Color defaultValue) {
      super(name, parent, defaultValue);
      Color.RGBtoHSB(defaultValue.getRed(), defaultValue.getGreen(), defaultValue.getBlue(), this.hsb);
   }

   public void setColor(Color c) {
      this.setValue(c);
      Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), this.hsb);
   }

   public float[] getHsb() {
      return this.hsb;
   }

   public void setHue(float hue) {
      this.hsb[0] = hue;
      this.updateColor();
   }

   public void setSaturation(float sat) {
      this.hsb[1] = sat;
      this.updateColor();
   }

   public void setBrightness(float bri) {
      this.hsb[2] = bri;
      this.updateColor();
   }

   private void updateColor() {
      int alpha = ((Color)this.getValue()).getAlpha();
      Color c = Color.getHSBColor(this.hsb[0], this.hsb[1], this.hsb[2]);
      this.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
   }

   public void setValue(Color value) {
      super.setValue(value);
      float[] newHsb = Color.RGBtoHSB(value.getRed(), value.getGreen(), value.getBlue(), (float[])null);
      this.hsb[1] = newHsb[1];
      this.hsb[2] = newHsb[2];
      if (newHsb[1] > 0.01F) {
         this.hsb[0] = newHsb[0];
      }

   }
}
