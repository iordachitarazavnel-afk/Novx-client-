package foure.dev.module.impl.combat.helper;

import foure.dev.module.impl.combat.helper.modes.LinearSmoothMode;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class RotationConfig {
   public static RotationConfig DEFAULT = new RotationConfig(new LinearSmoothMode(), true, true);
   private final boolean moveCorrection;
   private final boolean freeCorrection;
   private final AngleSmoothMode angleSmooth;
   private final int resetThreshold;

   public RotationConfig(boolean moveCorrection, boolean freeCorrection) {
      this(new LinearSmoothMode(), moveCorrection, freeCorrection);
   }

   public RotationConfig(boolean moveCorrection) {
      this(new LinearSmoothMode(), moveCorrection, true);
   }

   public RotationConfig(AngleSmoothMode angleSmooth, boolean moveCorrection, boolean freeCorrection) {
      this.resetThreshold = 3;
      this.angleSmooth = angleSmooth;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, int reset) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, reset, 3.0F, this.moveCorrection, this.freeCorrection);
   }

   public RotationPlan createRotationPlan(Angle angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
      return new RotationPlan(angle, vec, entity, this.angleSmooth, 1, 3.0F, moveCorrection, freeCorrection);
   }
}
