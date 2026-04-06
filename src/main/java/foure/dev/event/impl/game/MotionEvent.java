package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;

public class MotionEvent extends Event {
   private double x;
   private double y;
   private double z;
   private float yaw;
   private float pitch;
   private boolean onGround;

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MotionEvent)) {
         return false;
      } else {
         MotionEvent other = (MotionEvent)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else if (Double.compare(this.getX(), other.getX()) != 0) {
            return false;
         } else if (Double.compare(this.getY(), other.getY()) != 0) {
            return false;
         } else if (Double.compare(this.getZ(), other.getZ()) != 0) {
            return false;
         } else if (Float.compare(this.getYaw(), other.getYaw()) != 0) {
            return false;
         } else if (Float.compare(this.getPitch(), other.getPitch()) != 0) {
            return false;
         } else {
            return this.isOnGround() == other.isOnGround();
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof MotionEvent;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = super.hashCode();
      long $x = Double.doubleToLongBits(this.getX());
      result = result * 59 + (int)($x >>> 32 ^ $x);
      long $y = Double.doubleToLongBits(this.getY());
      result = result * 59 + (int)($y >>> 32 ^ $y);
      long $z = Double.doubleToLongBits(this.getZ());
      result = result * 59 + (int)($z >>> 32 ^ $z);
      result = result * 59 + Float.floatToIntBits(this.getYaw());
      result = result * 59 + Float.floatToIntBits(this.getPitch());
      result = result * 59 + (this.isOnGround() ? 79 : 97);
      return result;
   }

   @Generated
   public double getX() {
      return this.x;
   }

   @Generated
   public double getY() {
      return this.y;
   }

   @Generated
   public double getZ() {
      return this.z;
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
   public boolean isOnGround() {
      return this.onGround;
   }

   @Generated
   public void setX(double x) {
      this.x = x;
   }

   @Generated
   public void setY(double y) {
      this.y = y;
   }

   @Generated
   public void setZ(double z) {
      this.z = z;
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
   public void setOnGround(boolean onGround) {
      this.onGround = onGround;
   }

   @Generated
   public String toString() {
      double var10000 = this.getX();
      return "MotionEvent(x=" + var10000 + ", y=" + this.getY() + ", z=" + this.getZ() + ", yaw=" + this.getYaw() + ", pitch=" + this.getPitch() + ", onGround=" + this.isOnGround() + ")";
   }

   @Generated
   public MotionEvent(double x, double y, double z, float yaw, float pitch, boolean onGround) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.onGround = onGround;
   }
}
