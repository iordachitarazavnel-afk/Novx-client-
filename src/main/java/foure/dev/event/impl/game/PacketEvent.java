package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.network.packet.Packet;

public class PacketEvent extends Event {
   private Packet<?> packet;

   public <T extends Packet<?>> T getPacket() {
      return (T) this.packet;
   }

   @Generated
   public void setPacket(Packet<?> packet) {
      this.packet = packet;
   }

   @Generated
   public PacketEvent(Packet<?> packet) {
      this.packet = packet;
   }

   public static class ReceivePost extends PacketEvent {
      public ReceivePost(Packet<?> packet) {
         super(packet);
      }
   }

   public static class Receive extends PacketEvent {
      public Receive(Packet<?> packet) {
         super(packet);
      }
   }

   public static class Send extends PacketEvent {
      public Send(Packet<?> packet) {
         super(packet);
      }
   }
}
