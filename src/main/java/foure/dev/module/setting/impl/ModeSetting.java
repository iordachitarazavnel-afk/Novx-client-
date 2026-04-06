package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import java.util.Arrays;
import java.util.List;
import lombok.Generated;

public class ModeSetting extends Setting<String> {
   private final List<String> modes;
   private int index;

   public ModeSetting(String name, Function parent, String current, List<String> modes) {
      super(name, parent, current);
      this.modes = modes;
      this.index = modes.indexOf(current);
   }

   public ModeSetting(String name, Function parent, String current, String... modes) {
      this(name, parent, current, Arrays.asList(modes));
   }

   public ModeSetting(String name, String current, String... modes) {
      super(name, (Function)null, current);
      this.modes = Arrays.asList(modes);
      this.index = this.modes.indexOf(current);
      if (this.index == -1 && !this.modes.isEmpty()) {
         this.index = 0;
         this.setValue((String)this.modes.get(0));
      }

   }

   public boolean is(String modeName) {
      return ((String)this.getValue()).equalsIgnoreCase(modeName);
   }

   public void cycle() {
      if (this.index < this.modes.size() - 1) {
         ++this.index;
      } else {
         this.index = 0;
      }

      this.setValue((String)this.modes.get(this.index));
   }

   public void setValue(String value) {
      if (this.modes.contains(value)) {
         this.index = this.modes.indexOf(value);
         super.setValue(value);
      }

   }

   @Generated
   public List<String> getModes() {
      return this.modes;
   }

   @Generated
   public int getIndex() {
      return this.index;
   }
}
