package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.mixin.accessor.HandledScreenAccessor;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(
   name = "HoverTotem",
   category = Category.COMBAT,
   desc = "Equips a totem in offhand when hovered"
)
public class HoverTotem extends Function {
   private final BooleanSetting hotbarTotem = new BooleanSetting("Hotbar Totem", true);
   private final NumberSetting hotbarSlot = new NumberSetting("Hotbar Slot", this, 1.0D, 1.0D, 9.0D, 1.0D);
   private final BooleanSetting autoSwitchToTotem = new BooleanSetting("Auto Switch To Totem", false);
   private final BooleanSetting autoInvOpen = new BooleanSetting("Auto Inv Open", false);
   private boolean shouldOpenInv = false;
   private boolean totemEquipped = false;
   private boolean wasAutoOpened = false;

   public HoverTotem() {
      this.addSettings(new Setting[]{this.hotbarTotem, this.hotbarSlot, this.autoSwitchToTotem, this.autoInvOpen});
   }

   public void onEnable() {
      super.onEnable();
      this.shouldOpenInv = false;
      this.totemEquipped = false;
      this.wasAutoOpened = false;
   }

   public void onDisable() {
      super.onDisable();
      this.shouldOpenInv = false;
      this.totemEquipped = false;
      this.wasAutoOpened = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         if ((Boolean)this.autoInvOpen.getValue() && !mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) && this.hasTotemInInventory() && mc.currentScreen == null) {
            this.shouldOpenInv = true;
         }

         if (this.shouldOpenInv && mc.currentScreen == null) {
            if (this.hasTotemInInventory()) {
               mc.execute(() -> {
                  if (mc.player != null) {
                     mc.setScreen(new InventoryScreen(mc.player));
                     this.shouldOpenInv = false;
                     this.totemEquipped = false;
                     this.wasAutoOpened = true;
                  }

               });
            } else {
               this.shouldOpenInv = false;
            }

         } else {
            Screen currentScreen = mc.currentScreen;
            if (currentScreen instanceof InventoryScreen) {
               InventoryScreen inventoryScreen = (InventoryScreen)currentScreen;
               if ((Boolean)this.autoInvOpen.getValue() && !this.totemEquipped && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  this.totemEquipped = true;
                  this.wasAutoOpened = false;
                  mc.execute(() -> {
                     mc.player.closeHandledScreen();
                     mc.mouse.lockCursor();
                  });
               } else {
                  Slot focusedSlot = this.getFocusedSlot(inventoryScreen);
                  if (focusedSlot != null && focusedSlot.getIndex() <= 35) {
                     if ((Boolean)this.autoSwitchToTotem.getValue()) {
                        mc.player.getInventory().selectedSlot = (int)this.hotbarSlot.getValueFloat() - 1;
                     }

                     if (focusedSlot.getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                        int slotIndex = focusedSlot.getIndex();
                        int syncId = ((PlayerScreenHandler)inventoryScreen.getScreenHandler()).syncId;
                        if (!mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                           this.equipOffhandTotem(syncId, slotIndex);
                        } else {
                           if ((Boolean)this.hotbarTotem.getValue()) {
                              int hotbarIndex = (int)this.hotbarSlot.getValueFloat() - 1;
                              if (!mc.player.getInventory().getStack(hotbarIndex).isOf(Items.TOTEM_OF_UNDYING)) {
                                 this.equipHotbarTotem(syncId, slotIndex, hotbarIndex);
                              }
                           }

                        }
                     }
                  }
               }
            } else {
               if (this.wasAutoOpened && !this.totemEquipped && mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
                  this.totemEquipped = true;
                  this.wasAutoOpened = false;
               }

               if (this.wasAutoOpened && !this.totemEquipped) {
                  this.shouldOpenInv = true;
               }

            }
         }
      }
   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof EntityStatusS2CPacket) {
         EntityStatusS2CPacket statusPacket = (EntityStatusS2CPacket)var3;
         if (statusPacket.getStatus() == 35 && mc.player != null && statusPacket.getEntity(mc.world) == mc.player && (Boolean)this.autoInvOpen.getValue() && mc.currentScreen == null && this.hasTotemInInventory()) {
            this.shouldOpenInv = true;
            this.totemEquipped = false;
            this.wasAutoOpened = true;
         }
      }

   }

   private boolean hasTotemInInventory() {
      if (mc.player == null) {
         return false;
      } else {
         for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.TOTEM_OF_UNDYING)) {
               return true;
            }
         }

         return false;
      }
   }

   private void equipOffhandTotem(int syncId, int slotIndex) {
      mc.interactionManager.clickSlot(syncId, slotIndex, 40, SlotActionType.SWAP, mc.player);
   }

   private void equipHotbarTotem(int syncId, int slotIndex, int hotbarIndex) {
      mc.interactionManager.clickSlot(syncId, slotIndex, hotbarIndex, SlotActionType.SWAP, mc.player);
   }

   private Slot getFocusedSlot(HandledScreen<?> screen) {
      return ((HandledScreenAccessor)screen).getFocusedSlot();
   }
}
