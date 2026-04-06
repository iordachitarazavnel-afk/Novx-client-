package foure.dev.module.impl.combat;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;

@ModuleInfo(
   name = "sss",
   category = Category.COMBAT,
   desc = "ssss"
)
public class sss extends Function {
   private final BooleanSetting rotate = new BooleanSetting("Rotate", this, true);
   private final NumberSetting range = new NumberSetting("Range", (Function)null, 3.0D, 1.0D, 6.0D, 0.1D);
   private final ModeSetting mode = new ModeSetting("Mode", this, "Switch", new String[]{"Switch", "Single", "Multi"});

   public sss() {
      this.addSettings(new Setting[]{this.rotate, this.range, this.mode});
   }
}
