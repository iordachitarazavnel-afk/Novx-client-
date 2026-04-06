package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import foure.dev.FourEClient;
import foure.dev.event.impl.input.EventKeyboardInput;
import foure.dev.event.impl.input.InputEvent;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.module.impl.combat.helper.RotationPlan;
import foure.dev.util.Player.PlayerInventoryComponent;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin extends Input {
   @ModifyExpressionValue(
           method = {"tick"},
           at = {@At(
                   value = "NEW",
                   target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"
           )}
   )
   private PlayerInput tickHook(PlayerInput original) {
      InputEvent event = new InputEvent(original);
      FourEClient.getInstance().getEventBus().post(event);
      PlayerInventoryComponent.input(event);
      return this.transformInput(event.getInput());
   }

   @Inject(
           method = {"tick"},
           at = {@At("RETURN")}
   )
   public void onTick(CallbackInfo ci) {
      EventKeyboardInput event = new EventKeyboardInput(this.movementVector.y, this.movementVector.x);
      FourEClient.getInstance().getEventBus().post(event);
      this.movementVector = new Vec2f(event.getMovementSideways(), event.getMovementForward());
   }

   @Unique
   private PlayerInput transformInput(PlayerInput input) {
      RotationController rotationController = RotationController.INSTANCE;
      Angle angle = rotationController.getCurrentAngle();
      RotationPlan configurable = rotationController.getCurrentRotationPlan();
      if (Wrapper.mc.player != null && angle != null && configurable != null && configurable.isMoveCorrection() && configurable.isFreeCorrection()) {
         float deltaYaw = Wrapper.mc.player.getYaw() - angle.getYaw();
         float z = getMovementMultiplier(input.forward(), input.backward());
         float x = getMovementMultiplier(input.left(), input.right());
         float newX = x * MathHelper.cos((double)(deltaYaw * 0.017453292F)) - z * MathHelper.sin((double)(deltaYaw * 0.017453292F));
         float newZ = z * MathHelper.cos((double)(deltaYaw * 0.017453292F)) + x * MathHelper.sin((double)(deltaYaw * 0.017453292F));
         int movementSideways = Math.round(newX);
         int movementForward = Math.round(newZ);
         return new PlayerInput((float)movementForward > 0.0F, (float)movementForward < 0.0F, (float)movementSideways > 0.0F, (float)movementSideways < 0.0F, input.jump(), input.sneak(), input.sprint());
      } else {
         return input;
      }
   }

   @Unique
   private static float getMovementMultiplier(boolean positive, boolean negative) {
      if (positive == negative) {
         return 0.0F;
      } else {
         return positive ? 1.0F : -1.0F;
      }
   }
}
