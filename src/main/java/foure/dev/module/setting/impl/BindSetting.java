package foure.dev.module.setting.impl;

import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class BindSetting extends Setting<Integer> {
   private boolean listening = false;

   public BindSetting(String name, Function parent, int defaultKey) {
      super(name, parent, defaultKey);
   }

   public BindSetting(String name, int defaultKey) {
      super(name, (Function)null, defaultKey);
   }

   public boolean isPressed() {
      if ((Integer)this.getValue() != -1 && (Integer)this.getValue() != -1) {
         try {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            return GLFW.glfwGetKey(window, (Integer)this.getValue()) == 1;
         } catch (Exception var3) {
            return false;
         }
      } else {
         return false;
      }
   }

   public String getKeyName() {
      if ((Integer)this.getValue() != -1 && (Integer)this.getValue() != -1) {
         String keyName = GLFW.glfwGetKeyName((Integer)this.getValue(), 0);
         if (keyName != null) {
            return keyName.toUpperCase();
         } else {
            String var10000;
            switch((Integer)this.getValue()) {
            case 32:
               var10000 = "SPACE";
               break;
            case 256:
               var10000 = "ESC";
               break;
            case 257:
               var10000 = "ENTER";
               break;
            case 258:
               var10000 = "TAB";
               break;
            case 259:
               var10000 = "BACKSPACE";
               break;
            case 340:
               var10000 = "LSHIFT";
               break;
            case 341:
               var10000 = "LCTRL";
               break;
            case 342:
               var10000 = "LALT";
               break;
            case 344:
               var10000 = "RSHIFT";
               break;
            case 345:
               var10000 = "RCTRL";
               break;
            case 346:
               var10000 = "RALT";
               break;
            default:
               var10000 = "KEY_" + String.valueOf(this.getValue());
            }

            return var10000;
         }
      } else {
         return "NONE";
      }
   }

   @Generated
   public boolean isListening() {
      return this.listening;
   }

   @Generated
   public void setListening(boolean listening) {
      this.listening = listening;
   }
}
