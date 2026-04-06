package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;

public class StringSetting extends Setting<String> {
   public StringSetting(String name, Function parent, String defaultValue) {
      super(name, parent, defaultValue);
   }

   public StringSetting(String name, String defaultValue) {
      super(name, (Function)null, defaultValue);
   }
}
