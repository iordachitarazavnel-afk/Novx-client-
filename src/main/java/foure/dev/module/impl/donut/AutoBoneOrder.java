package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventPlayerTick;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.ItemSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(
   name = "AutoBoneOrder",
   desc = "Automates ordering bones",
   category = Category.DONUT
)
public class AutoBoneOrder extends Function {
   private final StringSetting orderName = new StringSetting("Order Name", "bones");
   private final ItemSetting orderItem;
   private final NumberSetting clickDelay;
   private final NumberSetting guiTimeout;
   private AutoBoneOrder.State state;
   private BlockPos spawnerPos;
   private int waitTicks;
   private int timeoutTicks;
   private int doubleEscapeRemaining;
   private boolean hasClickedConfirm;
   private float yaw;
   private float pitch;
   private boolean hasPressed;

   public AutoBoneOrder() {
      this.orderItem = new ItemSetting("Order Item", Items.BONE);
      this.clickDelay = new NumberSetting("Click Delay (ticks)", this, 2.0D, 1.0D, 10.0D, 1.0D);
      this.guiTimeout = new NumberSetting("GUI Timeout (ticks)", this, 60.0D, 20.0D, 200.0D, 5.0D);
      this.state = AutoBoneOrder.State.IDLE;
      this.waitTicks = 0;
      this.timeoutTicks = 0;
      this.doubleEscapeRemaining = 0;
      this.hasClickedConfirm = false;
      this.addSettings(new Setting[]{this.orderName, this.orderItem, this.clickDelay, this.guiTimeout});
   }

