package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.player.CameraEvent;
import foure.dev.event.impl.player.RotationUpdateEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleSmoothMode;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.RotationConfig;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.module.impl.combat.helper.attack.AttackHandler;
import foure.dev.module.impl.combat.helper.attack.AttackPerpetrator;
import foure.dev.module.impl.combat.helper.attack.PointFinder;
import foure.dev.module.impl.combat.helper.attack.TargetSelector;
import foure.dev.module.impl.combat.helper.modes.FunTimeMode;
import foure.dev.module.impl.combat.helper.modes.FunTimeSmoothMode;
import foure.dev.module.impl.combat.helper.modes.LinearSmoothMode;
import foure.dev.module.impl.combat.helper.modes.MatrixSmoothMode;
import foure.dev.module.impl.combat.helper.modes.SnapSmoothMode;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.Script.TaskPriority;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Generated;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Killaura",
   category = Category.COMBAT,
   desc = "Auto Attack"
)
public class Killaura extends Function {
   public NumberSetting getDistance = new NumberSetting("Distance", this, 3.3299999237060547D, 1.0D, 6.0D, 0.10000000149011612D);
   public MultiBoxSetting targetType = new MultiBoxSetting("Target Options", new BooleanSetting[]{new BooleanSetting("Players", true), new BooleanSetting("Mobs", false), new BooleanSetting("Animals", false), new BooleanSetting("Friends", false)});
   public MultiBoxSetting attackSetting = new MultiBoxSetting("Attack Options", new BooleanSetting[]{new BooleanSetting("Allows you to customize the attack", true), new BooleanSetting("Only Critical", true), new BooleanSetting("Dynamic Cooldown", false), new BooleanSetting("Break Shield", true), new BooleanSetting("UnPress Shield", true), new BooleanSetting("No Attack When Eat", true), new BooleanSetting("Ignore The Walls", false)});
   public static ModeSetting correctionType = new ModeSetting("Correction Type", "Free", new String[]{"Free", "Focused"});
   public static ModeSetting aimMode = new ModeSetting("Rotation Type", "Snap", new String[]{"FunTime", "Snap", "Matrix", "FunTime 2", "Sloth"});
   private long lastAttackTime = 0L;
   private double prevPosY;
   private boolean canCritical;
   private long lastEspAttackAnimTime = 0L;
   private int prevSwordSlot = -1;
   public LivingEntity target;
   public LivingEntity lastTarget;
   TargetSelector targetSelector = new TargetSelector();
   Float maxDistance;
   PointFinder pointFinder;

   public Killaura() {
      this.maxDistance = this.getDistance.getValueFloat();
      this.pointFinder = new PointFinder();
      this.addSettings(new Setting[]{this.getDistance, this.targetType, this.attackSetting, correctionType, aimMode});
   }

   public void onDisable() {
      this.targetSelector.releaseTarget();
      this.target = null;
      super.onDisable();
   }

   @Subscribe
   public void onRotationUpdate(RotationUpdateEvent e) {
      this.updateCriticalState();
      switch(e.getType()) {
      case 0:
         this.target = this.updateTarget();
         if (this.target != null) {
            this.rotateToTarget(this.getConfig());
            this.lastTarget = this.target;
         }
         break;
      case 2:
         if (this.target != null) {
            FourEClient.getInstance().getAttackPerpetrator().performAttack(this.getConfig());
         }
      }

   }

   @Subscribe
   public void onCamera(CameraEvent e) {
      if (this.target != null && !correctionType.is("Free")) {
         Angle targetAngle = this.getConfig().getAngle();
         e.setAngle(targetAngle);
      }

   }

   private LivingEntity updateTarget() {
      List<String> selectedNames = (List)this.targetType.getSelectedOptions().stream().map(Setting::getName).collect(Collectors.toList());
      TargetSelector.EntityFilter filter = new TargetSelector.EntityFilter(selectedNames);

      assert mc.world != null;

      if (mc.player.isGliding()) {
         this.targetSelector.searchTargets(mc.world.getEntities(), this.maxDistance * 30.0F, 360.0F, this.attackSetting.is("Ignore The Walls"));
      } else {
         float actualRange = this.maxDistance;
         this.targetSelector.searchTargets(mc.world.getEntities(), actualRange, 360.0F, this.attackSetting.is("Ignore The Walls"));
      }

      TargetSelector var10000 = this.targetSelector;
      Objects.requireNonNull(filter);
      var10000.validateTarget(filter::isValid);
      return this.targetSelector.getCurrentTarget();
   }

