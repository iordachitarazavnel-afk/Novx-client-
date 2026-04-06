package foure.dev.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

public class KeyUtils {
   public static boolean isKeyPressed(int keyCode) {
      MinecraftClient mc = MinecraftClient.getInstance();
      return mc != null && mc.getWindow() != null ? InputUtil.isKeyPressed(mc.getWindow(), keyCode) : false;
   }
}
