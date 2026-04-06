package foure.dev.module.setting.api;

import foure.dev.module.api.Function;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Generated;

public class SettingManager {
   private final List<Setting<?>> settings = new ArrayList();

   public void addSettings(Setting<?>... settings) {
      this.settings.addAll(Arrays.asList(settings));
   }

   public List<Setting<?>> getSettingsForModule(Function module) {
      return (List)this.settings.stream().filter((setting) -> {
         return setting.getParent() == module;
      }).filter(Setting::isVisible).collect(Collectors.toList());
   }

   public void addSetting(Setting<?> setting) {
      this.settings.add(setting);
   }

   @Generated
   public List<Setting<?>> getSettings() {
      return this.settings;
   }
}
