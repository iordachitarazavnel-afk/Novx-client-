package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.CloseScreenEvent;
import foure.dev.event.impl.game.EventPlayerTick;
import foure.dev.event.impl.game.MotionEvent;
import foure.dev.event.impl.game.MoveEvent;
import foure.dev.event.impl.game.PostUpdateEvent;
import foure.dev.event.impl.player.UsingItemEvent;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientPlayerEntity.class})
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
   @Shadow
   protected abstract void autoJump(float var1, float var2);

   @Shadow
   public abstract boolean isSneaking();

   public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
      super(world, profile);
   }

   @ModifyExpressionValue(
           method = {"isBlockedFromSprinting", "applyMovementSpeedFactors"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"
           )}
   )
   private boolean usingItemHook(boolean original) {
      if (original) {
         UsingItemEvent event = new UsingItemEvent((byte)1);
         FourEClient.getInstance().getEventBus().post(event);
         if (event.isCanceled()) {
            return false;
         }
      }

      return original;
   }

   @Inject(
           method = {"tick"},
           at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventPlayerTick event = new EventPlayerTick();
      FourEClient.getInstance().getEventBus().post(event);
   }

   @Inject(
           method = {"tick"},
           at = {@At("RETURN")}
   )
   private void onTickEnd(CallbackInfo ci) {
      FourEClient.getInstance().getEventBus().post(new PostUpdateEvent());
   }

   @Inject(
           method = {"closeHandledScreen"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void closeHandledScreenHook(CallbackInfo info) {
      CloseScreenEvent event = new CloseScreenEvent(Wrapper.mc.currentScreen);
      FourEClient.getInstance().getEventBus().post(event);
      if (event.isCanceled()) {
         info.cancel();
      }

   }

   @ModifyExpressionValue(
           method = {"sendMovementPackets", "tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getYaw()F"
           )}
   )
   private float hookSilentRotationYaw(float original) {
      return RotationController.INSTANCE.getRotation().getYaw();
   }

   @ModifyExpressionValue(
           method = {"sendMovementPackets", "tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getPitch()F"
           )}
   )
   private float hookSilentRotationPitch(float original) {
      return RotationController.INSTANCE.getRotation().getPitch();
   }

   @Inject(
           method = {"sendMovementPackets"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void preMotion(CallbackInfo ci) {
      MotionEvent event = new MotionEvent(this.getX(), this.getY(), this.getZ(), this.getYaw(1.0F), this.getPitch(1.0F), this.isOnGround());
      FourEClient.getInstance().getEventBus().post(event);
      if (event.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
           method = {"move"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"
           )},
           cancellable = true
   )
   public void onMoveHook(MovementType movementType, Vec3d movement, CallbackInfo ci) {
      MoveEvent event = new MoveEvent(movement);
      FourEClient.getInstance().getEventBus().post(event);
      double d = this.getX();
      double e = this.getZ();
      super.move(movementType, event.getMovement());
      this.autoJump((float)(this.getX() - d), (float)(this.getZ() - e));
      ci.cancel();
   }
}
