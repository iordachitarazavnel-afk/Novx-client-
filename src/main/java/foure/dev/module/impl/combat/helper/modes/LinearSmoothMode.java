package foure.dev.module.impl.combat.helper.modes;

import foure.dev.FourEClient;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleSmoothMode;
import foure.dev.module.impl.combat.helper.AngleUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class LinearSmoothMode extends AngleSmoothMode {
   public LinearSmoothMode() {
      super("Linear");
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).isToggled()) {
         Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
         float straightLineYaw = Math.abs(yawDelta / rotationDifference) * 360.0F;
         float straightLinePitch = Math.abs(pitchDelta / rotationDifference) * 360.0F;
         return new Angle(currentAngle.getYaw() + Math.min(Math.max(yawDelta, -straightLineYaw), straightLineYaw), currentAngle.getPitch() + Math.min(Math.max(pitchDelta, -straightLinePitch), straightLinePitch));
      } else {
         return AngleUtil.cameraAngle();
      }
   }

   public Vec3d randomValue() {
      return new Vec3d(0.0D, 0.0D, 0.0D);
   }
}
