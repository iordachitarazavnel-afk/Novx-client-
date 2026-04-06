package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;

@ModuleInfo(
   name = "CrystalOptimizer",
   category = Category.COMBAT,
   desc = "Marlowww based crystal optimizer - crystals faster"
)
public class CrystalOptimizer extends Function {
   @Subscribe
   public void onPacket(PacketEvent event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof PlayerInteractEntityC2SPacket) {
         PlayerInteractEntityC2SPacket packet = (PlayerInteractEntityC2SPacket)var3;
         HitResult var4 = mc.crosshairTarget;
         if (var4 instanceof EntityHitResult) {
            EntityHitResult hit = (EntityHitResult)var4;
            if (hit.getType() == Type.ENTITY) {
               Entity var5 = hit.getEntity();
               if (var5 instanceof EndCrystalEntity) {
                  EndCrystalEntity crystal = (EndCrystalEntity)var5;
                  boolean weakness = mc.player.hasStatusEffect(StatusEffects.WEAKNESS);
                  boolean strength = mc.player.hasStatusEffect(StatusEffects.STRENGTH);
                  int weaknessLvl = weakness ? mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier() : -1;
                  int strengthLvl = strength ? mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() : -1;
                  if (weakness && strengthLvl <= weaknessLvl && !this.isTool(mc.player.getMainHandStack())) {
                     return;
                  }

                  crystal.discard();
                  crystal.setRemoved(RemovalReason.KILLED);
                  crystal.onRemoved();
               }
            }
         }
      }

   }

   private boolean isTool(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         return stack.getItem() == Items.DIAMOND_PICKAXE || stack.getItem() == Items.NETHERITE_PICKAXE || stack.getItem() == Items.DIAMOND_AXE || stack.getItem() == Items.NETHERITE_AXE || stack.getItem() == Items.DIAMOND_SWORD || stack.getItem() == Items.NETHERITE_SWORD || stack.getItem() == Items.DIAMOND_SHOVEL || stack.getItem() == Items.NETHERITE_SHOVEL;
      }
   }
}
