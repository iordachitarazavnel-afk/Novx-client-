package foure.dev.util.Player;

import foure.dev.util.wrapper.Wrapper;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

public final class MobilityHandler implements Wrapper {
   public static boolean isMoving() {
      Vec2f inputVector = mc.player.input.movementVector;
      float forward = inputVector.y;
      float strafe = inputVector.x;
      return (double)forward != 0.0D || (double)strafe != 0.0D;
   }

   public static double getSpeed() {
      return Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
   }

   public static Vec3d getRotationVector(float pitch, float yaw) {
      float f = pitch * 0.017453292F;
      float g = -yaw * 0.017453292F;
      float h = MathHelper.cos((double)g);
      float i = MathHelper.sin((double)g);
      float j = MathHelper.cos((double)f);
      float k = MathHelper.sin((double)f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }

   public static boolean hasPlayerMovement() {
      Vec2f inputVector = mc.player.input.movementVector;
      float forward = inputVector.y;
      float strafe = inputVector.x;
      return forward != 0.0F || strafe != 0.0F;
   }

   public static double[] calculateDirection(double distance) {
      Vec2f inputVector = mc.player.input.movementVector;
      float forward = inputVector.y;
      float strafe = inputVector.x;
      return calculateDirection(forward, strafe, distance);
   }

   public static double[] calculateDirection(float forward, float sideways, double distance) {
      float yaw = mc.player.getYaw();
      if (forward != 0.0F) {
         if (sideways > 0.0F) {
            yaw += forward > 0.0F ? -45.0F : 45.0F;
         } else if (sideways < 0.0F) {
            yaw += forward > 0.0F ? 45.0F : -45.0F;
         }

         sideways = 0.0F;
         forward = forward > 0.0F ? 1.0F : -1.0F;
      }

      double sinYaw = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
      double cosYaw = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
      double xMovement = (double)forward * distance * cosYaw + (double)sideways * distance * sinYaw;
      double zMovement = (double)forward * distance * sinYaw - (double)sideways * distance * cosYaw;
      return new double[]{xMovement, zMovement};
   }

   public static double getSpeedSqrt(Entity entity) {
      return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
   }

   public static void setVelocity(double velocity) {
      double[] direction = calculateDirection(velocity);
      ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).setVelocity(direction[0], mc.player.getVelocity().getY(), direction[1]);
   }

   public static void setVelocity(double velocity, double y) {
      double[] direction = calculateDirection(velocity);
      ((ClientPlayerEntity)Objects.requireNonNull(mc.player)).setVelocity(direction[0], y, direction[1]);
   }

   public static double getDegreesRelativeToView(Vec3d positionRelativeToPlayer, float yaw) {
      float optimalYaw = (float)Math.atan2(-positionRelativeToPlayer.x, positionRelativeToPlayer.z);
      double currentYaw = Math.toRadians((double)MathHelper.wrapDegrees(yaw));
      return Math.toDegrees(MathHelper.wrapDegrees((double)optimalYaw - currentYaw));
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs, float deadAngle) {
      boolean forwards = input.forward();
      boolean backwards = input.backward();
      boolean left = input.left();
      boolean right = input.right();
      if (dgs >= (double)(-90.0F + deadAngle) && dgs <= (double)(90.0F - deadAngle)) {
         forwards = true;
      } else if (dgs < (double)(-90.0F - deadAngle) || dgs > (double)(90.0F + deadAngle)) {
         backwards = true;
      }

      if (dgs >= (double)(0.0F + deadAngle) && dgs <= (double)(180.0F - deadAngle)) {
         right = true;
      } else if (dgs >= (double)(-180.0F + deadAngle) && dgs <= (double)(0.0F - deadAngle)) {
         left = true;
      }

      return new PlayerInput(forwards, backwards, left, right, input.jump(), input.sneak(), input.sprint());
   }

   public static PlayerInput getDirectionalInputForDegrees(PlayerInput input, double dgs) {
      return getDirectionalInputForDegrees(input, dgs, 20.0F);
   }

