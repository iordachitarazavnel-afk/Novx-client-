package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.util.render.utils.ChatUtils;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(
   name = "CrateBuyer",
   category = Category.DONUT,
   desc = "Automatically buys items from the common crate"
)
public class CrateBuyer extends Function {
   private static final int HELMET_SLOT = 10;
   private static final int CHESTPLATE_SLOT = 11;
   private static final int LEGGINGS_SLOT = 12;
   private static final int BOOTS_SLOT = 13;
   private static final int SWORD_SLOT = 14;
   private static final int PICKAXE_SLOT = 15;
   private static final int SHOVEL_SLOT = 16;
   private static final int CONFIRM_SLOT_DEFAULT = 15;
   private final ModeSetting action = new ModeSetting("Action", this, "All", new String[]{"All", "Helmet", "Chestplate", "Leggings", "Boots", "Sword", "Pickaxe", "Shovel"});
   private final int delay = 4;
   private int tickCounter = 0;
   private int warningCooldown = 0;
   private int currentStep = 0;
   private int currentItemIndex = 0;
   private boolean hasClickedOnce = false;
   private final String[] ITEM_TYPES = new String[]{"Helmet", "Chestplate", "Leggings", "Boots", "Sword", "Pickaxe", "Shovel"};

   public void onEnable() {
      this.tickCounter = 0;
      this.warningCooldown = 0;
      this.currentStep = 0;
      this.currentItemIndex = 0;
      this.hasClickedOnce = false;
      ChatUtils.sendMessage("Activated. Mode: " + (String)this.action.getValue());
   }

   public void onDisable() {
      this.currentStep = 0;
      this.currentItemIndex = 0;
      this.hasClickedOnce = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.warningCooldown > 0) {
            --this.warningCooldown;
         }

         Screen var3 = mc.currentScreen;
         if (var3 instanceof HandledScreen) {
            HandledScreen<?> screen = (HandledScreen)var3;
            if (!this.hasClickedOnce && !this.isValidCrateScreen(screen)) {
               if (this.warningCooldown == 0) {
                  ChatUtils.sendMessage("This doesn't appear to be a valid crate screen. Closing screen.");
                  this.warningCooldown = 20;
                  mc.setScreen((Screen)null);
               }

            } else {
               ++this.tickCounter;
               if (this.tickCounter >= 4) {
                  this.tickCounter = 0;
                  if (this.action.is("All")) {
                     this.handleAllItems(screen);
                  } else {
                     this.handleSingleItem(screen);
                  }

               }
            }
         } else {
            if (this.isToggled() && this.warningCooldown == 0) {
               ChatUtils.sendMessage("You need to be on the crate screen to use this module.");
               this.warningCooldown = 20;
            }

         }
      }
   }

   private boolean isValidCrateScreen(HandledScreen<?> screen) {
      int i;
      for(i = 0; i <= 9; ++i) {
         if (screen.getScreenHandler().getSlot(i).hasStack() && !screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
            return false;
         }
      }

      for(i = 17; i <= 26; ++i) {
         if (screen.getScreenHandler().getSlot(i).hasStack() && !screen.getScreenHandler().getSlot(i).getStack().isOf(Items.GRAY_STAINED_GLASS_PANE)) {
            return false;
         }
      }

      return true;
   }

   private void handleAllItems(HandledScreen<?> screen) {
      if (this.currentStep == 0) {
         int itemSlot = this.getItemSlot(this.ITEM_TYPES[this.currentItemIndex]);
         mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
         this.hasClickedOnce = true;
         this.currentStep = 1;
      } else {
         int confirmSlot = 15;
         mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, confirmSlot, 0, SlotActionType.PICKUP, mc.player);
         this.currentStep = 0;
         ++this.currentItemIndex;
         if (this.currentItemIndex >= this.ITEM_TYPES.length) {
            this.currentItemIndex = 0;
         }
      }

   }

   private void handleSingleItem(HandledScreen<?> screen) {
      if (this.currentStep == 0) {
         int itemSlot = this.getItemSlot((String)this.action.getValue());
         mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, itemSlot, 0, SlotActionType.PICKUP, mc.player);
         this.hasClickedOnce = true;
         this.currentStep = 1;
      } else {
         int confirmSlot = 15;
         mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, confirmSlot, 0, SlotActionType.PICKUP, mc.player);
         this.currentStep = 0;
      }

   }

   private int getItemSlot(String itemType) {
      byte var3 = -1;
      switch(itemType.hashCode()) {
      case -2137067379:
         if (itemType.equals("Helmet")) {
            var3 = 0;
         }
         break;
      case -1819278141:
         if (itemType.equals("Shovel")) {
            var3 = 6;
         }
         break;
      case -1231550251:
         if (itemType.equals("Chestplate")) {
            var3 = 1;
         }
         break;
      case 64369569:
         if (itemType.equals("Boots")) {
            var3 = 3;
         }
         break;
      case 80307677:
         if (itemType.equals("Sword")) {
            var3 = 4;
         }
         break;
      case 1086624557:
         if (itemType.equals("Pickaxe")) {
            var3 = 5;
         }
         break;
      case 1800320138:
         if (itemType.equals("Leggings")) {
            var3 = 2;
         }
      }

      switch(var3) {
      case 0:
         return 10;
      case 1:
         return 11;
      case 2:
         return 12;
      case 3:
         return 13;
      case 4:
         return 14;
      case 5:
         return 15;
      case 6:
         return 16;
      default:
         return 10;
      }
   }
}
