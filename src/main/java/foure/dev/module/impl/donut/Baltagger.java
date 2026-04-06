package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.StringSetting;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

@ModuleInfo(
   name = "Baltagger",
   category = Category.DONUT,
   desc = "Appends hearts and money to nametags"
)
public class Baltagger extends Function {
   private final StringSetting apiKey = new StringSetting("API Key", this, "");
   private final BooleanSetting showSelf = new BooleanSetting("Show Self", this, false);
   private final ColorSetting moneyColor = new ColorSetting("Money Color", this, new Color(65280));
   private boolean checkedForApiKey = false;
   private int joinDelayTicks = 0;

   public void onEnable() {
      this.checkedForApiKey = false;
      this.joinDelayTicks = 0;
      BaltaggerRenderer.register();
   }

   public void onDisable() {
      BaltaggerRenderer.getInstance().clearCache();
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.world != null && mc.player != null) {
         if (this.isOnDonutSMP()) {
            if (((String)this.apiKey.getValue()).isEmpty() && !this.checkedForApiKey) {
               if (this.joinDelayTicks < 60) {
                  ++this.joinDelayTicks;
                  return;
               }

               this.checkedForApiKey = true;
               mc.player.networkHandler.sendChatCommand("api");
            }

            if (!((String)this.apiKey.getValue()).isEmpty()) {
               BaltaggerRenderer.getInstance().onTick(mc);
            }
         }
      } else {
         this.checkedForApiKey = false;
         this.joinDelayTicks = 0;
      }
   }

   public boolean isOnDonutSMP() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         return false;
      } else {
         ServerInfo serverInfo = client.getCurrentServerEntry();
         if (serverInfo != null) {
            String address = serverInfo.address.toLowerCase();
            return address.contains("donutsmp.net");
         } else {
            return false;
         }
      }
   }

   public String getApiKey() {
      return (String)this.apiKey.getValue();
   }

   public boolean shouldShowSelf() {
      return (Boolean)this.showSelf.getValue();
   }

   public int getMoneyColor() {
      return ((Color)this.moneyColor.getValue()).getRGB();
   }
}