   public void onEnable() {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         this.resetState();
      } else {
         this.toggle();
      }
   }

   public void onDisable() {
      this.state = AutoBoneOrder.State.IDLE;
      this.spawnerPos = null;
      this.waitTicks = 0;
      this.timeoutTicks = 0;
      this.doubleEscapeRemaining = 0;
      this.hasClickedConfirm = false;
   }

   @Subscribe
   public void onTick(EventPlayerTick event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (mc.currentScreen instanceof GameMenuScreen) {
            mc.player.closeHandledScreen();
            this.resetState();
         } else {
            mc.player.setYaw(this.yaw);
            mc.player.setPitch(this.pitch);
            if (this.waitTicks > 0) {
               --this.waitTicks;
            } else {
               if (this.hasPressed) {
                  this.hasPressed = false;
                  mc.options.useKey.setPressed(false);
               }

               switch(this.state.ordinal()) {
               case 0:
               default:
                  break;
               case 1:
                  this.findSpawner();
                  break;
               case 2:
                  this.openSpawner();
                  break;
               case 3:
                  this.waitSpawnerGui();
                  break;
               case 4:
                  this.lootBones();
                  break;
               case 5:
                  this.closeSpawner();
                  break;
               case 6:
                  this.sendOrderCommand();
                  break;
               case 7:
                  this.waitOrderGui();
                  break;
               case 8:
                  this.selectOrderItem();
                  break;
               case 9:
                  this.waitDeliveryGui();
                  break;
               case 10:
                  this.deliverBones();
                  break;
               case 11:
                  this.waitAfterDelivery1();
                  break;
               case 12:
                  this.closeDelivery();
                  break;
               case 13:
                  this.waitAfterCloseDelivery();
                  break;
               case 14:
                  this.waitConfirmGui();
                  break;
               case 15:
                  this.waitConfirmSettle();
                  break;
               case 16:
                  this.clickConfirmSlot();
                  break;
               case 17:
                  this.waitAfterConfirm1();
                  break;
               case 18:
                  this.waitAfterConfirm2();
                  break;
               case 19:
                  this.waitAfterConfirm3();
                  break;
               case 20:
                  this.performDoubleEscape();
                  break;
               case 21:
                  this.doubleRightClickFirst();
                  break;
               case 22:
                  this.doubleRightClickSecond();
                  break;
               case 23:
                  this.postCycleDelay();
               }

            }
         }
      } else {
         this.toggle();
      }
   }

   private void resetState() {
      this.state = AutoBoneOrder.State.FINDING_SPAWNER;
      this.spawnerPos = null;
      this.waitTicks = 0;
      this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
      this.doubleEscapeRemaining = 0;
      this.hasClickedConfirm = false;
      if (mc.player != null) {
         this.yaw = mc.player.getYaw();
         this.pitch = mc.player.getPitch();
      }

      this.hasPressed = false;
   }

   private void findSpawner() {
      if (this.setSpawnerFromCrosshairOrKeep()) {
         this.state = AutoBoneOrder.State.OPENING_SPAWNER;
         this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
      } else {
         this.toggle();
      }

   }

   private void openSpawner() {
      if (this.spawnerPos == null) {
         this.state = AutoBoneOrder.State.FINDING_SPAWNER;
      } else {
         mc.options.useKey.setPressed(true);
         this.hasPressed = true;
         this.state = AutoBoneOrder.State.WAITING_SPAWNER_GUI;
         this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
         this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
      }
   }

   private void waitSpawnerGui() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.state = AutoBoneOrder.State.LOOTING_BONES;
         this.waitTicks = 0;
      } else {
         --this.timeoutTicks;
         if (this.timeoutTicks <= 0) {
            this.toggle();
         }

      }
   }

   private void lootBones() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else if (this.isInventoryFull()) {
         this.state = AutoBoneOrder.State.CLOSING_SPAWNER;
         this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
      } else {
         int movedCount = 0;

         for(int slot = 0; slot < mc.player.currentScreenHandler.slots.size(); ++slot) {
            if (slot < 36) {
               ItemStack stack = mc.player.currentScreenHandler.getSlot(slot).getStack();
               if (!stack.isEmpty() && stack.isOf(Items.BONE)) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 0, SlotActionType.QUICK_MOVE, mc.player);
                  ++movedCount;
                  if (movedCount >= 3) {
                     this.waitTicks = 1;
                     return;
                  }
               }
            }
         }

         if (movedCount == 0) {
            this.state = AutoBoneOrder.State.CLOSING_SPAWNER;
            this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
         } else {
            this.waitTicks = 1;
         }

      }
   }

   private void closeSpawner() {
      mc.player.closeHandledScreen();
      this.state = AutoBoneOrder.State.ORDER_COMMAND;
      this.waitTicks = ((Double)this.clickDelay.getValue()).intValue() * 2;
   }

   private void sendOrderCommand() {
      mc.getNetworkHandler().sendChatCommand("order " + (String)this.orderName.getValue());
      this.state = AutoBoneOrder.State.WAIT_ORDER_GUI;
      this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
      this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
   }

   private void waitOrderGui() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.state = AutoBoneOrder.State.SELECT_ORDER_ITEM;
         this.waitTicks = ((Double)this.clickDelay.getValue()).intValue();
      } else {
         --this.timeoutTicks;
         if (this.timeoutTicks <= 0) {
            this.toggle();
         }

      }
   }

   private void selectOrderItem() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         boolean clicked = false;

         for(int i = 0; i < mc.player.currentScreenHandler.slots.size(); ++i) {
            ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.isOf(this.orderItem.getItem())) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
               clicked = true;
               this.waitTicks = ((Double)this.clickDelay.getValue()).intValue() * 2;
               break;
            }
         }

         if (clicked) {
            this.state = AutoBoneOrder.State.WAIT_DELIVERY_GUI;
            this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
         } else {
            this.toggle();
         }

      }
   }

   private void waitDeliveryGui() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         boolean hasPlayerInventorySlots = false;

         for(int i = 0; i < mc.player.currentScreenHandler.slots.size(); ++i) {
            if (mc.player.currentScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
               hasPlayerInventorySlots = true;
               break;
            }
         }

         if (hasPlayerInventorySlots) {
            this.state = AutoBoneOrder.State.DELIVERING_BONES;
            this.waitTicks = 0;
            return;
         }
      }

      --this.timeoutTicks;
      if (this.timeoutTicks <= 0) {
         this.toggle();
      }

   }

   private void deliverBones() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         int movedCount = 0;

         for(int i = 0; i < mc.player.currentScreenHandler.slots.size(); ++i) {
            if (mc.player.currentScreenHandler.getSlot(i).inventory == mc.player.getInventory()) {
               ItemStack stack = mc.player.currentScreenHandler.getSlot(i).getStack();
               if (!stack.isEmpty() && stack.isOf(Items.BONE)) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
                  ++movedCount;
                  if (movedCount >= 3) {
                     this.waitTicks = 1;
                     return;
                  }
               }
            }
         }

         if (movedCount == 0) {
            this.state = AutoBoneOrder.State.WAIT_AFTER_DELIVERY_1;
            this.waitTicks = 5;
         } else {
            this.waitTicks = 1;
         }

      }
   }

   private void waitAfterDelivery1() {
      this.state = AutoBoneOrder.State.CLOSING_DELIVERY;
      this.waitTicks = 5;
   }

   private void closeDelivery() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      this.state = AutoBoneOrder.State.WAIT_AFTER_CLOSE_DELIVERY;
      this.waitTicks = 5;
      this.hasClickedConfirm = false;
   }

   private void waitAfterCloseDelivery() {
      this.state = AutoBoneOrder.State.WAIT_CONFIRM_GUI;
      this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
      this.waitTicks = 5;
   }

   private void waitConfirmGui() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         this.state = AutoBoneOrder.State.WAIT_CONFIRM_SETTLE;
         this.waitTicks = 5;
      } else {
         --this.timeoutTicks;
         if (this.timeoutTicks <= 0) {
            this.toggle();
         }

      }
   }

   private void waitConfirmSettle() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.state = AutoBoneOrder.State.WAIT_CONFIRM_GUI;
      } else {
         if (mc.player.currentScreenHandler.slots.size() > 15) {
            ItemStack slot15 = mc.player.currentScreenHandler.getSlot(15).getStack();
            if (slot15.isOf(Items.LIME_STAINED_GLASS_PANE)) {
               this.state = AutoBoneOrder.State.CLICK_CONFIRM_SLOT;
               this.waitTicks = 5;
            } else {
               this.waitTicks = 5;
            }
         }

      }
   }

   private void clickConfirmSlot() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         if (!this.hasClickedConfirm && mc.player.currentScreenHandler.slots.size() > 15) {
            ItemStack slot15 = mc.player.currentScreenHandler.getSlot(15).getStack();
            if (slot15.isOf(Items.LIME_STAINED_GLASS_PANE)) {
               mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
               this.hasClickedConfirm = true;
            }
         }

         this.state = AutoBoneOrder.State.WAIT_AFTER_CONFIRM_1;
         this.waitTicks = 5;
      }
   }

   private void waitAfterConfirm1() {
      this.state = AutoBoneOrder.State.WAIT_AFTER_CONFIRM_2;
      this.waitTicks = 5;
   }

   private void waitAfterConfirm2() {
      this.state = AutoBoneOrder.State.WAIT_AFTER_CONFIRM_3;
      this.waitTicks = 5;
   }

   private void waitAfterConfirm3() {
      this.state = AutoBoneOrder.State.DOUBLE_ESCAPE;
      this.doubleEscapeRemaining = 2;
      this.waitTicks = 5;
   }

   private boolean isInventoryFull() {
      for(int i = 0; i < 36; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private void performDoubleEscape() {
      if (this.doubleEscapeRemaining > 0) {
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         --this.doubleEscapeRemaining;
         this.waitTicks = 5;
      } else {
         this.state = AutoBoneOrder.State.DOUBLE_RIGHTCLICK_FIRST;
         this.waitTicks = 5;
      }
   }

   private void doubleRightClickFirst() {
      if (!this.setSpawnerFromCrosshairOrKeep()) {
         this.toggle();
      } else {
         mc.options.useKey.setPressed(true);
         this.hasPressed = true;
         this.state = AutoBoneOrder.State.DOUBLE_RIGHTCLICK_SECOND;
         this.waitTicks = 5;
      }
   }

   private void doubleRightClickSecond() {
      if (!this.setSpawnerFromCrosshairOrKeep()) {
         this.toggle();
      } else {
         mc.options.useKey.setPressed(true);
         this.hasPressed = true;
         this.state = AutoBoneOrder.State.POST_CYCLE_DELAY;
         this.waitTicks = 5;
      }
   }

   private void postCycleDelay() {
      this.state = AutoBoneOrder.State.FINDING_SPAWNER;
      this.timeoutTicks = ((Double)this.guiTimeout.getValue()).intValue();
      this.waitTicks = 20;
   }

   private boolean setSpawnerFromCrosshairOrKeep() {
      HitResult var2 = mc.crosshairTarget;
      if (var2 instanceof BlockHitResult) {
         BlockHitResult bhr = (BlockHitResult)var2;
         BlockPos targetPos = bhr.getBlockPos();
         if (mc.world.getBlockState(targetPos).isOf(Blocks.SPAWNER)) {
            this.spawnerPos = targetPos.toImmutable();
            return true;
         }
      }

      return this.spawnerPos != null && mc.world.getBlockState(this.spawnerPos).isOf(Blocks.SPAWNER);
   }

   private static enum State {
      IDLE,
      FINDING_SPAWNER,
      OPENING_SPAWNER,
      WAITING_SPAWNER_GUI,
      LOOTING_BONES,
      CLOSING_SPAWNER,
      ORDER_COMMAND,
      WAIT_ORDER_GUI,
      SELECT_ORDER_ITEM,
      WAIT_DELIVERY_GUI,
      DELIVERING_BONES,
      WAIT_AFTER_DELIVERY_1,
      CLOSING_DELIVERY,
      WAIT_AFTER_CLOSE_DELIVERY,
      WAIT_CONFIRM_GUI,
      WAIT_CONFIRM_SETTLE,
      CLICK_CONFIRM_SLOT,
      WAIT_AFTER_CONFIRM_1,
      WAIT_AFTER_CONFIRM_2,
      WAIT_AFTER_CONFIRM_3,
      DOUBLE_ESCAPE,
      DOUBLE_RIGHTCLICK_FIRST,
      DOUBLE_RIGHTCLICK_SECOND,
      POST_CYCLE_DELAY;

      // $FF: synthetic method
      private static AutoBoneOrder.State[] $values() {
         return new AutoBoneOrder.State[]{IDLE, FINDING_SPAWNER, OPENING_SPAWNER, WAITING_SPAWNER_GUI, LOOTING_BONES, CLOSING_SPAWNER, ORDER_COMMAND, WAIT_ORDER_GUI, SELECT_ORDER_ITEM, WAIT_DELIVERY_GUI, DELIVERING_BONES, WAIT_AFTER_DELIVERY_1, CLOSING_DELIVERY, WAIT_AFTER_CLOSE_DELIVERY, WAIT_CONFIRM_GUI, WAIT_CONFIRM_SETTLE, CLICK_CONFIRM_SLOT, WAIT_AFTER_CONFIRM_1, WAIT_AFTER_CONFIRM_2, WAIT_AFTER_CONFIRM_3, DOUBLE_ESCAPE, DOUBLE_RIGHTCLICK_FIRST, DOUBLE_RIGHTCLICK_SECOND, POST_CYCLE_DELAY};
      }
   }
}
