package foure.dev.module.impl.visual;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

@ModuleInfo(
   name = "FullBright",
   category = Category.RENDER,
   desc = "Maximizes brightness."
)
public class FullBright extends Function {
   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 500, 0, false, false));
      }

   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }

   }
}
