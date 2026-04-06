package foure.dev.util.character;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;

public class RotateCharacter {
   private final MinecraftClient mc;
   private final Random random = new Random();
   private boolean isRotating = false;
   private float targetYaw;
   private float targetPitch;
   private Runnable onComplete;

   public RotateCharacter(MinecraftClient mc) {
      this.mc = mc;
   }

   public void rotate(float yaw, float pitch, Runnable onComplete) {
      this.targetYaw = yaw;
      this.targetPitch = pitch;
      this.onComplete = onComplete;
      this.isRotating = true;
   }

   public void update(boolean human, boolean fast) {
      if (this.isRotating && this.mc.player != null) {
         float currentYaw = this.mc.player.getYaw();
         float currentPitch = this.mc.player.getPitch();
         float yawDiff = MathHelper.wrapDegrees(this.targetYaw - currentYaw);
         float pitchDiff = this.targetPitch - currentPitch;
         float speed = 1.0F;
         if (fast) {
            speed = 1.5F;
         }

         if (human) {
            speed += (this.random.nextFloat() - 0.5F) * 0.2F;
         }

         float newYaw = currentYaw + MathHelper.clamp(yawDiff, -speed, speed);
         float newPitch = currentPitch + MathHelper.clamp(pitchDiff, -speed, speed);
         this.mc.player.setYaw(newYaw);
         this.mc.player.setPitch(MathHelper.clamp(newPitch, -90.0F, 90.0F));
         if (Math.abs(yawDiff) < 0.1F && Math.abs(pitchDiff) < 0.1F) {
            this.mc.player.setYaw(this.targetYaw);
            this.mc.player.setPitch(MathHelper.clamp(this.targetPitch, -90.0F, 90.0F));
            this.isRotating = false;
            if (this.onComplete != null) {
               this.onComplete.run();
            }
         }

      }
   }

   public boolean isActive() {
      return this.isRotating;
   }
}
