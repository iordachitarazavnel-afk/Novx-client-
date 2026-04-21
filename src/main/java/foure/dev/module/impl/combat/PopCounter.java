package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(
   name = "PopCounter",
   category = Category.COMBAT,
   desc = "Tracks totem pops"
)
public class PopCounter extends Function {
   private final BooleanSetting announceChat = new BooleanSetting("Announce Chat", true);
   private final BooleanSetting announceToast = new BooleanSetting("Announce Toast", false);
   private final Map<UUID, Integer> popMap = new HashMap();

   public PopCounter() {
      this.addSettings(new Setting[]{this.announceChat, this.announceToast});
   }

   public void onEnable() {
      super.onEnable();
      this.popMap.clear();
   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      if (mc.world != null) {
         Packet var3 = event.getPacket();
         if (var3 instanceof EntityStatusS2CPacket) {
            EntityStatusS2CPacket packet = (EntityStatusS2CPacket)var3;
            if (packet.getStatus() == 35) {
               Entity entity = packet.getEntity(mc.world);
               if (entity instanceof PlayerEntity) {
                  PlayerEntity player = (PlayerEntity)entity;
                  UUID uuid = player.getUuid();
                  int pops = (Integer)this.popMap.getOrDefault(uuid, 0) + 1;
                  this.popMap.put(uuid, pops);
                  String name = player.getName().getString();
                  String message = name + " popped " + String.valueOf(Formatting.GOLD) + pops + String.valueOf(Formatting.RESET) + (pops == 1 ? " totem" : " totems");
                  ClientPlayerEntity var10000;
                  String var10001;
                  if ((Boolean)this.announceChat.getValue()) {
                     var10000 = mc.player;
                     var10001 = String.valueOf(Formatting.GRAY);
                     var10000.sendMessage(Text.literal(var10001 + "[" + String.valueOf(Formatting.BLUE) + "novx Client" + String.valueOf(Formatting.GRAY) + "] " + String.valueOf(Formatting.RESET) + message), false);
                  }

                  if ((Boolean)this.announceToast.getValue()) {
                     var10000 = mc.player;
                     var10001 = String.valueOf(Formatting.YELLOW);
                     var10000.sendMessage(Text.literal(var10001 + "Totem Pop: " + message), true);
                  }
               }
            }
         }

      }
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.world != null) {
         this.popMap.entrySet().removeIf((entry) -> {
            PlayerEntity player = mc.world.getPlayerByUuid((UUID)entry.getKey());
            return player == null || player.isDead();
         });
      }
   }

   public int getPops(UUID uuid) {
      return (Integer)this.popMap.getOrDefault(uuid, 0);
   }

   public void resetPops(UUID uuid) {
      this.popMap.remove(uuid);
   }
}