   public static double[] forward(double speed) {
      ClientPlayerEntity player = mc.player;
      if (player == null) {
         return new double[]{0.0D, 0.0D};
      } else {
         Vec2f inputVector = player.input.movementVector;
         float forward = inputVector.y;
         float strafe = inputVector.x;
         float yaw = player.getYaw();
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += (float)(forward > 0.0F ? -45 : 45);
            } else if (strafe < 0.0F) {
               yaw += (float)(forward > 0.0F ? 45 : -45);
            }

            strafe = 0.0F;
            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         double sin = Math.sin(Math.toRadians((double)(yaw + 90.0F)));
         double cos = Math.cos(Math.toRadians((double)(yaw + 90.0F)));
         double motionX = (double)forward * speed * cos + (double)strafe * speed * sin;
         double motionZ = (double)forward * speed * sin - (double)strafe * speed * cos;
         return new double[]{motionX, motionZ};
      }
   }

   public static void setMotion(double speed) {
      Vec2f inputVector = mc.player.input.movementVector;
      float forward = inputVector.y;
      float strafe = inputVector.x;
      float yaw = mc.player.getYaw();
      if (forward == 0.0F && strafe == 0.0F) {
         mc.player.setVelocity(0.0D, mc.player.getVelocity().y, 0.0D);
      } else {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += (float)(forward > 0.0F ? -45 : 45);
            } else if (strafe < 0.0F) {
               yaw += (float)(forward > 0.0F ? 45 : -45);
            }

            strafe = 0.0F;
            if (forward > 0.0F) {
               forward = 1.0F;
            } else if (forward < 0.0F) {
               forward = -1.0F;
            }
         }

         double sin = (double)MathHelper.sin((double)((float)Math.toRadians((double)(yaw + 90.0F))));
         double cos = (double)MathHelper.cos((double)((float)Math.toRadians((double)(yaw + 90.0F))));
         mc.player.setVelocity((double)forward * speed * cos + (double)strafe * speed * sin, mc.player.getVelocity().y, (double)forward * speed * sin - (double)strafe * speed * cos);
      }

   }

   public static float getMoveDirection() {
      Vec2f inputVector = mc.player.input.movementVector;
      float forward = inputVector.y;
      float strafe = inputVector.x;
      if (strafe > 0.0F) {
         strafe = 1.0F;
      } else if (strafe < 0.0F) {
         strafe = -1.0F;
      }

      float yaw = mc.player.getYaw();
      if (forward == 0.0F && strafe == 0.0F) {
         return yaw;
      } else {
         if (forward != 0.0F) {
            if (strafe > 0.0F) {
               yaw += forward > 0.0F ? -45.0F : -135.0F;
            } else if (strafe < 0.0F) {
               yaw += forward > 0.0F ? 45.0F : 135.0F;
            } else if (forward < 0.0F) {
               yaw += 180.0F;
            }
         }

         if (forward == 0.0F) {
            if (strafe > 0.0F) {
               yaw -= 90.0F;
            } else if (strafe < 0.0F) {
               yaw += 90.0F;
            }
         }

         return yaw;
      }
   }

   public static double getJumpSpeed() {
      double jumpSpeed = 0.3999999463558197D;
      if (mc.player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
         double amplifier = (double)mc.player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier();
         jumpSpeed += (amplifier + 1.0D) * 0.1D;
      }

      return jumpSpeed;
   }

   public static double getBoost() {
      int[] vectors = new int[]{-45, 45, 135, -135};
      int[] addVectors = new int[]{-90, 90, 180, -180, 0};
      int[] pitchVectors = new int[]{-45, 45};
      float lastYaw = mc.player.lastYaw;
      float lastPitch = mc.player.lastPitch;
      int minDist = findClosestVector(lastYaw, vectors);
      float maxDist = Math.abs(MathHelper.wrapDegrees(lastYaw) - (float)vectors[minDist]);
      int addMinDist = findClosestVector(lastYaw, addVectors);
      float addMaxDist = Math.abs(MathHelper.wrapDegrees(lastYaw) - (float)addVectors[addMinDist]);
      float countableSpeed = minDist == -1 ? 1.5F : 2.06F - maxDist * 0.56F / 45.0F;
      if (addMaxDist < 10.0F) {
         countableSpeed += 0.1F - 0.1F * addMaxDist / 10.0F;
      }

      int pitchMinDist = findClosestVector(lastPitch, pitchVectors);
      float pitchMaxDist = Math.abs(Math.abs(lastPitch) - (float)Math.abs(pitchVectors[pitchMinDist]));
      if (pitchMaxDist < 26.0F) {
         countableSpeed = Math.max(1.94F, countableSpeed);
         countableSpeed += 0.05F - pitchMaxDist * 0.05F / 26.0F;
      }

      countableSpeed = Math.min(2.045F, countableSpeed);
      if (mc.player.lastPitch > -55.0F && mc.player.lastPitch < -19.0F) {
         countableSpeed = 1.91F;
      } else if (mc.player.lastPitch < -55.0F) {
         countableSpeed = 1.54F;
      }

      if (mc.player.lastPitch > 19.0F && mc.player.lastPitch < 55.0F) {
         countableSpeed = 1.8F;
      } else if (mc.player.lastPitch > 55.0F) {
         countableSpeed = 1.54F;
      }

      return (double)countableSpeed;
   }

   private static int findClosestVector(float lastYaw, int[] vectors) {
      int index = 0;
      int minDistIndex = -1;
      float minDist = Float.MAX_VALUE;
      int[] var5 = vectors;
      int var6 = vectors.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         int vector = var5[var7];
         float dist = Math.abs(MathHelper.wrapDegrees(lastYaw) - (float)vector);
         if (dist < minDist) {
            minDist = dist;
            minDistIndex = index;
         }

         ++index;
      }

      return minDistIndex;
   }

   @Generated
   private MobilityHandler() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
