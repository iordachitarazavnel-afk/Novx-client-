package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

@ModuleInfo(
   name = "AutoLog",
   category = Category.DONUT,
   desc = "Automatically disconnects when certain requirements are met"
)
public class AutoLog extends Function {
   private final NumberSetting health = new NumberSetting("Health", this, 6.0D, 0.0D, 19.0D, 1.0D);
   private final BooleanSetting totemPops = new BooleanSetting("Totem Pop", true);
   private final BooleanSetting toggleOff = new BooleanSetting("Toggle Off", true);
   private final BooleanSetting toggleAutoReconnect = new BooleanSetting("Toggle Auto Reconnect", true);
   private int pops;

   public AutoLog() {
      this.addSettings(new Setting[]{this.health, this.totemPops, this.toggleOff, this.toggleAutoReconnect});
   }

   public void onEnable() {
      super.onEnable();
      this.pops = 0;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         float playerHealth = mc.player.getHealth();
         if (playerHealth <= 0.0F) {
            this.toggle();
         } else {
            if ((double)this.health.getValueFloat() > 0.0D && playerHealth <= this.health.getValueFloat()) {
               this.disconnect("Health was lower than " + (int)this.health.getValueFloat() + ".");
               if ((Boolean)this.toggleOff.getValue()) {
                  this.toggle();
               }
            }

         }
      }
   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof EntityStatusS2CPacket) {
         EntityStatusS2CPacket packet = (EntityStatusS2CPacket)var3;
         if (packet.getStatus() != 35) {
            return;
         }

         if (fullNullCheck()) {
            return;
         }

         Entity entity = packet.getEntity(mc.world);
         if (entity == null || !entity.equals(mc.player)) {
            return;
         }

         if ((Boolean)this.totemPops.getValue()) {
            this.disconnect("Popped totem.");
            if ((Boolean)this.toggleOff.getValue()) {
               this.toggle();
            }
         }
      }

   }

   private void disconnect(String reason) {
      this.disconnect((Text)Text.literal(reason));
   }

   private void disconnect(Text reason) {
      MutableText text = Text.literal("[AutoLog] ");
      text.append(reason);
      boolean autoReconnectFound = false;
      Iterator var4 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

      while(var4.hasNext()) {
         Function module = (Function)var4.next();
         if (module.getName().equals("AutoReconnect") && module.isToggled()) {
            if ((Boolean)this.toggleAutoReconnect.getValue()) {
               text.append(Text.literal("\n\nINFO - AutoReconnect was disabled").withColor(-8355712));
               module.toggle();
            }

            autoReconnectFound = true;
            break;
         }
      }

      mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));
   }
}
