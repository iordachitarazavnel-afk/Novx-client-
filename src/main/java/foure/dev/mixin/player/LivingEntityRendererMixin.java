package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import foure.dev.FourEClient;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({LivingEntityRenderer.class})
public abstract class LivingEntityRendererMixin implements Wrapper {
   @Shadow
   @Nullable
   protected abstract RenderLayer getRenderLayer(LivingEntityRenderState var1, boolean var2, boolean var3, boolean var4);

   @ModifyExpressionValue(
           method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"
           )}
   )
   private float lerpAngleDegreesHook(float original, @Local(ordinal = 0,argsOnly = true) LivingEntity entity, @Local(ordinal = 0,argsOnly = true) float delta) {
      if (entity.equals(mc.player) && ((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).isToggled()) {
         RotationController controller = RotationController.INSTANCE;
         return MathHelper.lerpAngleDegrees(delta, controller.getPreviousRotation().getYaw(), controller.getRotation().getYaw());
      } else {
         return original;
      }
   }

   @ModifyExpressionValue(
           method = {"updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"
           )}
   )
   private float getLerpedPitchHook(float original, @Local(ordinal = 0,argsOnly = true) LivingEntity entity, @Local(ordinal = 0,argsOnly = true) float delta) {
      if (entity.equals(mc.player) && ((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).isToggled()) {
         RotationController controller = RotationController.INSTANCE;
         return MathHelper.lerp(delta, controller.getPreviousRotation().getPitch(), controller.getRotation().getPitch());
      } else {
         return original;
      }
   }
}
