package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class MultiBoxSetting extends Setting<Void> {
   private final List<BooleanSetting> settings;

   public MultiBoxSetting(String name, BooleanSetting... settings) {
      super(name, (Function)null, (Void) null);
      this.settings = Arrays.asList(settings);
   }

   public List<BooleanSetting> getSettings() {
      return this.settings;
   }

   public List<BooleanSetting> getSelectedOptions() {
      return (List)this.settings.stream().filter(Setting::getValue).collect(Collectors.toList());
   }

   public List<BooleanSetting> getOptions() {
      return this.settings;
   }

   public boolean is(String name) {
      Iterator var2 = this.settings.iterator();

      BooleanSetting s;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         s = (BooleanSetting)var2.next();
      } while(!s.getName().equalsIgnoreCase(name));

      return (Boolean)s.getValue();
   }

   public BooleanSetting getName(String name) {
      Iterator var2 = this.settings.iterator();

      BooleanSetting s;
      do {
         if (!var2.hasNext()) {
            return null;
         }

         s = (BooleanSetting)var2.next();
      } while(!s.getName().equalsIgnoreCase(name));

      return s;
   }
}