   private void rotateToTarget(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      AttackHandler attackHandler = FourEClient.getInstance().getAttackPerpetrator().getAttackHandler();
      RotationController controller = RotationController.INSTANCE;
      Angle targetAngle = config.getAngle();
      Angle finalAngle;
      if (mc.player.getAttackCooldownProgress(0.9F) >= 0.9F) {
         finalAngle = targetAngle;
      } else {
         finalAngle = targetAngle;
      }

      Angle.VecRotation rotation = new Angle.VecRotation(finalAngle, finalAngle.toVector());
      RotationConfig rotationConfig = this.getRotationConfig();
      String var8 = (String)aimMode.getValue();
      byte var9 = -1;
      switch(var8.hashCode()) {
      case -1997372447:
         if (var8.equals("Matrix")) {
            var9 = 4;
         }
         break;
      case 2581482:
         if (var8.equals("Snap")) {
            var9 = 0;
         }
         break;
      case 79980042:
         if (var8.equals("Sloth")) {
            var9 = 1;
         }
         break;
      case 1154553036:
         if (var8.equals("FunTime")) {
            var9 = 2;
         }
         break;
      case 1423906270:
         if (var8.equals("FunTime 2")) {
            var9 = 3;
         }
      }

      switch(var9) {
      case 0:
         if (attackHandler.canAttack(config, 2)) {
            controller.clear();
            controller.rotateTo(rotation, this.target, 3, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         }
         break;
      case 1:
         controller.clear();
         controller.rotateTo(rotation, this.target, 3, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         break;
      case 2:
         if (attackHandler.canAttack(config, 4)) {
            controller.clear();
            controller.rotateTo(rotation, this.target, 40, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         }
         break;
      case 3:
         controller.clear();
         controller.rotateTo(rotation, this.target, 40, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
         break;
      case 4:
         controller.rotateTo(rotation, this.target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
      }

   }

   public AttackPerpetrator.AttackPerpetratorConfigurable getConfig() {
      boolean ignoreWalls = this.attackSetting.is("Ignore The Walls");
      Pair<Vec3d, Box> point = this.pointFinder.computeVector(this.target, this.maxDistance, RotationController.INSTANCE.getRotation(), this.getSmoothMode().randomValue(), ignoreWalls);
      Angle angle = AngleUtil.fromVec3d(((Vec3d)point.getLeft()).subtract(((ClientPlayerEntity)Objects.requireNonNull(mc.player)).getEyePos()));
      Box box = (Box)point.getRight();
      List<String> selectedOptionNames = (List)this.attackSetting.getSelectedOptions().stream().map(Setting::getName).collect(Collectors.toList());
      return new AttackPerpetrator.AttackPerpetratorConfigurable(this.target, angle, this.maxDistance, selectedOptionNames, aimMode, box);
   }

   public RotationConfig getRotationConfig() {
      boolean isFree = correctionType.is("Free");
      return new RotationConfig(this.getSmoothMode(), !isFree, isFree);
   }

   public AngleSmoothMode getSmoothMode() {
      String var1 = (String)aimMode.getValue();
      byte var2 = -1;
      switch(var1.hashCode()) {
      case -1997372447:
         if (var1.equals("Matrix")) {
            var2 = 1;
         }
         break;
      case 2581482:
         if (var1.equals("Snap")) {
            var2 = 2;
         }
         break;
      case 79980042:
         if (var1.equals("Sloth")) {
            var2 = 3;
         }
         break;
      case 1154553036:
         if (var1.equals("FunTime")) {
            var2 = 0;
         }
         break;
      case 1423906270:
         if (var1.equals("FunTime 2")) {
            var2 = 4;
         }
      }

      Object var10000;
      switch(var2) {
      case 0:
         var10000 = new FunTimeSmoothMode();
         break;
      case 1:
         var10000 = new MatrixSmoothMode();
         break;
      case 2:
         var10000 = new SnapSmoothMode();
         break;
      case 3:
         var10000 = new SnapSmoothMode();
         break;
      case 4:
         var10000 = new FunTimeMode();
         break;
      default:
         var10000 = new LinearSmoothMode();
      }

      return (AngleSmoothMode)var10000;
   }

   private void updateCriticalState() {
      if (!fullNullCheck()) {
         double currentPosY = mc.player.getY();
         boolean onGround = mc.player.isOnGround();
         this.canCritical = !onGround && currentPosY < this.prevPosY;
         this.prevPosY = currentPosY;
      }
   }

   private boolean shouldAttack() {
      long currentTime = System.currentTimeMillis();
      boolean isReady = currentTime - this.lastAttackTime >= 220L && mc.player.getAttackCooldownProgress(1.5F) >= 1.0F;
      if (!isReady) {
         return false;
      } else if (this.attackSetting.is("Only Critical")) {
         return !this.shouldCritical() ? false : this.canCritical;
      } else {
         return true;
      }
   }

   private boolean shouldCritical() {
      boolean isDeBuffed = mc.player.hasStatusEffect(StatusEffects.LEVITATION) || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING) || mc.player.hasStatusEffect(StatusEffects.BLINDNESS);
      boolean isInLiquid = mc.player.isSubmergedInWater() || mc.player.isInLava();
      boolean isFlying = mc.player.getAbilities().flying || mc.player.isGliding();
      boolean isClimbing = mc.player.isClimbing();
      boolean isCantJump = mc.player.hasVehicle();
      return !isDeBuffed && !isInLiquid && !isFlying && !isClimbing && !isCantJump;
   }

   @Generated
   public LivingEntity getTarget() {
      return this.target;
   }

   @Generated
   public LivingEntity getLastTarget() {
      return this.lastTarget;
   }
}
