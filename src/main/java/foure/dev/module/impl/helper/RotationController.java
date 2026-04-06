package foure.dev.module.impl.combat.helper;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.player.PlayerVelocityStrafeEvent;
import foure.dev.event.impl.player.RotationUpdateEvent;
import foure.dev.module.api.Function;
import foure.dev.util.Script.TaskPriority;
import foure.dev.util.Script.TaskProcessor;
import foure.dev.util.wrapper.Wrapper;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationController implements Wrapper {
   public static RotationController INSTANCE = new RotationController();
   private RotationPlan lastRotationPlan;
   private final TaskProcessor<RotationPlan> rotationPlanTaskProcessor = new TaskProcessor();
   private Angle currentAngle;
   private Angle previousAngle;
   private Angle serverAngle;

   public RotationController() {
      this.serverAngle = Angle.DEFAULT;
      FourEClient.getInstance().getEventBus().register(this);
   }

   public void setRotation(Angle value) {
      if (value == null) {
         this.previousAngle = this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
      } else {
         this.previousAngle = this.currentAngle;
      }

      this.currentAngle = value;
   }

   public Angle getRotation() {
      return this.currentAngle != null ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public Angle getPreviousRotation() {
      return this.currentAngle != null && this.previousAngle != null ? this.previousAngle : new Angle(mc.player.lastYaw, mc.player.lastPitch);
   }

   public Angle getMoveRotation() {
      RotationPlan rotationPlan = this.getCurrentRotationPlan();
      return this.currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? this.currentAngle : AngleUtil.cameraAngle();
   }

   public RotationPlan getCurrentRotationPlan() {
      return this.rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? (RotationPlan)this.rotationPlanTaskProcessor.fetchActiveTaskValue() : this.lastRotationPlan;
   }

   public void rotateTo(Angle.VecRotation vecRotation, LivingEntity entity, int reset, RotationConfig configurable, TaskPriority taskPriority, Function provider) {
      this.rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, int reset, RotationConfig configurable, TaskPriority taskPriority, Function provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), (Entity)null, reset), taskPriority, provider);
   }

   public void rotateTo(Angle angle, RotationConfig configurable, TaskPriority taskPriority, Function provider) {
      this.rotateTo(configurable.createRotationPlan(angle, angle.toVector(), (Entity)null, 1), taskPriority, provider);
   }

   public void rotateTo(RotationPlan plan, TaskPriority taskPriority, Function provider) {
      this.rotationPlanTaskProcessor.addTask(new TaskProcessor.Task(1, taskPriority.getPriority(), provider, plan));
   }

   public void update() {
      RotationPlan activePlan = this.getCurrentRotationPlan();
      if (activePlan != null) {
         if (mc.player != null && mc.world != null) {
            Angle clientAngle = AngleUtil.cameraAngle();
            if (this.lastRotationPlan != null) {
               double differenceFromCurrentToPlayer = computeRotationDifference(this.serverAngle, clientAngle);
               if (activePlan.getTicksUntilReset() <= this.rotationPlanTaskProcessor.tickCounter && differenceFromCurrentToPlayer < (double)activePlan.getResetThreshold()) {
                  this.setRotation((Angle)null);
                  this.lastRotationPlan = null;
                  this.rotationPlanTaskProcessor.tickCounter = 0;
                  return;
               }
            }

            Angle newAngle = activePlan.nextRotation(this.currentAngle != null ? this.currentAngle : clientAngle, this.rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
            this.setRotation(newAngle);
            this.lastRotationPlan = activePlan;
            this.rotationPlanTaskProcessor.tick(1);
         }
      }
   }

   public static double computeRotationDifference(Angle a, Angle b) {
      return Math.hypot((double)Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), (double)Math.abs(a.getPitch() - b.getPitch()));
   }

   public static float computeAngleDifference(float a, float b) {
      return MathHelper.wrapDegrees(a - b);
   }

   private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
      if (this.currentAngle != null) {
         float yaw = this.currentAngle.getYaw();
         double d = movementInput.lengthSquared();
         if (d < 1.0E-7D) {
            return Vec3d.ZERO;
         } else {
            Vec3d vec3d = (d > 1.0D ? movementInput.normalize() : movementInput).multiply((double)speed);
            float f = MathHelper.sin((double)(yaw * 0.017453292F));
            float g = MathHelper.cos((double)(yaw * 0.017453292F));
            return new Vec3d(vec3d.getX() * (double)g - vec3d.getZ() * (double)f, vec3d.getY(), vec3d.getZ() * (double)g + vec3d.getX() * (double)f);
         }
      } else {
         return currVelocity;
      }
   }

   public void clear() {
      this.rotationPlanTaskProcessor.activeTasks.clear();
   }

   @Subscribe
   public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
      RotationPlan currentRotationPlan = this.getCurrentRotationPlan();
      if (currentRotationPlan != null && currentRotationPlan.isMoveCorrection()) {
         e.setVelocity(this.fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
      }

   }

   @Subscribe
   public void onTick(EventUpdate e) {
      FourEClient.getInstance().getEventBus().post(new RotationUpdateEvent((byte)0));
      this.update();
      FourEClient.getInstance().getEventBus().post(new RotationUpdateEvent((byte)2));
   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      if (!event.isCanceled()) {
         Packet packet = event.getPacket();

         if (packet instanceof PlayerMoveC2SPacket player) {
            if (player.changesLook()) {
               this.serverAngle = new Angle(player.getYaw(1.0F), player.getPitch(1.0F));
            }
         } else if (packet instanceof PlayerPositionLookS2CPacket player) {
            this.serverAngle = new Angle(player.change().yaw(), player.change().pitch());
         }
      }
   }

   @Generated
   public RotationPlan getLastRotationPlan() {
      return this.lastRotationPlan;
   }

   @Generated
   public TaskProcessor<RotationPlan> getRotationPlanTaskProcessor() {
      return this.rotationPlanTaskProcessor;
   }

   @Generated
   public Angle getCurrentAngle() {
      return this.currentAngle;
   }

   @Generated
   public Angle getPreviousAngle() {
      return this.previousAngle;
   }

   @Generated
   public Angle getServerAngle() {
      return this.serverAngle;
   }
}
