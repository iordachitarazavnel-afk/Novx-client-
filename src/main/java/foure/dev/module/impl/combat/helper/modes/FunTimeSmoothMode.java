package foure.dev.module.impl.combat.helper.modes;

import foure.dev.FourEClient;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleSmoothMode;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.attack.AttackHandler;
import foure.dev.util.math.TimerUtil;
import java.security.SecureRandom;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class FunTimeSmoothMode extends AngleSmoothMode {
   public FunTimeSmoothMode() {
      super("FunTime");
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).isToggled()) {
         AttackHandler attackHandler = FourEClient.getInstance().getAttackPerpetrator().getAttackHandler();
         TimerUtil attackTimer = attackHandler.getAttackTimer();
         int count = attackHandler.getCount();
         Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
         float speed;
         float random;
         float yaw;
         if (entity != null) {
            speed = attackHandler.canAttack(((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).getConfig(), 0) ? 1.0F : ((new SecureRandom()).nextBoolean() ? 0.4F : 0.2F);
            speed = Math.abs(yawDelta / rotationDifference) * 180.0F;
            random = Math.abs(pitchDelta / rotationDifference) * 180.0F;
            float moveYaw = MathHelper.clamp(yawDelta, -speed, speed);
            yaw = MathHelper.clamp(pitchDelta, -random, random);
            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(this.randomLerp(speed, speed + 0.2F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
            moveAngle.setPitch(MathHelper.lerp(this.randomLerp(speed, speed + 0.2F), currentAngle.getPitch(), currentAngle.getPitch() + yaw));
            return moveAngle;
         } else {
            int suck = count % 3;
            speed = attackTimer.finished(400.0D) ? ((new SecureRandom()).nextBoolean() ? 0.4F : 0.2F) : -0.2F;
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
               var10000 = new Angle((float)(-Math.cos((double)random % 1.11D)), (float)Math.sin((double)random % 1.11D));
            }

            Angle randomAngle = var10000;
            yaw = !attackTimer.finished(2000.0D) ? this.randomLerp(20.0F, 30.0F) * randomAngle.getYaw() : 0.0F;
            float pitch2 = this.randomLerp(0.0F, 2.0F) * (float)Math.cos((double)System.currentTimeMillis() / 5000.0D);
            float pitch = !attackTimer.finished(2000.0D) ? this.randomLerp(4.0F, 10.0F) * randomAngle.getPitch() + pitch2 : 0.0F;
            float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
            float linePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
            float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
            float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
            Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
            moveAngle.setYaw(MathHelper.lerp(Math.clamp(this.randomLerp(speed, speed + 0.2F), 0.0F, 1.0F), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw) + yaw);
            moveAngle.setPitch(MathHelper.lerp(Math.clamp(this.randomLerp(speed, speed + 0.2F), 0.0F, 1.0F), currentAngle.getPitch(), currentAngle.getPitch() + movePitch) + pitch);
            return moveAngle;
         }
      } else {
         return AngleUtil.cameraAngle();
      }
   }

   public Vec3d randomValue() {
      return new Vec3d(0.06D, 0.1D, 0.06D);
   }

   private float randomLerp(float min, float max) {
      return MathHelper.lerp((new SecureRandom()).nextFloat(), min, max);
   }
}
