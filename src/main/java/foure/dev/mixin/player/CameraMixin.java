package foure.dev.mixin.player;

import foure.dev.FourEClient;
import foure.dev.event.impl.game.CameraPositionEvent;
import foure.dev.event.impl.player.CameraEvent;
import foure.dev.module.impl.combat.helper.Angle;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3d pos;
   @Shadow
   @Final
   private Mutable blockPos;
   @Shadow
   private float yaw;
   @Shadow
   private float pitch;

   @Shadow
   public abstract void setRotation(float var1, float var2);

   @Shadow
   protected abstract void moveBy(float var1, float var2, float var3);

   @Shadow
   protected abstract float clipToSpace(float var1);

   @Inject(
           method = {"update"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V",
                   shift = Shift.AFTER
           )},
           cancellable = true
   )
   private void updateHook(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
      CameraEvent event = new CameraEvent(false, 4.0F, new Angle(this.yaw, this.pitch));
      FourEClient.getInstance().getEventBus().post(event);
      Angle angle = event.getAngle();
      if (event.isCanceled() && focusedEntity instanceof ClientPlayerEntity) {
         ClientPlayerEntity player = (ClientPlayerEntity)focusedEntity;
         if (!player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (float)(inverseView ? 180 : 0);
            float distance = event.getDistance();
            this.setRotation(yaw, pitch);
            this.moveBy(event.isCameraClip() ? -distance : -this.clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
         }
      }

   }

   @Inject(
           method = {"setPos(Lnet/minecraft/util/math/Vec3d;)V"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void posHook(Vec3d pos, CallbackInfo ci) {
      CameraPositionEvent event = new CameraPositionEvent(pos);
      FourEClient.getInstance().getEventBus().post(event);
      this.pos = pos = event.getPos();
      this.blockPos.set(pos.x, pos.y, pos.z);
      ci.cancel();
   }
}
