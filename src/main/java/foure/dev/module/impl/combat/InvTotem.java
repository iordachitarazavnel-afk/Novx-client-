package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(
   name = "InvTotem",
   category = Category.COMBAT,
   desc = "Automatically equips totems from your inventory"
)
public class InvTotem extends Function {
   private final ModeSetting mode = new ModeSetting("Mode", "Blatant", new String[]{"Blatant", "Random"});
   private final BooleanSetting autoOpen = new BooleanSetting("Auto Open", true);
   private final NumberSetting stayOpenFor = new NumberSetting("Stay Open For", this, 2.0D, 0.0D, 20.0D, 1.0D);
   private final List<Long> recentIntervals = new ArrayList();
   private final Random random = new Random();
   private long nextActionTime = 0L;
   private int closeClock = -1;
   private boolean justOpenedInventory = false;

   public InvTotem() {
      this.addSettings(new Setting[]{this.mode, this.autoOpen, this.stayOpenFor});
   }

   public void onEnable() {
      super.onEnable();
      this.nextActionTime = System.currentTimeMillis();
      this.closeClock = -1;
      this.justOpenedInventory = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.player != null) {
         long currentTime = System.currentTimeMillis();
         if (this.shouldOpenScreen() && (Boolean)this.autoOpen.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
            mc.setScreen(new InventoryScreen(mc.player));
            this.justOpenedInventory = true;
            this.nextActionTime = currentTime + 150L + (long)this.random.nextInt(100);
         } else if (!(mc.currentScreen instanceof InventoryScreen)) {
            this.nextActionTime = currentTime;
            this.closeClock = -1;
         } else {
            if (this.closeClock == -1) {
               this.closeClock = (int)this.stayOpenFor.getValueFloat() + this.random.nextInt(3);
            }

            if (currentTime >= this.nextActionTime) {
               if (this.justOpenedInventory) {
                  this.justOpenedInventory = false;
                  this.nextActionTime = currentTime + this.generateHumanDelay();
               } else {
                  PlayerInventory inventory = mc.player.getInventory();
                  if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                     int slot = this.chooseTotemSlot();
                     if (slot != -1) {
                        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, this.remapSlot(slot), 40, SlotActionType.SWAP, mc.player);
                        this.nextActionTime = currentTime + this.generateHumanDelay();
                        return;
                     }
                  }

                  if (this.shouldCloseScreen() && (Boolean)this.autoOpen.getValue()) {
                     if (this.closeClock > 0) {
                        --this.closeClock;
                        return;
                     }

                     mc.setScreen((Screen)null);
                     this.closeClock = (int)this.stayOpenFor.getValueFloat() + this.random.nextInt(3);
                  }

               }
            }
         }
      }
   }

   private boolean shouldCloseScreen() {
      return mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
   }

   private boolean shouldOpenScreen() {
      return mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING && this.countItemExceptHotbar(Items.TOTEM_OF_UNDYING) != 0;
   }

   private int countItemExceptHotbar(Item item) {
      int count = 0;

      for(int i = 9; i < 36; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            count += mc.player.getInventory().getStack(i).getCount();
         }
      }

      return count;
   }

   private int remapSlot(int slot) {
      return slot < 9 ? 36 + slot : slot;
   }

   private int chooseTotemSlot() {
      if (((String)this.mode.getValue()).equals("Blatant")) {
         return this.random.nextDouble() < 0.7D ? this.findTotemSlot() : this.findRandomTotemSlot();
      } else {
         return this.findRandomTotemSlot();
      }
   }

   private int findTotemSlot() {
      for(int i = 9; i < 36; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      return -1;
   }

   private int findRandomTotemSlot() {
      List<Integer> totemSlots = new ArrayList();

      for(int i = 9; i < 36; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
            totemSlots.add(i);
         }
      }

      return totemSlots.isEmpty() ? -1 : (Integer)totemSlots.get(this.random.nextInt(totemSlots.size()));
   }

   private long generateHumanDelay() {
      long lastDelay = this.recentIntervals.isEmpty() ? 120L : (Long)this.recentIntervals.get(this.recentIntervals.size() - 1);
      long base = (long)(100 + this.random.nextInt(100));
      long correlatedJitter = (long)((this.random.nextDouble() - 0.5D) * (double)(50 + this.random.nextInt(50)));
      long longPause = 0L;
      if (this.random.nextDouble() < 0.1D) {
         longPause = (long)(200 + this.random.nextInt(300));
      }

      long delay = base + correlatedJitter + longPause;
      delay = (delay + lastDelay) / 2L;
      delay = Math.max(50L, Math.min(600L, delay));
      this.recentIntervals.add(delay);
      if (this.recentIntervals.size() > 50) {
         this.recentIntervals.remove(0);
      }

      return delay;
   }
}
