package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.player.EventAttackEntity;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

@ModuleInfo(
   name = "AutoWeapon",
   category = Category.COMBAT,
   desc = "Switches to the best weapon when attacking"
)
public class AutoWeapon extends Function {
   private final BooleanSetting preferMace = new BooleanSetting("Prefer Mace", true);

   public AutoWeapon() {
      this.addSettings(new Setting[]{this.preferMace});
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (mc.player != null) {
         int bestSlot = -1;
         double maxDamage = -1.0D;

         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && this.isWeapon(stack.getItem())) {
               double damage = this.getAttackDamage(stack);
               if ((Boolean)this.preferMace.getValue() && stack.getItem() == Items.MACE) {
                  damage += 100.0D;
               }

               if (damage > maxDamage) {
                  maxDamage = damage;
                  bestSlot = i;
               }
            }
         }

         if (bestSlot != -1 && bestSlot != mc.player.getInventory().selectedSlot) {
            mc.player.getInventory().selectedSlot = bestSlot;
         }

      }
   }

   private boolean isWeapon(Item item) {
      return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.STONE_SWORD || item == Items.WOODEN_SWORD || item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE || item == Items.IRON_AXE || item == Items.GOLDEN_AXE || item == Items.STONE_AXE || item == Items.WOODEN_AXE || item == Items.MACE || item == Items.TRIDENT;
   }

   private double getAttackDamage(ItemStack stack) {
      Item item = stack.getItem();
      if (item == Items.NETHERITE_SWORD) {
         return 8.0D;
      } else if (item == Items.DIAMOND_SWORD) {
         return 7.0D;
      } else if (item == Items.IRON_SWORD) {
         return 6.0D;
      } else if (item == Items.GOLDEN_SWORD) {
         return 4.0D;
      } else if (item == Items.STONE_SWORD) {
         return 5.0D;
      } else if (item == Items.WOODEN_SWORD) {
         return 4.0D;
      } else if (item == Items.NETHERITE_AXE) {
         return 10.0D;
      } else if (item == Items.DIAMOND_AXE) {
         return 9.0D;
      } else if (item == Items.IRON_AXE) {
         return 9.0D;
      } else if (item == Items.GOLDEN_AXE) {
         return 7.0D;
      } else if (item == Items.STONE_AXE) {
         return 9.0D;
      } else if (item == Items.WOODEN_AXE) {
         return 7.0D;
      } else if (item == Items.MACE) {
         return 7.0D;
      } else {
         return item == Items.TRIDENT ? 9.0D : 0.0D;
      }
   }
}
