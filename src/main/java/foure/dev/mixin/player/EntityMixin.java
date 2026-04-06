package foure.dev.mixin.player;

import foure.dev.FourEClient;
import foure.dev.event.impl.player.PlayerVelocityStrafeEvent;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({Entity.class})
public abstract class EntityMixin implements Wrapper {
   @Shadow
   public abstract float getYaw();

   @Redirect(
           method = {"updateVelocity"},
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"
           )
   )
   public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
      if ((Object) this == mc.player) {
         float reportYaw = RotationController.INSTANCE.getRotation().getYaw();

         PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(
                 movementInput,
                 speed,
                 reportYaw,
                 Entity.movementInputToVelocity(movementInput, speed, reportYaw)
         );

         FourEClient.getInstance().getEventBus().post(event);
         return event.getVelocity();
      } else {
         return Entity.movementInputToVelocity(movementInput, speed, yaw);
      }
   }
}
