package foure.dev.mixin.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.FireworkEvent;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({FireworkRocketEntity.class})
public abstract class FireworkRocketEntityMixin extends ProjectileEntity implements Wrapper {
   @Shadow
   private LivingEntity shooter;
   @Unique
   private Vec3d rotation;

   public FireworkRocketEntityMixin(EntityType<? extends ProjectileEntity> entityType, World world) {
      super(entityType, world);
   }

   @WrapOperation(
           method = {"tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
           )}
   )
   public Vec3d getRotationVectorHook(LivingEntity instance, Operation<Vec3d> original) {
      Vec3d result;
      if (this.shooter == mc.player) {
         result = RotationController.INSTANCE.getMoveRotation().toVector();
      } else {
         result = (Vec3d)original.call(new Object[]{instance});
      }

      this.rotation = result;
      return result;
   }

   @WrapOperation(
           method = {"tick"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/entity/LivingEntity;getVelocity()Lnet/minecraft/util/math/Vec3d;",
                   ordinal = 0
           )}
   )
   public Vec3d getVelocityHook(LivingEntity instance, Operation<Vec3d> original) {
      if (this.shooter == mc.player) {
         FireworkEvent event = new FireworkEvent((Vec3d)original.call(new Object[]{instance}));
         FourEClient.getInstance().getEventBus().post(event);
         return event.getVector();
      } else {
         return (Vec3d)original.call(new Object[]{instance});
      }
   }
}
