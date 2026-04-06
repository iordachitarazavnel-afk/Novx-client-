package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.utils.ChatUtils;
import java.util.Iterator;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;

@ModuleInfo(
   name = "LegitTridentFly",
   category = Category.DONUT,
   desc = "Undetectable Trident Fly. Requires Rain, RipTide"
)
public class LegitTridentFly extends Function {
   private boolean isCharging = false;
   private boolean isReleasing = false;
   private int previousSlot = -1;
   private int releaseCounter = 0;
   private long lastTime;

   public void onEnable() {
      this.isCharging = false;
      this.isReleasing = false;
      this.previousSlot = -1;
      this.releaseCounter = 0;
      if (mc.player != null && !this.selectTrident()) {
         ChatUtils.sendMessage("No riptide trident found in hotbar!");
         this.toggle();
      } else {
         this.lastTime = System.nanoTime();
      }
   }

   public void onDisable() {
      if (mc.options.useKey.isPressed()) {
         mc.options.useKey.setPressed(false);
      }

      if (this.previousSlot != -1 && mc.player != null) {
         mc.player.getInventory().selectedSlot = this.previousSlot;
         this.previousSlot = -1;
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && mc.world != null) {
         long now = System.nanoTime();
         float deltaSeconds = (float)(now - this.lastTime) / 1.0E9F;
         this.lastTime = now;
         if (mc.world.getRainGradient(deltaSeconds) == 0.0F) {
            ChatUtils.sendMessage("It needs to be raining to use this!");
            this.toggle();
         } else {
            ItemStack heldItem = mc.player.getMainHandStack();
            if (!this.selectTrident()) {
               ChatUtils.sendMessage("No riptide trident found in hotbar!");
               this.toggle();
            } else {
               int maxUseTime;
               if (heldItem.isDamageable()) {
                  int maxDamage = heldItem.getMaxDamage();
                  int currentDamage = heldItem.getDamage();
                  maxUseTime = maxDamage - currentDamage;
                  double durabilityPercent = (double)maxUseTime / (double)maxDamage * 100.0D;
                  if (durabilityPercent <= 20.0D) {
                     ChatUtils.sendMessage("Trident durability too low!");
                     this.toggle();
                     return;
                  }
               }

               boolean hasRiptide = false;
               ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(heldItem);
               Iterator var13 = enchantments.getEnchantments().iterator();

               while(var13.hasNext()) {
                  RegistryEntry<Enchantment> entry = (RegistryEntry)var13.next();
                  if (entry.matchesKey(Enchantments.RIPTIDE)) {
                     hasRiptide = true;
                     break;
                  }
               }

               if (!hasRiptide) {
                  ChatUtils.sendMessage("No riptide trident found in hotbar!");
                  this.toggle();
               } else {
                  maxUseTime = heldItem.getMaxUseTime(mc.player);
                  int useTimeLeft = mc.player.getItemUseTimeLeft();
                  int chargeTime = maxUseTime - useTimeLeft;
                  if (this.isReleasing) {
                     ++this.releaseCounter;
                     if (this.releaseCounter >= 1) {
                        this.isReleasing = false;
                        this.isCharging = true;
                        mc.options.useKey.setPressed(true);
                     }
                  } else if (this.isCharging) {
                     if (mc.player.isUsingItem() && chargeTime >= 10) {
                        mc.options.useKey.setPressed(false);
                        this.isCharging = false;
                        this.isReleasing = true;
                        this.releaseCounter = 0;
                     } else if (!mc.player.isUsingItem()) {
                        mc.options.useKey.setPressed(true);
                     }
                  } else {
                     this.isCharging = true;
                     mc.options.useKey.setPressed(true);
                  }

               }
            }
         }
      }
   }

   private boolean selectTrident() {
      if (mc.player == null) {
         return false;
      } else {
         if (this.previousSlot == -1) {
            this.previousSlot = mc.player.getInventory().selectedSlot;
         }

         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.TRIDENT)) {
               ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
               Iterator var4 = enchantments.getEnchantments().iterator();

               while(var4.hasNext()) {
                  RegistryEntry<Enchantment> entry = (RegistryEntry)var4.next();
                  if (entry.matchesKey(Enchantments.RIPTIDE)) {
                     mc.player.getInventory().selectedSlot = i;
                     return true;
                  }
               }
            }
         }

         return false;
      }
   }
}
