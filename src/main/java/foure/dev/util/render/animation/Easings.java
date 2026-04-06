package foure.dev.util.render.animation;

public final class Easings {
   public static final EasingFunction EASE_OUT_CUBIC = (t) -> {
      float p = clamp(t);
      float inv = 1.0F - p;
      return 1.0F - inv * inv * inv;
   };
   public static final EasingFunction EASE_IN_OUT_QUINT = (t) -> {
      float p = clamp(t);
      float scaled;
      if (p < 0.5F) {
         scaled = p * 2.0F;
         return 0.5F * scaled * scaled * scaled * scaled * scaled;
      } else {
         scaled = (p - 0.5F) * 2.0F;
         float inv = 1.0F - scaled;
         return 1.0F - 0.5F * inv * inv * inv * inv * inv;
      }
   };
   public static final EasingFunction SMOOTH_STEP = (t) -> {
      float p = clamp(t);
      return p * p * (3.0F - 2.0F * p);
   };

   private Easings() {
   }

   private static float clamp(float value) {
      if (value <= 0.0F) {
         return 0.0F;
      } else {
         return value >= 1.0F ? 1.0F : value;
      }
   }
}
