package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

@ModuleInfo(
   name = "ShopBuyer",
   category = Category.DONUT,
   desc = "Automatically buys selected items from PVP shop category"
)
public class ShopBuyer extends Function {
   private final ModeSetting itemToBuy = new ModeSetting("Item", this, "Obsidian", new String[]{"Obsidian", "End Crystal", "Respawn Anchor", "Glowstone", "Totem of Undying", "Ender Pearl", "Golden Apple", "Experience Bottle", "Slow Falling Arrow"});
   private final BooleanSetting autoDrop = new BooleanSetting("Auto Drop", this, true);
   private final int delay = 1;
   private int delayCounter = 0;
   private boolean inPvpCategory = false;
   private boolean inBuyingScreen = false;

   public void onEnable() {
      this.delayCounter = 0;
      this.inPvpCategory = false;
      this.inBuyingScreen = false;
      super.onEnable();
   }

   public void onDisable() {
      this.delayCounter = 0;
      this.inPvpCategory = false;
      this.inBuyingScreen = false;
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null) {
         if (this.delayCounter > 0) {
            --this.delayCounter;
         } else {
            ScreenHandler currentScreenHandler = mc.player.currentScreenHandler;
            if (currentScreenHandler instanceof GenericContainerScreenHandler) {
               GenericContainerScreenHandler containerHandler = (GenericContainerScreenHandler)currentScreenHandler;
               int rows = containerHandler.getRows();
               this.updateScreenState(currentScreenHandler, rows);
               if (rows == 3) {
                  if (this.isBuyingScreen(currentScreenHandler)) {
                     this.handleBuyingScreen(currentScreenHandler);
                     return;
                  }

                  if (this.isPvpCategoryScreen(currentScreenHandler)) {
                     this.handlePvpCategory(currentScreenHandler);
                     return;
                  }

                  if (this.isMainShopScreen(currentScreenHandler)) {
                     this.handleMainShop(currentScreenHandler);
                     return;
                  }
               }

               this.resetState();
            } else {
               mc.getNetworkHandler().sendChatCommand("shop");
               this.delayCounter = 1;
               this.resetState();
            }
         }
      }
   }

   private void updateScreenState(ScreenHandler handler, int rows) {
      if (this.isBuyingScreen(handler)) {
         this.inBuyingScreen = true;
      } else if (this.isPvpCategoryScreen(handler)) {
         this.inPvpCategory = true;
         this.inBuyingScreen = false;
      } else if (this.isMainShopScreen(handler)) {
         this.inPvpCategory = false;
         this.inBuyingScreen = false;
      }

   }

   private boolean isMainShopScreen(ScreenHandler handler) {
      return handler.getSlot(13).getStack().isOf(Items.TOTEM_OF_UNDYING) && !this.isBuyingScreen(handler);
   }

   private boolean isPvpCategoryScreen(ScreenHandler handler) {
      return handler.getSlot(9).getStack().isOf(Items.OBSIDIAN) || handler.getSlot(10).getStack().isOf(Items.END_CRYSTAL) || handler.getSlot(11).getStack().isOf(Items.RESPAWN_ANCHOR) || handler.getSlot(12).getStack().isOf(Items.GLOWSTONE);
   }

   private boolean isBuyingScreen(ScreenHandler handler) {
      for(int i = 0; i < handler.slots.size(); ++i) {
         if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
            return true;
         }
      }

      return false;
   }

   private void handleMainShop(ScreenHandler handler) {
      mc.interactionManager.clickSlot(handler.syncId, 13, 0, SlotActionType.PICKUP, mc.player);
      this.delayCounter = 1;
      this.inPvpCategory = true;
   }

   private void handlePvpCategory(ScreenHandler handler) {
      String selectedItem = (String)this.itemToBuy.getValue();
      int slot = this.getItemSlot(selectedItem);
      if (slot != -1 && this.isCorrectItemInSlot(handler, slot, selectedItem)) {
         this.clickItem(handler, slot);
      }

   }

   private int getItemSlot(String itemType) {
      byte var3 = -1;
      switch(itemType.hashCode()) {
      case -1990159055:
         if (itemType.equals("Slow Falling Arrow")) {
            var3 = 8;
         }
         break;
      case -1672657533:
         if (itemType.equals("Golden Apple")) {
            var3 = 6;
         }
         break;
      case -1668482902:
         if (itemType.equals("Totem of Undying")) {
            var3 = 4;
         }
         break;
      case -1581458255:
         if (itemType.equals("End Crystal")) {
            var3 = 1;
         }
         break;
      case -1284959528:
         if (itemType.equals("Glowstone")) {
            var3 = 3;
         }
         break;
      case -914551634:
         if (itemType.equals("Ender Pearl")) {
            var3 = 5;
         }
         break;
      case 416515707:
         if (itemType.equals("Obsidian")) {
            var3 = 0;
         }
         break;
      case 1005287661:
         if (itemType.equals("Respawn Anchor")) {
            var3 = 2;
         }
         break;
      case 1622034748:
         if (itemType.equals("Experience Bottle")) {
            var3 = 7;
         }
      }

      byte var10000;
      switch(var3) {
      case 0:
         var10000 = 9;
         break;
      case 1:
         var10000 = 10;
         break;
      case 2:
         var10000 = 11;
         break;
      case 3:
         var10000 = 12;
         break;
      case 4:
         var10000 = 13;
         break;
      case 5:
         var10000 = 14;
         break;
      case 6:
         var10000 = 15;
         break;
      case 7:
         var10000 = 16;
         break;
      case 8:
         var10000 = 17;
         break;
      default:
         var10000 = -1;
      }

      return var10000;
   }

   private boolean isCorrectItemInSlot(ScreenHandler handler, int slot, String itemType) {
      byte var5 = -1;
      switch(itemType.hashCode()) {
      case -1990159055:
         if (itemType.equals("Slow Falling Arrow")) {
            var5 = 8;
         }
         break;
      case -1672657533:
         if (itemType.equals("Golden Apple")) {
            var5 = 6;
         }
         break;
      case -1668482902:
         if (itemType.equals("Totem of Undying")) {
            var5 = 4;
         }
         break;
      case -1581458255:
         if (itemType.equals("End Crystal")) {
            var5 = 1;
         }
         break;
      case -1284959528:
         if (itemType.equals("Glowstone")) {
            var5 = 3;
         }
         break;
      case -914551634:
         if (itemType.equals("Ender Pearl")) {
            var5 = 5;
         }
         break;
      case 416515707:
         if (itemType.equals("Obsidian")) {
            var5 = 0;
         }
         break;
      case 1005287661:
         if (itemType.equals("Respawn Anchor")) {
            var5 = 2;
         }
         break;
      case 1622034748:
         if (itemType.equals("Experience Bottle")) {
            var5 = 7;
         }
      }

      boolean var10000;
      switch(var5) {
      case 0:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.OBSIDIAN);
         break;
      case 1:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.END_CRYSTAL);
         break;
      case 2:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.RESPAWN_ANCHOR);
         break;
      case 3:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.GLOWSTONE);
         break;
      case 4:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.TOTEM_OF_UNDYING);
         break;
      case 5:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.ENDER_PEARL);
         break;
      case 6:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.GOLDEN_APPLE);
         break;
      case 7:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.EXPERIENCE_BOTTLE);
         break;
      case 8:
         var10000 = handler.getSlot(slot).getStack().isOf(Items.TIPPED_ARROW);
         break;
      default:
         var10000 = false;
      }

      return var10000;
   }

   private void handleBuyingScreen(ScreenHandler handler) {
      int i;
      for(i = 0; i < handler.slots.size(); ++i) {
         if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE) && handler.getSlot(i).getStack().getCount() == 64) {
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            this.delayCounter = 1;
            return;
         }
      }

      for(i = 0; i < handler.slots.size(); ++i) {
         if (handler.getSlot(i).getStack().isOf(Items.LIME_STAINED_GLASS_PANE)) {
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            this.delayCounter = 1;
            if ((Boolean)this.autoDrop.getValue()) {
               mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(Action.DROP_ALL_ITEMS, BlockPos.ORIGIN, Direction.DOWN));
            }

            this.resetState();
            return;
         }
      }

   }

   private void clickItem(ScreenHandler handler, int slot) {
      mc.interactionManager.clickSlot(handler.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
      this.delayCounter = 1;
      this.inBuyingScreen = true;
   }

   private void resetState() {
      this.inPvpCategory = false;
      this.inBuyingScreen = false;
   }
}
