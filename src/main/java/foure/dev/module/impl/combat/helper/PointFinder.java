package foure.dev.module.impl.combat.helper;

import foure.dev.util.Player.RaytracingUtil;
import foure.dev.util.wrapper.Wrapper;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext.ShapeType;

public class PointFinder implements Wrapper {
   private final Random random = new SecureRandom();
   private Vec3d offset;

   public PointFinder() {
      this.offset = Vec3d.ZERO;
   }

   public Pair<Vec3d, Box> computeVector(LivingEntity entity, float maxDistance, Angle initialAngle, Vec3d velocity, boolean ignoreWalls) {
      Pair<List<Vec3d>, Box> candidatePoints = this.generateCandidatePoints(entity, maxDistance, ignoreWalls);
      Vec3d bestVector = this.findBestVector((List)candidatePoints.getLeft(), initialAngle);
      this.updateOffset(velocity);
      return new Pair((bestVector == null ? entity.getEyePos() : bestVector).add(this.offset), (Box)candidatePoints.getRight());
   }

   public Pair<List<Vec3d>, Box> generateCandidatePoints(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box entityBox = entity.getBoundingBox();
      double stepY = entityBox.getLengthY() / 10.0D;
      List<Vec3d> list = Stream.iterate(entityBox.minY, (y) -> {
         return y <= entityBox.maxY;
      }, (y) -> {
         return y + stepY;
      }).map((y) -> {
         return new Vec3d(entityBox.getCenter().x, y, entityBox.getCenter().z);
      }).filter((point) -> {
         return this.isValidPoint(mc.player.getEyePos(), point, maxDistance, ignoreWalls);
      }).toList();
      return new Pair(list, entityBox);
   }

   public boolean hasValidPoint(LivingEntity entity, float maxDistance, boolean ignoreWalls) {
      Box entityBox = entity.getBoundingBox();
      double stepY = entityBox.getLengthY() / 10.0D;
      return Stream.iterate(entityBox.minY, (y) -> {
         return y < entityBox.maxY;
      }, (y) -> {
         return y + stepY;
      }).map((y) -> {
         return new Vec3d(entityBox.getCenter().x, y, entityBox.getCenter().z);
      }).anyMatch((point) -> {
         return this.isValidPoint(mc.player.getEyePos(), point, maxDistance, ignoreWalls);
      });
   }

   private boolean isValidPoint(Vec3d startPoint, Vec3d endPoint, float maxDistance, boolean ignoreWalls) {
      return startPoint.distanceTo(endPoint) <= (double)maxDistance && (ignoreWalls || !RaytracingUtil.raycast(startPoint, endPoint, ShapeType.COLLIDER).getType().equals(Type.BLOCK));
   }

   private Vec3d findBestVector(List<Vec3d> candidatePoints, Angle initialAngle) {
      return (Vec3d)candidatePoints.stream().min(Comparator.comparing((point) -> {
         return this.calculateRotationDifference(mc.player.getEyePos(), point, initialAngle);
      })).orElse((Vec3d) null);
   }

   private double calculateRotationDifference(Vec3d startPoint, Vec3d endPoint, Angle initialAngle) {
      Angle targetAngle = AngleUtil.fromVec3d(endPoint.subtract(startPoint));
      Angle delta = AngleUtil.calculateDelta(initialAngle, targetAngle);
      return Math.hypot((double)delta.getYaw(), (double)delta.getPitch());
   }

   private void updateOffset(Vec3d velocity) {
      this.offset = this.offset.add(this.random.nextGaussian(), this.random.nextGaussian(), this.random.nextGaussian()).multiply(velocity);
   }

   @Generated
   public Random getRandom() {
      return this.random;
   }

   @Generated
   public Vec3d getOffset() {
      return this.offset;
   }
}
