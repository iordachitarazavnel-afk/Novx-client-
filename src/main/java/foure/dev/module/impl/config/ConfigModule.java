package foure.dev.module.impl.config;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.StringSetting;

@ModuleInfo(
   name = "ConfigManager",
   category = Category.CONFIG,
   desc = "Manage your configs"
)
public class ConfigModule extends Function {
   private final StringSetting configName = new StringSetting("Config Name", this, "default");
   private final BooleanSetting save = new BooleanSetting("Save", false);
   private final BooleanSetting load = new BooleanSetting("Load", false);

   public ConfigModule() {
      this.addSettings(new Setting[]{this.configName, this.save, this.load});
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if ((Boolean)this.save.getValue()) {
         FourEClient.getInstance().getConfigManager().save((String)this.configName.getValue());
         this.save.setValue(false);
      }

      if ((Boolean)this.load.getValue()) {
         FourEClient.getInstance().getConfigManager().load((String)this.configName.getValue());
         this.load.setValue(false);
      }

   }
}
