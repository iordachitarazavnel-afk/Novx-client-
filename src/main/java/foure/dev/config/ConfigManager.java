package foure.dev.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import foure.dev.FourEClient;
import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.ui.notification.NotificationType;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import net.minecraft.client.MinecraftClient;
import org.apache.commons.io.IOUtils;

public class ConfigManager {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private final File configDir;

   public ConfigManager() {
      this.configDir = new File(MinecraftClient.getInstance().runDirectory, "configs");
      if (!this.configDir.exists()) {
         this.configDir.mkdirs();
      }

   }

   public void save(String name) {
      if (name != null && !name.isEmpty()) {
         File file = new File(this.configDir, name + ".json");
         JsonObject json = new JsonObject();
         Iterator var4 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

         while(var4.hasNext()) {
            Function module = (Function)var4.next();
            JsonObject moduleJson = new JsonObject();
            moduleJson.addProperty("toggled", module.isToggled());
            moduleJson.addProperty("key", module.getKey());
            JsonObject settingsJson = new JsonObject();
            Iterator var8 = module.getSettings().iterator();

            while(var8.hasNext()) {
               Setting<?> setting = (Setting)var8.next();
               if (setting instanceof BooleanSetting) {
                  BooleanSetting bs = (BooleanSetting)setting;
                  settingsJson.addProperty(bs.getName(), (Boolean)bs.getValue());
               } else if (setting instanceof NumberSetting) {
                  NumberSetting ns = (NumberSetting)setting;
                  settingsJson.addProperty(ns.getName(), (Number)ns.getValue());
               } else if (setting instanceof ModeSetting) {
                  ModeSetting ms = (ModeSetting)setting;
                  settingsJson.addProperty(ms.getName(), (String)ms.getValue());
               } else if (setting instanceof StringSetting) {
                  StringSetting ss = (StringSetting)setting;
                  settingsJson.addProperty(ss.getName(), (String)ss.getValue());
               } else if (setting instanceof ColorSetting) {
                  ColorSetting cs = (ColorSetting)setting;
                  settingsJson.addProperty(cs.getName(), ((Color)cs.getValue()).getRGB());
               } else if (setting instanceof BindSetting) {
                  BindSetting bs = (BindSetting)setting;
                  settingsJson.addProperty(bs.getName(), (Number)bs.getValue());
               }
            }

            moduleJson.add("settings", settingsJson);
            json.add(module.getName(), moduleJson);
         }

         try {
            FileOutputStream fos = new FileOutputStream(file);

            try {
               IOUtils.write(GSON.toJson(json), fos, StandardCharsets.UTF_8);
               NotificationManager.add("Config", "Saved config '" + name + "'", NotificationType.SUCCESS);
            } catch (Throwable var17) {
               try {
                  fos.close();
               } catch (Throwable var16) {
                  var17.addSuppressed(var16);
               }

               throw var17;
            }

            fos.close();
         } catch (IOException var18) {
            var18.printStackTrace();
            NotificationManager.add("Config", "Failed to save config!", NotificationType.ERROR);
         }

      } else {
         NotificationManager.add("Config", "Invalid config name!", NotificationType.ERROR);
      }
   }

   public void load(String name) {
      if (name != null && !name.isEmpty()) {
         File file = new File(this.configDir, name + ".json");
         if (!file.exists()) {
            NotificationManager.add("Config", "Config not found!", NotificationType.ERROR);
         } else {
            try {
               FileInputStream fis = new FileInputStream(file);

               try {
                  String content = IOUtils.toString(fis, StandardCharsets.UTF_8);
                  JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                  Iterator var6 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

                  label89:
                  while(true) {
                     Function module;
                     JsonObject moduleJson;
                     do {
                        do {
                           if (!var6.hasNext()) {
                              NotificationManager.add("Config", "Loaded config '" + name + "'", NotificationType.SUCCESS);
                              break label89;
                           }

                           module = (Function)var6.next();
                        } while(!json.has(module.getName()));

                        moduleJson = json.getAsJsonObject(module.getName());
                        if (moduleJson.has("toggled")) {
                           boolean state = moduleJson.get("toggled").getAsBoolean();
                           if (module.isToggled() != state) {
                              module.toggle();
                           }
                        }

                        if (moduleJson.has("key")) {
                           module.setKey(moduleJson.get("key").getAsInt());
                        }
                     } while(!moduleJson.has("settings"));

                     JsonObject settingsJson = moduleJson.getAsJsonObject("settings");
                     Iterator var10 = module.getSettings().iterator();

                     while(var10.hasNext()) {
                        Setting<?> setting = (Setting)var10.next();
                        if (settingsJson.has(setting.getName())) {
                           JsonElement element = settingsJson.get(setting.getName());

                           try {
                              if (setting instanceof BooleanSetting) {
                                 BooleanSetting bs = (BooleanSetting)setting;
                                 bs.setValue(element.getAsBoolean());
                              } else if (setting instanceof NumberSetting) {
                                 NumberSetting ns = (NumberSetting)setting;
                                 ns.setValue(element.getAsDouble());
                              } else if (setting instanceof ModeSetting) {
                                 ModeSetting ms = (ModeSetting)setting;
                                 ms.setValue(element.getAsString());
                              } else if (setting instanceof StringSetting) {
                                 StringSetting ss = (StringSetting)setting;
                                 ss.setValue(element.getAsString());
                              } else if (setting instanceof ColorSetting) {
                                 ColorSetting cs = (ColorSetting)setting;
                                 cs.setValue(new Color(element.getAsInt(), true));
                              } else if (setting instanceof BindSetting) {
                                 BindSetting bs = (BindSetting)setting;
                                 bs.setValue(element.getAsInt());
                              }
                           } catch (Exception var20) {
                              PrintStream var10000 = System.err;
                              String var10001 = setting.getName();
                              var10000.println("Error loading setting " + var10001 + " for module " + module.getName());
                           }
                        }
                     }
                  }
               } catch (Throwable var21) {
                  try {
                     fis.close();
                  } catch (Throwable var19) {
                     var21.addSuppressed(var19);
                  }

                  throw var21;
               }

               fis.close();
            } catch (IOException var22) {
               var22.printStackTrace();
               NotificationManager.add("Config", "Failed to load config!", NotificationType.ERROR);
            }

         }
      } else {
         NotificationManager.add("Config", "Invalid config name!", NotificationType.ERROR);
      }
   }
}
