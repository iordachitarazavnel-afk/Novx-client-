package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.JumpEvent;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements Wrapper {

   @Shadow
   public float bodyYaw;

   @Unique
   private final MinecraftClient client = MinecraftClient.getInstance();

   public LivingEntityMixin(EntityType<?> type, World world) {
      super(type, world);
   }

   @Inject(
           method = "jump",
           at = @At("HEAD"),
           cancellable = true
   )
   private void jump(CallbackInfo info) {
      if ((Object)this instanceof ClientPlayerEntity) {
         ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;

         JumpEvent event = new JumpEvent(player);
         FourEClient.getInstance().getEventBus().post(event);

         if (event.isCanceled()) {
            info.cancel();
         }
      }
   }

   @ModifyExpressionValue(
           method = "jump",
           at = @At(
                   value = "NEW",
                   target = "(DDD)Lnet/minecraft/util/math/Vec3d;"
           )
   )
   private Vec3d hookFixRotation(Vec3d original) {
      if ((Object) this != client.player) {
         return original;
      } else {
         float yaw = RotationController.INSTANCE.getMoveRotation().getYaw() * 0.017453292F;

         return new Vec3d(
                 -MathHelper.sin((double) yaw) * 0.2F,
                 0.0D,
                 MathHelper.cos((double) yaw) * 0.2F
         );
      }
   }

   @ModifyExpressionValue(
           method = "calcGlidingVelocity",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getPitch()F"
           )
   )
   private float hookModifyFallFlyingPitch(float original) {
      return ((Object) this != client.player)
              ? original
              : RotationController.INSTANCE.getMoveRotation().getPitch();
   }

   @ModifyExpressionValue(
           method = "calcGlidingVelocity",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
           )
   )
   private Vec3d hookModifyFallFlyingRotationVector(Vec3d original) {
      return ((Object) this != client.player)
              ? original
              : RotationController.INSTANCE.getMoveRotation().toVector();
   }

   @ModifyExpressionValue(
           method = "turnHead",
           at = @At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/util/math/MathHelper;wrapDegrees(F)F",
                   ordinal = 1
           )
   )
   private float wrapDegreesHook(float original) {
      return ((Object) this == client.player)
              ? MathHelper.wrapDegrees(RotationController.INSTANCE.getRotation().getYaw() - this.bodyYaw)
              : original;
   }
}