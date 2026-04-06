package foure.dev.util.others.Lisener;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.player.UsingItemEvent;
import foure.dev.util.Player.PlayerInventoryComponent;
import foure.dev.util.others.ServerUtil;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class EventListener implements Listener {
   public static boolean serverSprint;
   public static int selectedSlot;

   @Subscribe
   public void onTick(EventUpdate e) {
      ServerUtil.tick();
      FourEClient.getInstance().getAttackPerpetrator().tick();
      PlayerInventoryComponent.tick();
   }

   @Subscribe
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();

      if (packet instanceof ClientCommandC2SPacket command) {
         switch (command.getMode()) {
            case START_SPRINTING:
               serverSprint = true;
               break;
            case STOP_SPRINTING:
               serverSprint = false;
               break;
            default:
               // keep previous value
               break;
         }

      } else if (packet instanceof UpdateSelectedSlotC2SPacket slot) {
         selectedSlot = slot.getSelectedSlot();
      }

      FourEClient.getInstance().getAttackPerpetrator().onPacket(e);
   }

   @Subscribe
   public void onUsingItemEvent(UsingItemEvent e) {
      FourEClient.getInstance().getAttackPerpetrator().onUsingItem(e);
   }
}
