package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import lombok.Generated;

public class NumberSetting extends Setting<Double> {
   private final double min;
   private final double max;
   private final double increment;

   public NumberSetting(String name, Function parent, double defaultValue, double min, double max, double increment) {
      super(name, parent, defaultValue);
      this.min = min;
      this.max = max;
      this.increment = increment;
   }

   public int getValueInt() {
      return ((Double)this.getValue()).intValue();
   }

   public float getValueFloat() {
      return ((Double)this.getValue()).floatValue();
   }

   public long getValueLong() {
      return ((Double)this.getValue()).longValue();
   }

   public void setValueNumber(double value) {
      double precision = 1.0D / this.increment;
      this.setValue((double)Math.round(Math.max(this.min, Math.min(this.max, value)) * precision) / precision);
   }

   @Generated
   public double getMin() {
      return this.min;
   }

   @Generated
   public double getMax() {
      return this.max;
   }

   @Generated
   public double getIncrement() {
      return this.increment;
   }
}
