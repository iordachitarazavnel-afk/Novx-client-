package foure.dev.util.others;

import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.ItemCooldownManager.Entry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemUtil implements Wrapper {
   public static int maxUseTick(Item item) {
      return maxUseTick(item.getDefaultStack());
   }

   public static int maxUseTick(ItemStack stack) {
      int var10000;
      switch(stack.getUseAction()) {
      case EAT:
      case DRINK:
         var10000 = 32;
         break;
      case CROSSBOW:
      case SPEAR:
         var10000 = 10;
         break;
      case BOW:
         var10000 = 20;
         break;
      case BLOCK:
         var10000 = 0;
         break;
      default:
         var10000 = stack.getMaxUseTime(mc.player);
      }

      return var10000;
   }

   public static float getCooldownProgress(Item item) {
      ItemCooldownManager cooldownManager = mc.player.getItemCooldownManager();
      Entry entry = (Entry)cooldownManager.entries.get(item);
      return entry == null ? 0.0F : Math.max(0.0F, (float)(entry.endTick - cooldownManager.tick) / 20.0F);
   }

   @Generated
   private ItemUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
