package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.mixin.accessor.ClientPlayerInteractionManagerAccessor;
import foure.dev.mixin.accessor.LivingEntityAccessor;
import foure.dev.mixin.accessor.MinecraftClientAccessor;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;

@ModuleInfo(
   name = "NoDelay",
   category = Category.MOVEMENT,
   desc = "Removes delays"
)
public class NoDelay extends Function {
   public final BooleanSetting BreakCoolDown = new BooleanSetting("BreakCoolDown", false);
   public final BooleanSetting RightClick = new BooleanSetting("RightClick", false);
   public final BooleanSetting Jump = new BooleanSetting("Jump", false);

   public NoDelay() {
      this.addSettings(new Setting[]{this.BreakCoolDown, this.RightClick, this.Jump});
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if ((Boolean)this.BreakCoolDown.getValue() && mc.interactionManager != null) {
         ((ClientPlayerInteractionManagerAccessor)mc.interactionManager).setBlockBreakingCooldown(0);
      }

      if ((Boolean)this.Jump.getValue() && mc.player != null) {
         ((LivingEntityAccessor)mc.player).setJumpingCooldown(0);
      }

      if ((Boolean)this.RightClick.getValue()) {
         ((MinecraftClientAccessor)mc).setItemUseCooldown(0);
      }

   }
}
