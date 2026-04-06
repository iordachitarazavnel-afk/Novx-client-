package foure.dev.event.impl.input;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;

public class EventKeyboardInput extends Event {
   private float movementForward;
   private float movementSideways;

   public void setYaw(float yaw, float yaw2) {
      float forward = this.getMovementForward();
      float sideways = this.getMovementSideways();
      double angle = MathHelper.wrapDegrees(Math.toDegrees(this.direction(yaw2, (double)forward, (double)sideways)));
      if (forward != 0.0F || sideways != 0.0F) {
         float closestForward = 0.0F;
         float closestSideways = 0.0F;
         float closestDifference = Float.MAX_VALUE;

         for(float predictedForward = -1.0F; predictedForward <= 1.0F; ++predictedForward) {
            for(float predictedSideways = -1.0F; predictedSideways <= 1.0F; ++predictedSideways) {
               if (predictedSideways != 0.0F || predictedForward != 0.0F) {
                  double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(this.direction(yaw, (double)predictedForward, (double)predictedSideways)));
                  double difference = Math.abs(angle - predictedAngle);
                  if (difference < (double)closestDifference) {
                     closestDifference = (float)difference;
                     closestForward = predictedForward;
                     closestSideways = predictedSideways;
                  }
               }
            }
         }

         this.setMovementForward(closestForward);
         this.setMovementSideways(closestSideways);
      }
   }

   private double direction(float yaw, double movementForward, double movementSideways) {
      if (movementForward < 0.0D) {
         yaw += 180.0F;
      }

      float forward = 1.0F;
      if (movementForward < 0.0D) {
         forward = -0.5F;
      } else if (movementForward > 0.0D) {
         forward = 0.5F;
      }

      if (movementSideways > 0.0D) {
         yaw -= 90.0F * forward;
      }

      if (movementSideways < 0.0D) {
         yaw += 90.0F * forward;
      }

      return Math.toRadians((double)yaw);
   }

   @Generated
   public EventKeyboardInput(float movementForward, float movementSideways) {
      this.movementForward = movementForward;
      this.movementSideways = movementSideways;
   }

   @Generated
   public float getMovementForward() {
      return this.movementForward;
   }

   @Generated
   public float getMovementSideways() {
      return this.movementSideways;
   }

   @Generated
   public void setMovementForward(float movementForward) {
      this.movementForward = movementForward;
   }

   @Generated
   public void setMovementSideways(float movementSideways) {
      this.movementSideways = movementSideways;
   }
}
