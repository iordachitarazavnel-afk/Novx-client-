package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.CameraPositionEvent;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.KeyEvent;
import foure.dev.event.impl.player.CameraEvent;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.mixin.accessor.MinecraftClientAccessor;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.math.MathUtil;
import net.minecraft.client.option.Perspective;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;

@ModuleInfo(
   name = "Freecam",
   category = Category.MOVEMENT,
   desc = "Move freely"
)
public class Freecam extends Function {
   public final Vector3d currentPosition = new Vector3d();
   public final Vector3d previousPosition = new Vector3d();
   private final NumberSetting speed = new NumberSetting("Speed", this, 5.0D, 1.0D, 20.0D, 0.1D);
   private float yaw;
   private float pitch;
   private float prevYaw;
   private float prevPitch;
   private Perspective currentPerspective;
   private boolean isMovingForward;
   private boolean isMovingBackward;
   private boolean isMovingRight;
   private boolean isMovingLeft;
   private boolean isMovingUp;
   private boolean isMovingDown;
   private long lastFrameTime;
   private Double oldGamma;
   private double velX;
   private double velY;
   private double velZ;

   public Freecam() {
      this.addSettings(new Setting[]{this.speed});
   }

   public void onEnable() {
      super.onEnable();
      if (mc.player != null && mc.world != null && mc.options != null) {
         this.oldGamma = (Double)mc.options.getGamma().getValue();
         mc.options.getGamma().setValue(1000.0D);
         mc.options.getFovEffectScale().setValue(0.0D);
         mc.options.getBobView().setValue(false);
         ((MinecraftClientAccessor)mc).setChunkCullingEnabled(false);
         this.yaw = mc.player.getYaw();
         this.pitch = mc.player.getPitch();
         this.prevYaw = this.yaw;
         this.prevPitch = this.pitch;
         this.currentPerspective = mc.options.getPerspective();
         Vec3d eyePos = mc.player.getEyePos();
         this.currentPosition.set(eyePos.x, eyePos.y, eyePos.z);
         this.previousPosition.set(eyePos.x, eyePos.y, eyePos.z);
         mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
         this.isMovingForward = mc.options.forwardKey.isPressed();
         this.isMovingBackward = mc.options.backKey.isPressed();
         this.isMovingRight = mc.options.rightKey.isPressed();
         this.isMovingLeft = mc.options.leftKey.isPressed();
         this.isMovingUp = mc.options.jumpKey.isPressed();
         this.isMovingDown = mc.options.sneakKey.isPressed();
         this.velX = 0.0D;
         this.velY = 0.0D;
         this.velZ = 0.0D;
         this.lastFrameTime = System.currentTimeMillis();
         this.resetMovementKeys();
         if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
         }

      } else {
         this.toggle();
      }
   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null) {
         this.resetMovementKeys();
         if (this.currentPerspective != null && mc.options != null) {
            mc.options.setPerspective(this.currentPerspective);
         }

         if (this.oldGamma != null && mc.options != null) {
            mc.options.getGamma().setValue(this.oldGamma);
         }

         ((MinecraftClientAccessor)mc).setChunkCullingEnabled(true);
         if (mc.worldRenderer != null) {
            mc.worldRenderer.reload();
         }

      }
   }

   private void resetMovementKeys() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
      }
   }

   @Subscribe
   public void onTick(EventUpdate event) {
      if (mc.player != null) {
         if (mc.getCameraEntity() != null) {
            mc.getCameraEntity().noClip = true;
         }

         this.resetMovementKeys();
      }
   }

   @Subscribe
   public void onRender(Render3DEvent event) {
      this.previousPosition.set(this.currentPosition);
      long currentTime = System.currentTimeMillis();
      float deltaTime = (float)(currentTime - this.lastFrameTime) / 1000.0F;
      this.lastFrameTime = currentTime;
      if (deltaTime < 0.001F) {
         deltaTime = 0.001F;
      }

      if (deltaTime > 0.1F) {
         deltaTime = 0.1F;
      }

      this.yaw = mc.player.getYaw();
      this.pitch = mc.player.getPitch();
      Vec3d forward = Vec3d.fromPolar(0.0F, this.yaw);
      Vec3d right = Vec3d.fromPolar(0.0F, this.yaw + 90.0F);
      double targetSpeed = (Double)this.speed.getValue() * 2.0D * (mc.options.sprintKey.isPressed() ? 2.0D : 1.0D);
      double targetX = 0.0D;
      double targetY = 0.0D;
      double targetZ = 0.0D;
      if (this.isMovingForward) {
         targetX += forward.x;
         targetZ += forward.z;
      }

      if (this.isMovingBackward) {
         targetX -= forward.x;
         targetZ -= forward.z;
      }

      if (this.isMovingRight) {
         targetX += right.x;
         targetZ += right.z;
      }

      if (this.isMovingLeft) {
         targetX -= right.x;
         targetZ -= right.z;
      }

      if (this.isMovingUp) {
         ++targetY;
      }

      if (this.isMovingDown) {
         --targetY;
      }

      if (targetX == 0.0D && targetZ != 0.0D) {
      }

      targetX *= targetSpeed;
      targetY *= targetSpeed;
      targetZ *= targetSpeed;
      double friction = 10.0D;
      double lerpFactor = friction * (double)deltaTime;
      if (lerpFactor > 1.0D) {
         lerpFactor = 1.0D;
      }

      this.velX = MathHelper.lerp(lerpFactor, this.velX, targetX);
      this.velY = MathHelper.lerp(lerpFactor, this.velY, targetY);
      this.velZ = MathHelper.lerp(lerpFactor, this.velZ, targetZ);
      Vector3d var10000 = this.currentPosition;
      var10000.x += this.velX * (double)deltaTime * 5.0D;
      var10000 = this.currentPosition;
      var10000.y += this.velY * (double)deltaTime * 5.0D;
      var10000 = this.currentPosition;
      var10000.z += this.velZ * (double)deltaTime * 5.0D;
   }

   @Subscribe
   public void onCameraPos(CameraPositionEvent event) {
      float tickDelta = MathUtil.getTickDelta();
      event.setPos(new Vec3d(MathHelper.lerp((double)tickDelta, this.previousPosition.x, this.currentPosition.x), MathHelper.lerp((double)tickDelta, this.previousPosition.y, this.currentPosition.y), MathHelper.lerp((double)tickDelta, this.previousPosition.z, this.currentPosition.z)));
   }

   @Subscribe
   public void onCameraRot(CameraEvent event) {
      event.setAngle(new Angle(mc.player.getYaw(), mc.player.getPitch()));
   }

   @Subscribe
   public void onKey(KeyEvent e) {
      if (mc.options != null) {
         if (e.getKey() != 292) {
            boolean handled = true;
            int action = e.getAction();
            boolean press = action != 0;
            int key = e.getKey();
            if (key == mc.options.forwardKey.getDefaultKey().getCode()) {
               this.isMovingForward = press;
               mc.options.forwardKey.setPressed(false);
            } else if (key == mc.options.backKey.getDefaultKey().getCode()) {
               this.isMovingBackward = press;
               mc.options.backKey.setPressed(false);
            } else if (key == mc.options.rightKey.getDefaultKey().getCode()) {
               this.isMovingRight = press;
               mc.options.rightKey.setPressed(false);
            } else if (key == mc.options.leftKey.getDefaultKey().getCode()) {
               this.isMovingLeft = press;
               mc.options.leftKey.setPressed(false);
            } else if (key == mc.options.jumpKey.getDefaultKey().getCode()) {
               this.isMovingUp = press;
               mc.options.jumpKey.setPressed(false);
            } else if (key == mc.options.sneakKey.getDefaultKey().getCode()) {
               this.isMovingDown = press;
               mc.options.sneakKey.setPressed(false);
            } else {
               handled = false;
            }

            if (handled) {
               e.cancel();
            }

         }
      }
   }
}
