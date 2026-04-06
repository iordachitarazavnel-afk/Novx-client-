package foure.dev.util.others.Lisener;

import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;

public final class Counter implements Wrapper {
   private static int currentFPS;

   public static void updateFPS() {
      int prevFPS = mc.getCurrentFps();
      currentFPS = MathHelper.lerp(0.8F, prevFPS, currentFPS);
   }

   @Generated
   private Counter() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static int getCurrentFPS() {
      return currentFPS;
   }
}
