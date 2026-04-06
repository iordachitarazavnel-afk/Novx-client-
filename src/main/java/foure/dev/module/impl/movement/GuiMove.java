package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.CloseScreenEvent;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.input.ClickSlotEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.Player.MobilityHandler;
import foure.dev.util.Player.PlayerIntersectionUtil;
import foure.dev.util.Player.PlayerInventoryComponent;
import foure.dev.util.Player.PlayerInventoryUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(
   name = "GuiMove",
   category = Category.MOVEMENT,
   desc = "Allows movement in inventory"
)
public class GuiMove extends Function {
   private final List<Packet<?>> packets = new ArrayList();

   @Subscribe
   public void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();

      // Handle ClickSlot packets
      if (packet instanceof ClickSlotC2SPacket slot) {
         if ((!this.packets.isEmpty() || MobilityHandler.hasPlayerMovement())
                 && PlayerInventoryComponent.shouldSkipExecution()) {

            this.packets.add(slot);
            e.cancel();
            return;
         }
      }

      // Handle CloseScreen packets
      else if (packet instanceof CloseScreenS2CPacket screen) {
         if (screen.getSyncId() == 0) {
            e.cancel();
            return;
         }
      }
   }

   @Subscribe
   public void onTick(EventUpdate e) {
      if (mc.player != null) {
         if (!PlayerInventoryUtil.isServerScreen() && PlayerInventoryComponent.shouldSkipExecution() && (!this.packets.isEmpty() || mc.player.currentScreenHandler.getCursorStack().isEmpty())) {
            PlayerInventoryComponent.updateMoveKeys();
         }

      }
   }

   @Subscribe
   public void onClickSlot(ClickSlotEvent e) {
      SlotActionType actionType = e.getActionType();
      if ((!this.packets.isEmpty() || MobilityHandler.hasPlayerMovement()) && (e.getButton() == 1 && !actionType.equals(SlotActionType.SWAP) && !actionType.equals(SlotActionType.THROW) || actionType.equals(SlotActionType.PICKUP_ALL))) {
         e.cancel();
      }

   }

   @Subscribe
   public void onCloseScreen(CloseScreenEvent e) {
      if (!this.packets.isEmpty()) {
         PlayerInventoryComponent.addTask(() -> {
            this.packets.forEach(PlayerIntersectionUtil::sendPacketWithOutEvent);
            this.packets.clear();
            PlayerInventoryUtil.updateSlots();
         });
      }

   }
}
