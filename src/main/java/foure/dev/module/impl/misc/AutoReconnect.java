package foure.dev.module.impl.misc;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

@ModuleInfo(
   name = "AutoReconnect",
   category = Category.MISC,
   desc = "Automatically reconnects when disconnected from a server"
)
public class AutoReconnect extends Function {
   private final NumberSetting time = new NumberSetting("Delay", this, 3.5D, 0.0D, 60.0D, 0.1D);
   private final BooleanSetting hideButtons = new BooleanSetting("Hide Buttons", this, false);
   public ServerAddress lastServerAddress;
   public ServerInfo lastServerInfo;

   public void setLastServer(ServerAddress address, ServerInfo info) {
      this.lastServerAddress = address;
      this.lastServerInfo = info;
   }

   public double getTime() {
      return (Double)this.time.getValue();
   }

   public boolean shouldHideButtons() {
      return (Boolean)this.hideButtons.getValue();
   }

   public boolean hasLastServer() {
      return this.lastServerAddress != null;
   }
}
