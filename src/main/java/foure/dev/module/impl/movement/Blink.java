package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@ModuleInfo(
   name = "Blink",
   category = Category.MOVEMENT,
   desc = "Suspends movement packets and sends them all at once"
)
public class Blink extends Function {
   private final List<Packet<?>> packets = new ArrayList();

   public void onEnable() {
      super.onEnable();
      this.packets.clear();
   }

   public void onDisable() {
      super.onDisable();
      if (mc.player != null && mc.getNetworkHandler() != null) {
         synchronized(this.packets) {
            Iterator var2 = this.packets.iterator();

            while(var2.hasNext()) {
               Packet<?> packet = (Packet)var2.next();
               mc.getNetworkHandler().sendPacket(packet);
            }

            this.packets.clear();
         }
      }

   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      if (event.getPacket() instanceof PlayerMoveC2SPacket) {
         event.cancel();
         synchronized(this.packets) {
            this.packets.add(event.getPacket());
         }
      }

   }
}
