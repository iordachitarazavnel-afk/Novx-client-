package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({PlayerEntity.class})
public class PlayerEntityMixin implements Wrapper {
   @ModifyExpressionValue(
           method = {"knockbackTarget", "doSweepingAttack"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
           )}
   )
   private float hookAttackRotation(float original) {
      return RotationController.INSTANCE.getMoveRotation().getYaw();
   }
}
