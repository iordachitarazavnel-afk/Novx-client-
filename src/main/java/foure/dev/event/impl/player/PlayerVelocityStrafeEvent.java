package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.math.Vec3d;

public class PlayerVelocityStrafeEvent extends Event {
   private final Vec3d movementInput;
   private final float speed;
   private final float yaw;
   private Vec3d velocity;

   @Generated
   public Vec3d getMovementInput() {
      return this.movementInput;
   }

   @Generated
   public float getSpeed() {
      return this.speed;
   }

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public Vec3d getVelocity() {
      return this.velocity;
   }

   @Generated
   public void setVelocity(Vec3d velocity) {
      this.velocity = velocity;
   }

   @Generated
   public PlayerVelocityStrafeEvent(Vec3d movementInput, float speed, float yaw, Vec3d velocity) {
      this.movementInput = movementInput;
      this.speed = speed;
      this.yaw = yaw;
      this.velocity = velocity;
   }
}
