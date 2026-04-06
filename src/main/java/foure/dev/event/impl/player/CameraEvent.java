package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import foure.dev.module.impl.combat.helper.Angle;
import lombok.Generated;

public class CameraEvent extends Event {
   private boolean cameraClip;
   private float getDistance;
   private Angle angle;

   @Generated
   public boolean isCameraClip() {
      return this.cameraClip;
   }

   @Generated
   public float getDistance() {
      return this.getDistance;
   }

   @Generated
   public Angle getAngle() {
      return this.angle;
   }

   @Generated
   public void setCameraClip(boolean cameraClip) {
      this.cameraClip = cameraClip;
   }

   @Generated
   public void setDistance(float distance) {
      this.getDistance = distance;
   }

   @Generated
   public void setAngle(Angle angle) {
      this.angle = angle;
   }

   @Generated
   public CameraEvent(boolean cameraClip, float distance, Angle angle) {
      this.cameraClip = cameraClip;
      this.getDistance = distance;
      this.angle = angle;
   }
}
