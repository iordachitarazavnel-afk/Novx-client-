package foure.dev.util.character;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class CenterCharacter {
   private static final double TOLERANCE = 0.15D;
   private final MinecraftClient mc;
   private boolean isActive = false;
   private BlockPos targetBlock = null;
   private int tickCount = 0;

   public CenterCharacter(MinecraftClient mc) {
      this.mc = mc;
   }

   public boolean initiate() {
      if (this.mc.player == null) {
         return false;
      } else {
         BlockPos playerBlock = this.mc.player.getBlockPos();
         Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         double offsetX = playerPos.x - ((double)playerBlock.getX() + 0.5D);
         double offsetZ = playerPos.z - ((double)playerBlock.getZ() + 0.5D);
         if (Math.abs(offsetX) < 0.15D && Math.abs(offsetZ) < 0.15D) {
            return false;
         } else {
            this.targetBlock = playerBlock;
            this.isActive = true;
            this.tickCount = 0;
            return true;
         }
      }
   }

   public boolean update() {
      if (this.isActive && this.mc.player != null && this.targetBlock != null) {
         ++this.tickCount;
         Vec3d playerPos = new Vec3d(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
         double targetX = (double)this.targetBlock.getX() + 0.5D;
         double targetZ = (double)this.targetBlock.getZ() + 0.5D;
         double worldOffsetX = playerPos.x - targetX;
         double worldOffsetZ = playerPos.z - targetZ;
         if (Math.abs(worldOffsetX) < 0.15D && Math.abs(worldOffsetZ) < 0.15D) {
            this.haltCentering();
            return false;
         } else {
            this.stopMovement();
            boolean shouldTap = this.tickCount % 2 == 0;
            if (!shouldTap) {
               return true;
            } else {
               float yaw = this.mc.player.getYaw();
               double yawRad = Math.toRadians((double)yaw);
               double moveX = -worldOffsetX;
               double moveZ = -worldOffsetZ;
               double relativeForward = moveX * -Math.sin(yawRad) + moveZ * Math.cos(yawRad);
               double relativeStrafe = moveX * -Math.cos(yawRad) + moveZ * -Math.sin(yawRad);
               if (Math.abs(relativeForward) > 0.075D) {
                  if (relativeForward > 0.0D) {
                     this.mc.options.forwardKey.setPressed(true);
                  } else {
                     this.mc.options.backKey.setPressed(true);
                  }
               }

               if (Math.abs(relativeStrafe) > 0.075D) {
                  if (relativeStrafe > 0.0D) {
                     this.mc.options.rightKey.setPressed(true);
                  } else {
                     this.mc.options.leftKey.setPressed(true);
                  }
               }

               if (this.tickCount > 100) {
                  this.haltCentering();
                  return false;
               } else {
                  return true;
               }
            }
         }
      } else {
         return false;
      }
   }

   public void haltCentering() {
      this.isActive = false;
      this.targetBlock = null;
      this.stopMovement();
      this.tickCount = 0;
   }

   private void stopMovement() {
      this.mc.options.forwardKey.setPressed(false);
      this.mc.options.backKey.setPressed(false);
      this.mc.options.leftKey.setPressed(false);
      this.mc.options.rightKey.setPressed(false);
      this.mc.options.sneakKey.setPressed(false);
   }

   public boolean isCentering() {
      return this.isActive;
   }
}
