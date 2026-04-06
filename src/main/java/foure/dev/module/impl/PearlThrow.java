package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BindSetting;
import net.minecraft.item.Items;

@ModuleInfo(
   name = "PearlThrow",
   category = Category.COMBAT,
   desc = "Throws an ender pearl on Activate"
)
public class PearlThrow extends Function {
   private final BindSetting triggerKey = new BindSetting("Trigger Key", -1);
   private boolean isActivated;
   private boolean hasThrown;
   private int previousSlot;
   private int currentSwitchBackDelay;

   public PearlThrow() {
      this.addSettings(new Setting[]{this.triggerKey});
   }

   public void onEnable() {
      super.onEnable();
      this.resetState();
   }

   public void onDisable() {
      super.onDisable();
      if (mc != null && mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.currentScreen == null) {
         if (this.triggerKey.isPressed()) {
            this.isActivated = true;
         }

         if (this.isActivated) {
            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().selectedSlot;
            }

            int pearlSlot = this.findPearlSlot();
            if (pearlSlot != -1) {
               mc.player.getInventory().selectedSlot = pearlSlot;
            }

            if (!this.hasThrown) {
               mc.options.useKey.setPressed(true);
               this.hasThrown = true;
               this.currentSwitchBackDelay = 0;
               return;
            }

            mc.options.useKey.setPressed(false);
            ++this.currentSwitchBackDelay;
            if (this.currentSwitchBackDelay >= 1) {
               this.handleSwitchBack();
            }
         }

      }
   }

   private int findPearlSlot() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
            return i;
         }
      }

      return -1;
   }

   private void handleSwitchBack() {
      if (this.previousSlot != -1) {
         mc.player.getInventory().selectedSlot = this.previousSlot;
      }

      this.resetState();
   }

   private void resetState() {
      this.previousSlot = -1;
      this.currentSwitchBackDelay = 0;
      this.isActivated = false;
      this.hasThrown = false;
      if (mc != null && mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

   }
}
