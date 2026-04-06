package foure.dev.ui.notification;

import lombok.Generated;

public class Notification {
   private final String title;
   private final String message;
   private final NotificationType type;
   private final long startTime;
   private final float maxTime;
   private float animationProgress = 0.0F;
   private boolean isExiting = false;
   private static final float ANIM_SPEED = 0.03F;

   public Notification(String title, String message, NotificationType type) {
      this.title = title;
      this.message = message;
      this.type = type;
      this.startTime = System.currentTimeMillis();
      this.maxTime = 900.0F;
   }

   public void update() {
      long timeAlive = System.currentTimeMillis() - this.startTime;
      if ((float)timeAlive > this.maxTime) {
         this.isExiting = true;
      }

      if (this.isExiting) {
         this.animationProgress -= 0.03F;
      } else {
         this.animationProgress += 0.03F;
      }

      if (this.animationProgress > 1.0F) {
         this.animationProgress = 1.0F;
      }

      if (this.animationProgress < 0.0F) {
         this.animationProgress = 0.0F;
      }

   }

   public void forceExit() {
      this.isExiting = true;
   }

   public boolean shouldRemove() {
      return this.isExiting && this.animationProgress <= 0.0F;
   }

   @Generated
   public String getTitle() {
      return this.title;
   }

   @Generated
   public String getMessage() {
      return this.message;
   }

   @Generated
   public NotificationType getType() {
      return this.type;
   }

   @Generated
   public long getStartTime() {
      return this.startTime;
   }

   @Generated
   public float getMaxTime() {
      return this.maxTime;
   }

   @Generated
   public float getAnimationProgress() {
      return this.animationProgress;
   }

   @Generated
   public boolean isExiting() {
      return this.isExiting;
   }
}
