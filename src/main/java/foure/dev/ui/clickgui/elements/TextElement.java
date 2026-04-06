package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class TextElement implements SettingElement {
   private final StringSetting setting;
   private boolean listening;
   private final float height = 18.0F;

   public TextElement(StringSetting setting) {
      this.setting = setting;
   }

   public void render(Renderer2D r, FontObject font, float x, float y, float width, float alpha) {
      int textAlpha = (int)(255.0F * alpha);
      r.text(font, x + 4.0F, y + 6.0F, 7.0F, this.setting.getName(), (new Color(180, 180, 180, textAlpha)).getRGB(), "l");
      float boxY = y + 10.0F;
      float boxH = 12.0F;
      int bgColor = this.listening ? (new Color(40, 40, 45, textAlpha)).getRGB() : (new Color(30, 30, 35, textAlpha)).getRGB();
      r.rect(x + 4.0F, boxY, width - 8.0F, boxH, 2.0F, bgColor);
      if (this.listening) {
         r.rectOutline(x + 4.0F, boxY, width - 8.0F, boxH, 2.0F, (new Color(100, 160, 255, textAlpha)).getRGB(), 1.0F);
      }

      String var10000 = (String)this.setting.getValue();
      String content = var10000 + (this.listening && System.currentTimeMillis() % 1000L > 500L ? "_" : "");
      r.text(font, x + 7.0F, boxY + boxH / 2.0F + 2.0F, 7.0F, content, (new Color(255, 255, 255, textAlpha)).getRGB(), "l");
   }

   public boolean mouseClicked(double mx, double my, int btn, float x, float y, float width) {
      if (mx >= (double)x && mx <= (double)(x + width) && my >= (double)y && my <= (double)(y + 18.0F)) {
         this.listening = !this.listening;
         return true;
      } else {
         this.listening = false;
         return false;
      }
   }

   public void keyTyped(int key, int scanCode, int action) {
      if (this.listening && action != 0) {
         if (key == 259) {
            if (!((String)this.setting.getValue()).isEmpty()) {
               this.setting.setValue(((String)this.setting.getValue()).substring(0, ((String)this.setting.getValue()).length() - 1));
            }
         } else if (key == 257 || key == 256) {
            this.listening = false;
         }

      }
   }

   public void handleKeyPress(int key) {
      if (this.listening) {
         if (key == 259) {
            String val = (String)this.setting.getValue();
            if (!val.isEmpty()) {
               this.setting.setValue(val.substring(0, val.length() - 1));
            }

         } else if (key != 257 && key != 256) {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            boolean ctrl = GLFW.glfwGetKey(window, 341) == 1 || GLFW.glfwGetKey(window, 345) == 1;
            StringSetting var10000;
            String var10001;
            if (ctrl && key == 86) {
               String clipboard = MinecraftClient.getInstance().keyboard.getClipboard();
               if (clipboard != null && !clipboard.isEmpty()) {
                  var10000 = this.setting;
                  var10001 = (String)this.setting.getValue();
                  var10000.setValue(var10001 + clipboard);
               }

            } else {
               char c = this.keyToChar(key);
               if (c != 0) {
                  var10000 = this.setting;
                  var10001 = (String)this.setting.getValue();
                  var10000.setValue(var10001 + c);
               }

            }
         } else {
            this.listening = false;
         }
      }
   }

   private char keyToChar(int key) {
      long window = MinecraftClient.getInstance().getWindow().getHandle();
      boolean shift = GLFW.glfwGetKey(window, 340) == 1 || GLFW.glfwGetKey(window, 344) == 1;
      if (key >= 65 && key <= 90) {
         return shift ? (char)key : (char)(key + 32);
      } else if (key >= 48 && key <= 57) {
         if (shift) {
            if (key == 49) {
               return '!';
            }

            if (key == 50) {
               return '@';
            }

            if (key == 51) {
               return '#';
            }

            if (key == 52) {
               return '$';
            }

            if (key == 53) {
               return '%';
            }

            if (key == 54) {
               return '^';
            }

            if (key == 55) {
               return '&';
            }

            if (key == 56) {
               return '*';
            }

            if (key == 57) {
               return '(';
            }

            if (key == 48) {
               return ')';
            }
         }

         return (char)key;
      } else {
         switch(key) {
         case 32:
            return ' ';
         case 39:
            return (char)(shift ? '"' : '\'');
         case 44:
            return (char)(shift ? '<' : ',');
         case 45:
            return (char)(shift ? '_' : '-');
         case 46:
            return (char)(shift ? '>' : '.');
         case 47:
            return (char)(shift ? '?' : '/');
         case 59:
            return (char)(shift ? ':' : ';');
         case 61:
            return (char)(shift ? '+' : '=');
         case 91:
            return (char)(shift ? '{' : '[');
         case 92:
            return (char)(shift ? '|' : '\\');
         case 93:
            return (char)(shift ? '}' : ']');
         case 96:
            return (char)(shift ? '~' : '`');
         default:
            return '\u0000';
         }
      }
   }

   public boolean isListening() {
      return this.listening;
   }

   public float getHeight() {
      return 18.0F;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }

   public void updateHover(double mx, double my, float x, float y, float w) {
   }

   public void mouseReleased(double mx, double my, int b) {
   }

   public void mouseDragged(double mx, double my, int b, float x, float y, float w) {
   }

   public boolean mouseClicked(double mx, double my, float x, float y, float w) {
      return false;
   }
}
