package foure.dev.util.math;

import foure.dev.util.wrapper.Wrapper;
import java.util.Random;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MathUtil implements Wrapper {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   static Random random = new Random();
   public static double PI2 = 6.283185307179586D;

   public static double computeGcd() {
      return Math.pow((Double)mc.options.getMouseSensitivity().getValue() * 0.6D + 0.2D, 3.0D) * 1.2D;
   }

   public static float getTickDelta() {
      return mc.getRenderTickCounter().getTickProgress(false);
   }

   public static double interpolate(double prev, double current, double delta) {
      return prev + (current - prev) * delta;
   }

   public static float interpolateRandom(float a, float b) {
      return (float)(random.nextGaussian() * (double)(b - a) + (double)a);
   }

   public static double round(double num, double increment) {
      double rounded = (double)Math.round(num / increment) * increment;
      return (double)Math.round(rounded * 100.0D) / 100.0D;
   }

   public static float textScrolling(float textWidth) {
      int speed = (int)(textWidth * 75.0F);
      return (float)MathHelper.clamp((double)(System.currentTimeMillis() % (long)speed) * 3.141592653589793D / (double)speed, 0.0D, 1.0D) * textWidth;
   }

   public static float round(float number) {
      return (float)Math.round(number * 10.0F) / 10.0F;
   }

   public static float interpolate(float prev, float current, float delta) {
      return prev + (current - prev) * delta;
   }

   public static Vec3d interpolate(Entity entity) {
      return entity == null ? Vec3d.ZERO : new Vec3d(interpolate(entity.lastX, entity.getX()), interpolate(entity.lastY, entity.getY()), interpolate(entity.lastZ, entity.getZ()));
   }

   public static int floorNearestMulN(int x, int n) {
      return n * (int)Math.floor((double)x / (double)n);
   }

   public static double interpolate(double prev, double orig) {
      return MathHelper.lerp((double)mc.getRenderTickCounter().getTickProgress(false), prev, orig);
   }

   public static float interpolate(float prev, float orig) {
      return MathHelper.lerp(mc.getRenderTickCounter().getTickProgress(false), prev, orig);
   }

   public static Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
      return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
   }

   public static float getRandom(float min, double max) {
      return (float)(Math.random() * (max - (double)min) + (double)min);
   }

   public static double clamp(double value, double min, double max) {
      return Math.max(min, Math.min(max, value));
   }

   public static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   public static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   public static Vec3d cosSin(int i, int size, double width) {
      int index = Math.min(i, size);
      float cos = (float)(Math.cos((double)index * PI2 / (double)size) * width);
      float sin = (float)(-Math.sin((double)index * PI2 / (double)size) * width);
      return new Vec3d((double)cos, 0.0D, (double)sin);
   }

   public static double absSinAnimation(double input) {
      return Math.abs(1.0D + Math.sin(input)) / 2.0D;
   }

   public static double getDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
      double dx = x1 - x2;
      double dy = y1 - y2;
      double dz = z1 - z2;
      return Math.sqrt(dx * dx + dy * dy + dz * dz);
   }

   public static double toRadians(double degrees) {
      return degrees * 0.017453292519943295D;
   }

   public static double toDegrees(double radians) {
      return radians * 57.29577951308232D;
   }

   public static float wrapDegrees(float angle) {
      return MathHelper.wrapDegrees(angle);
   }

   public static float calcAngle(float srcX, float srcZ, float targetX, float targetZ) {
      float diffX = targetX - srcX;
      float diffZ = targetZ - srcZ;
      return (float)(Math.toDegrees(Math.atan2((double)diffZ, (double)diffX)) - 90.0D);
   }

   @Generated
   private MathUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
