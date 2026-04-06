package foure.dev.util.render.animation;

@FunctionalInterface
public interface EasingFunction {
   float ease(float var1);

   static EasingFunction identity() {
      return (t) -> {
         return t;
      };
   }

   default EasingFunction compose(EasingFunction after) {
      return after == null ? this : (t) -> {
         return after.ease(this.ease(t));
      };
   }
}
