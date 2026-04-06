package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import lombok.Generated;

public class BooleanSetting extends Setting<Boolean> {
   private int key = -1;

   public BooleanSetting(String name, Function parent, boolean value) {
      super(name, parent, value);
   }

   public BooleanSetting(String name, boolean value) {
      super(name, (Function)null, value);
   }

   public void toggle() {
      this.setValue(!(Boolean)this.getValue());
   }

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public void setKey(int key) {
      this.key = key;
   }
}
