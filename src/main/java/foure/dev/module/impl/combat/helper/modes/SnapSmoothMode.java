package foure.dev.module.impl.combat.helper.modes;

import foure.dev.FourEClient;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleSmoothMode;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.util.math.MathUtil;
import java.util.Random;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SnapSmoothMode extends AngleSmoothMode {
   Random rand = new Random();

   public SnapSmoothMode() {
      super("Snap");
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      if (((Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class)).isToggled()) {
         Angle angleDelta = AngleUtil.calculateDelta(currentAngle, targetAngle);
         float yawDelta = angleDelta.getYaw();
         float pitchDelta = angleDelta.getPitch();
         float rotationDifference = (float)Math.hypot((double)Math.abs(yawDelta), (double)Math.abs(pitchDelta));
         float speed = entity != null ? 1.0F : 0.9F;
         float lineYaw = Math.abs(yawDelta / rotationDifference) * 180.0F;
         float linePitch = Math.abs(pitchDelta / rotationDifference) * 180.0F;
         float moveYaw = MathHelper.clamp(yawDelta, -lineYaw, lineYaw);
         float movePitch = MathHelper.clamp(pitchDelta, -linePitch, linePitch);
         Angle moveAngle = new Angle(currentAngle.getYaw(), currentAngle.getPitch());
         moveAngle.setYaw(MathHelper.lerp(MathUtil.getRandom(speed, (double)speed + Math.cos((double)MathUtil.getRandom(0.01F, 4.0D))), currentAngle.getYaw(), currentAngle.getYaw() + moveYaw));
         moveAngle.setPitch(MathHelper.lerp(MathUtil.getRandom(speed, (double)speed + Math.cos((double)MathUtil.getRandom(0.01F, 4.0D))), currentAngle.getPitch(), currentAngle.getPitch() + movePitch));
         return new Angle(moveAngle.getYaw(), moveAngle.getPitch());
      } else {
         return AngleUtil.cameraAngle();
      }
   }

   public Vec3d randomValue() {
      return new Vec3d(0.13D, 0.13D, 0.13D);
   }
}
