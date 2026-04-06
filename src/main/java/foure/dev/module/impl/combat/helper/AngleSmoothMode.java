package foure.dev.module.impl.combat.helper;

import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public abstract class AngleSmoothMode implements Wrapper {
   private final String name;

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle) {
      return this.limitAngleChange(currentAngle, targetAngle, (Vec3d)null, (Entity)null);
   }

   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d) {
      return this.limitAngleChange(currentAngle, targetAngle, vec3d, (Entity)null);
   }

   public abstract Angle limitAngleChange(Angle var1, Angle var2, Vec3d var3, Entity var4);

   public abstract Vec3d randomValue();

   @Generated
   public String getName() {
      return this.name;
   }

   @Generated
   public AngleSmoothMode(String name) {
      this.name = name;
   }
}
