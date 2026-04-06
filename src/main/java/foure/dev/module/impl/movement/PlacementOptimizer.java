package foure.dev.module.impl.movement;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;

@ModuleInfo(
   name = "PlacementOptimizer",
   category = Category.MOVEMENT,
   desc = "Adjusts block/crystal placement delays"
)
public class PlacementOptimizer extends Function {
   private final BooleanSetting excludeAnchors = new BooleanSetting("Exclude Anchors/Glowstone", true);
   private final NumberSetting blockDelay = new NumberSetting("Block delay", this, 3.0D, 0.0D, 5.0D, 0.10000000149011612D);
   private final NumberSetting crystalDelay = new NumberSetting("Crystal delay", this, 0.0D, 0.0D, 2.0D, 1.0D);

   public PlacementOptimizer() {
      this.addSettings(new Setting[]{this.excludeAnchors, this.blockDelay, this.crystalDelay});
   }

   public boolean shouldExcludeAnchors() {
      return (Boolean)this.excludeAnchors.getValue();
   }

   public int getBlockDelay() {
      return (int)this.blockDelay.getValueFloat();
   }

   public int getCrystalDelay() {
      return (int)this.crystalDelay.getValueFloat();
   }
}
