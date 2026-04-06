package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventPlayerTick;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import java.util.Iterator;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(
   name = "AutoTool",
   category = Category.MISC,
   desc = "Automatically switches to the best tool for the job."
)
public class AutoTool extends Function {
   @Subscribe
   public void onTick(EventPlayerTick event) {
      if (!fullNullCheck()) {
         if (mc.interactionManager.isBreakingBlock()) {
            HitResult crosshairTarget = mc.crosshairTarget;
            if (crosshairTarget != null && crosshairTarget.getType() == Type.BLOCK) {
               BlockHitResult blockHitResult = (BlockHitResult)crosshairTarget;
               BlockState blockState = mc.world.getBlockState(blockHitResult.getBlockPos());
               this.switchToBestTool(blockState);
            }
         }

         if (mc.options.attackKey.isPressed() && mc.targetedEntity != null) {
            this.switchToBestWeapon();
         }

      }
   }

   private void switchToBestTool(BlockState blockState) {
      PlayerInventory inventory = mc.player.getInventory();
      int bestSlot = -1;
      float bestSpeed = 1.0F;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = inventory.getStack(i);
         if (!stack.isEmpty()) {
            float speed = stack.getMiningSpeedMultiplier(blockState);
            if (speed > bestSpeed) {
               bestSpeed = speed;
               bestSlot = i;
            }
         }
      }

      if (bestSlot != -1 && inventory.selectedSlot != bestSlot) {
         inventory.selectedSlot = bestSlot;
      }

   }

   private void switchToBestWeapon() {
      PlayerInventory inventory = mc.player.getInventory();
      int bestSlot = -1;
      double bestDamage = 0.0D;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = inventory.getStack(i);
         if (!stack.isEmpty()) {
            AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            double damage = 0.0D;
            if (modifiers != null) {
               Iterator var10 = modifiers.modifiers().iterator();

               while(var10.hasNext()) {
                  Entry entry = (Entry)var10.next();
                  RegistryEntry<?> attribute = entry.attribute();
                  if (attribute.equals(EntityAttributes.ATTACK_DAMAGE)) {
                     damage += entry.modifier().value();
                  }
               }
            }

            if (damage > bestDamage) {
               bestDamage = damage;
               bestSlot = i;
            }
         }
      }

      if (bestSlot != -1 && inventory.selectedSlot != bestSlot) {
         inventory.selectedSlot = bestSlot;
      }

   }
}
