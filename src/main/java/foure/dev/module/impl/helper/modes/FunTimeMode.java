package foure.dev.module.impl.combat.helper.modes;

import foure.dev.FourEClient;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleSmoothMode;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.attack.AttackHandler;
import foure.dev.util.math.TimerUtil;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FunTimeMode extends AngleSmoothMode {
   public FunTimeMode() {
      super("FunTimeMode");
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      Killaura aura = (Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class);
      if (!aura.isToggled()) {
         return AngleUtil.cameraAngle();
      } else {
         AttackHandler attackHandler = FourEClient.getInstance().getAttackPerpetrator().getAttackHandler();
         TimerUtil attackTimer = attackHandler.getAttackTimer();
         int count = attackHandler.getCount();
         Angle delta = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float yawDelta = delta.getYaw();
         float pitchDelta = delta.getPitch();
         float newYaw;
         float newPitch;
         float speed;
         float random;
         float smoothPitchDelta;
         if (entity != null) {
            boolean canAttack = attackHandler.canAttack(aura.getConfig(), 0);
            speed = canAttack ? 0.9F : 0.6F;
            random = this.randomLerp(speed, speed + 0.1F);
            smoothPitchDelta = 73.0F;
            yawDelta = MathHelper.clamp(yawDelta, -smoothPitchDelta, smoothPitchDelta);
            pitchDelta = MathHelper.clamp(pitchDelta, -smoothPitchDelta, smoothPitchDelta);
            newYaw = currentAngle.getYaw() + yawDelta * random;
            newPitch = currentAngle.getPitch() + pitchDelta * random;
         } else {
            int suck = count % 3;
            speed = attackTimer.finished(400.0D) ? (this.randomBool() ? 0.4F : 0.2F) : -0.2F;
            random = (float)attackTimer.elapsedTime() / 40.0F + (float)(count % 7);
            Angle var10000;
            switch(suck) {
            case 0:
               var10000 = new Angle((float)Math.cos((double)(random % 2.0F)), (float)Math.sin((double)(random % 2.0F)));
               break;
            case 1:
               var10000 = new Angle((float)Math.sin((double)random % 1.4D), (float)Math.cos((double)random % 1.4D));
               break;
            case 2:
               var10000 = new Angle((float)Math.sin((double)(random % 3.0F)), (float)(-Math.cos((double)(random % 3.0F))));
               break;
            default:
               var10000 = new Angle((float)(-Math.cos((double)random % 1.523231D)), (float)Math.sin((double)random % 1.661D));
            }

            Angle randomAngle = var10000;
            float yawOffset = !attackTimer.finished(2000.0D) ? this.randomLerp(20.0F, 30.0F) * randomAngle.getYaw() : 0.0F;
            float pitch2 = this.randomLerp(0.0F, 2.0F) * (float)Math.cos((double)System.currentTimeMillis() / 5000.0D);
            float pitchOffset = !attackTimer.finished(2000.0D) ? this.randomLerp(4.0F, 10.0F) * randomAngle.getPitch() + pitch2 : 0.0F;
            float lerpFactor = MathHelper.clamp(this.randomLerp(speed, speed + 0.2F), 0.0F, 1.0F);
            newYaw = MathHelper.lerp(lerpFactor, currentAngle.getYaw(), currentAngle.getYaw() + yawDelta) + yawOffset;
            newPitch = MathHelper.lerp(lerpFactor, currentAngle.getPitch(), currentAngle.getPitch() + pitchDelta) + pitchOffset;
         }

         float sensitivity = (float)((Double)mc.options.getMouseSensitivity().getValue() * 0.800000011920929D + 0.20000000298023224D);
         speed = sensitivity * sensitivity * sensitivity * 1.2F;
         random = newYaw - currentAngle.getYaw();
         smoothPitchDelta = newPitch - currentAngle.getPitch();
         random -= random % speed;
         smoothPitchDelta -= smoothPitchDelta % speed;
         return new Angle(currentAngle.getYaw() + random, currentAngle.getPitch() + smoothPitchDelta);
      }
   }

   public Vec3d randomValue() {
      return new Vec3d((double)this.randomLerp(-0.5F, 0.5F), (double)this.randomLerp(-0.5F, 0.5F), (double)this.randomLerp(-0.5F, 0.5F));
   }

   private float randomLerp(float min, float max) {
      return MathHelper.lerp(ThreadLocalRandom.current().nextFloat(), min, max);
   }

   private boolean randomBool() {
      return ThreadLocalRandom.current().nextBoolean();
   }
}
