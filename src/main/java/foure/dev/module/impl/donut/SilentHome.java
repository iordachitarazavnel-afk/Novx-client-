package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventPlayerTick;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.WebhookUtils;
import java.io.File;
import net.minecraft.client.util.ScreenshotRecorder;

@ModuleInfo(
   name = "SilentHome",
   desc = "Sets home silently with screenshot webhook",
   category = Category.DONUT
)
public class SilentHome extends Function {
   private final StringSetting webhookUrl = new StringSetting("Webhook URL", "");
   private final BooleanSetting screenshot = new BooleanSetting("Screenshot", true);
   private final BindSetting triggerKey = new BindSetting("Trigger Key", 71);
   private final BooleanSetting delHome = new BooleanSetting("Delete Previous Home", true);
   private final NumberSetting home = new NumberSetting("Home Slot", this, 1.0D, 1.0D, 5.0D, 1.0D);
   private boolean wasKeyPressed = false;
   private boolean suppressActionBar = false;
   private int suppressTicks = 0;
   private boolean suppressChatMessages = false;
   private int suppressChatTicks = 0;
   private long lastTriggerTime = 0L;
   private int delayTicks = 0;
   private boolean waitingToSnap = false;

   public SilentHome() {
      this.addSettings(new Setting[]{this.webhookUrl, this.screenshot, this.triggerKey, this.delHome, this.home});
   }

   public void onEnable() {
      this.wasKeyPressed = false;
      this.lastTriggerTime = 0L;
      this.delayTicks = 0;
      this.waitingToSnap = false;
   }

   public void onDisable() {
      this.wasKeyPressed = false;
      this.suppressActionBar = false;
      this.suppressTicks = 0;
      this.suppressChatMessages = false;
      this.suppressChatTicks = 0;
      this.lastTriggerTime = 0L;
      this.delayTicks = 0;
      this.waitingToSnap = false;
   }

   @Subscribe
   public void onTick(EventPlayerTick event) {
      if (mc.currentScreen == null) {
         if (mc.player != null) {
            if (this.suppressActionBar) {
               --this.suppressTicks;
               if (this.suppressTicks <= 0) {
                  this.suppressActionBar = false;
               }
            }

            if (this.suppressChatMessages) {
               --this.suppressChatTicks;
               if (this.suppressChatTicks <= 0) {
                  this.suppressChatMessages = false;
               }
            }

            if (this.waitingToSnap) {
               --this.delayTicks;
               if (this.delayTicks <= 0) {
                  this.homeCoords();
                  this.waitingToSnap = false;
               }
            }

            boolean isKeyPressed = this.triggerKey.isPressed();
            if (isKeyPressed && !this.wasKeyPressed) {
               this.suppressActionBar = true;
               this.suppressTicks = 40;
               this.suppressChatMessages = true;
               this.suppressChatTicks = 40;
               if ((Boolean)this.delHome.getValue()) {
                  mc.getNetworkHandler().sendChatCommand("delhome " + ((Double)this.home.getValue()).intValue());
               }

               this.delayTicks = 10;
               this.waitingToSnap = true;
            }

            this.wasKeyPressed = isKeyPressed;
         }
      }
   }

   private void homeCoords() {
      if (mc.player != null) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastTriggerTime >= 500L) {
            this.lastTriggerTime = currentTime;
            mc.getNetworkHandler().sendChatCommand("sethome " + ((Double)this.home.getValue()).intValue());
            String url = (String)this.webhookUrl.getValue();
            if (!url.trim().isEmpty() && url.startsWith("https://")) {
               int var10000 = (int)Math.round(mc.player.getX());
               String screenshotName = "home_" + var10000 + "_" + (int)Math.round(mc.player.getY()) + "_" + (int)Math.round(mc.player.getZ()) + "_" + System.currentTimeMillis();
               if (!(Boolean)this.screenshot.getValue()) {
                  (new WebhookUtils(url)).setTitle("Home Snapped").addCoords().addServer().addTime().send();
               } else {
                  try {
                     ScreenshotRecorder.saveScreenshot(mc.runDirectory, mc.getFramebuffer(), (text) -> {
                     });
                  } catch (Exception var6) {
                     var6.printStackTrace();
                     return;
                  }

                  File screenshotFile = new File(mc.runDirectory, "screenshots/" + screenshotName + ".png");
                  if (screenshotFile.exists()) {
                     (new WebhookUtils(url)).setTitle("Home Snapped").addCoords().addServer().addTime().setScreenshot(screenshotFile).send();
                  } else {
                     (new WebhookUtils(url)).setTitle("Home Snapped (No Image)").addCoords().addServer().addTime().send();
                  }

               }
            }
         }
      }
   }
}
