package foure.dev.module.impl.combat.helper.attack;

import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.module.impl.combat.helper.modes.LinearSmoothMode;
import foure.dev.util.Player.RaytracingUtil;
import foure.dev.util.wrapper.Wrapper;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public class TargetSelector implements Wrapper {
   private final PointFinder pointFinder = new PointFinder();
   private LivingEntity currentTarget = null;
   private Stream<LivingEntity> potentialTargets;

   public void lockTarget(LivingEntity target) {
      if (this.currentTarget == null) {
         this.currentTarget = target;
      }

   }

   public void releaseTarget() {
      this.currentTarget = null;
   }

   public void validateTarget(Predicate<LivingEntity> predicate) {
      this.findFirstMatch(predicate).ifPresent(this::lockTarget);
      if (this.currentTarget != null && !predicate.test(this.currentTarget)) {
         this.releaseTarget();
      }

   }

   public void searchTargets(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      if (this.currentTarget != null && (!this.pointFinder.hasValidPoint(this.currentTarget, maxDistance, ignoreWalls) || this.getFov(this.currentTarget, maxDistance, ignoreWalls) > (double)maxFov)) {
         this.releaseTarget();
      }

      this.potentialTargets = this.createStreamFromEntities(entities, maxDistance, maxFov, ignoreWalls);
   }

   private double getFov(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Vec3d attackVector = (Vec3d)this.pointFinder.computeVector(entity, maxDistance, RotationController.INSTANCE.getRotation(), (new LinearSmoothMode()).randomValue(), ignoreWalls).getLeft();
      return RaytracingUtil.rayTrace((double)maxDistance, entity.getBoundingBox()) ? 0.0D : RotationController.computeRotationDifference(AngleUtil.cameraAngle(), AngleUtil.calculateAngle(attackVector));
   }

   private Stream<LivingEntity> createStreamFromEntities(Iterable<Entity> entities, float maxDistance, float maxFov, boolean ignoreWalls) {
      return StreamSupport.stream(entities.spliterator(), false)
              .filter(LivingEntity.class::isInstance)
              .map(LivingEntity.class::cast)
              .filter(entity -> {
                 return this.pointFinder.hasValidPoint(entity, maxDistance, ignoreWalls)
                         && this.getFov(entity, maxDistance, ignoreWalls) < (double) maxFov;
              })
              .sorted(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(mc.player)));
   }

   private Optional<LivingEntity> findFirstMatch(Predicate<LivingEntity> predicate) {
      return this.potentialTargets.filter(predicate).findFirst();
   }

   @Generated
   public PointFinder getPointFinder() {
      return this.pointFinder;
   }

   @Generated
   public LivingEntity getCurrentTarget() {
      return this.currentTarget;
   }

   @Generated
   public Stream<LivingEntity> getPotentialTargets() {
      return this.potentialTargets;
   }

   public static class EntityFilter {
      private final List<String> targetSettings;

      public boolean isValid(LivingEntity entity) {
         if (this.isLocalPlayer(entity)) {
            return false;
         } else {
            return this.isInvalidHealth(entity) ? false : this.isValidEntityType(entity);
         }
      }

      private boolean isLocalPlayer(LivingEntity entity) {
         return entity == Wrapper.mc.player;
      }

      private boolean isInvalidHealth(LivingEntity entity) {
         return !entity.isAlive() || entity.getHealth() <= 0.0F;
      }

      private boolean isNakedPlayer(LivingEntity entity) {
         return entity.isPlayer();
      }

      private boolean isValidEntityType(LivingEntity entity) {
         if (entity instanceof PlayerEntity player) {
            // If "Friends" is NOT allowed, skip players entirely
            if (!this.targetSettings.contains("Friends")) {
               return false;
            }
            return this.targetSettings.contains("Players");

         } else if (entity instanceof AnimalEntity) {
            return this.targetSettings.contains("Animals");

         } else if (entity instanceof MobEntity) {
            return this.targetSettings.contains("Mobs");
         }

         return false;
      }

      @Generated
      public EntityFilter(List<String> targetSettings) {
         this.targetSettings = targetSettings;
      }
   }
}
