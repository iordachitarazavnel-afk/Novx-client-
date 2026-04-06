package foure.dev.module.impl.combat.helper;

import foure.dev.util.math.MathUtil;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Angle {
   public static Angle DEFAULT = new Angle(0.0F, 0.0F);
   private float yaw;
   private float pitch;

   public Angle adjustSensitivity() {
      double gcd = MathUtil.computeGcd();
      Angle previousAngle = RotationController.INSTANCE.getServerAngle();
      float adjustedYaw = this.adjustAxis(this.yaw, previousAngle.yaw, gcd);
      float adjustedPitch = this.adjustAxis(this.pitch, previousAngle.pitch, gcd);
      return new Angle(adjustedYaw, MathHelper.clamp(adjustedPitch, -90.0F, 90.0F));
   }

   public Angle random(float f) {
      return new Angle(this.yaw + MathUtil.getRandom(-f, (double)f), this.pitch + MathUtil.getRandom(-f, (double)f));
   }

   private float adjustAxis(float axisValue, float previousValue, double gcd) {
      float delta = axisValue - previousValue;
      return previousValue + (float)Math.round((double)delta / gcd) * (float)gcd;
   }

   public final Vec3d toVector() {
      float f = this.pitch * 0.017453292F;
      float g = -this.yaw * 0.017453292F;
      float h = MathHelper.cos((double)g);
      float i = MathHelper.sin((double)g);
      float j = MathHelper.cos((double)f);
      float k = MathHelper.sin((double)f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }

   public Angle addYaw(float yaw) {
      return new Angle(this.yaw + yaw, this.pitch);
   }

   public Angle addPitch(float pitch) {
      this.pitch = MathHelper.clamp(this.pitch + pitch, -90.0F, 90.0F);
      return this;
   }

   public Angle of(Angle angle) {
      return new Angle(angle.getYaw(), angle.getPitch());
   }

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }

   @Generated
   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Generated
   public String toString() {
      float var10000 = this.getYaw();
      return "Angle(yaw=" + var10000 + ", pitch=" + this.getPitch() + ")";
   }

   @Generated
   public Angle(float yaw, float pitch) {
      this.yaw = yaw;
      this.pitch = pitch;
   }

   public static class VecRotation {
      private final Angle angle;
      private final Vec3d vec;

      @Generated
      public String toString() {
         String var10000 = String.valueOf(this.getAngle());
         return "Angle.VecRotation(angle=" + var10000 + ", vec=" + String.valueOf(this.getVec()) + ")";
      }

      @Generated
      public Angle getAngle() {
         return this.angle;
      }

      @Generated
      public Vec3d getVec() {
         return this.vec;
      }

      @Generated
      public VecRotation(Angle angle, Vec3d vec) {
         this.angle = angle;
         this.vec = vec;
      }
   }
}
