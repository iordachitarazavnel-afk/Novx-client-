package foure.dev.util.input;

import org.lwjgl.glfw.GLFW;

public class KeyNameUtil {
   public static String getKeyName(int key) {
      if (key == -1) {
         return "None";
      } else if (key == 2000) {
         return "Wheel Up";
      } else if (key == 2001) {
         return "Wheel Down";
      } else if (key >= 1000) {
         return "Mouse " + (key - 1000);
      } else {
         return GLFW.glfwGetKeyName(key, 0) != null ? GLFW.glfwGetKeyName(key, 0).toUpperCase() : "Key " + key;
      }
   }
}
