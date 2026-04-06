package foure.dev.mixin.player;

import foure.dev.FourEClient;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.util.wrapper.Wrapper;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientConnection.class})
public class MixinClientConnection {
   @Inject(
           method = {"handlePacket"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private static <T extends PacketListener> void onHandlePacket(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      if (Wrapper.mc.player != null && Wrapper.mc.world != null) {
         if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket packs = (BundleS2CPacket)packet;
            packs.getPackets().forEach((p) -> {
               PacketEvent.Receive event = new PacketEvent.Receive(p);
               FourEClient.getInstance().getEventBus().post(event);
               if (event.isCanceled()) {
                  info.cancel();
               }

            });
         } else {
            PacketEvent.Receive event = new PacketEvent.Receive(packet);
            FourEClient.getInstance().getEventBus().post(event);
            if (event.isCanceled()) {
               info.cancel();
            }
         }

      }
   }

   @Inject(
           method = {"handlePacket"},
           at = {@At("TAIL")},
           cancellable = true
   )
   private static <T extends PacketListener> void onHandlePacketPost(Packet<T> packet, PacketListener listener, CallbackInfo info) {
      if (Wrapper.mc.player != null && Wrapper.mc.world != null) {
         if (packet instanceof BundleS2CPacket) {
            BundleS2CPacket packs = (BundleS2CPacket)packet;
            packs.getPackets().forEach((p) -> {
               PacketEvent.ReceivePost event = new PacketEvent.ReceivePost(p);
               FourEClient.getInstance().getEventBus().post(event);
               if (event.isCanceled()) {
                  info.cancel();
               }

            });
         } else {
            PacketEvent.ReceivePost event = new PacketEvent.ReceivePost(packet);
            FourEClient.getInstance().getEventBus().post(event);
            if (event.isCanceled()) {
               info.cancel();
            }
         }

      }
   }

   @Inject(
           method = {"send(Lnet/minecraft/network/packet/Packet;)V"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onSendPacketPre(Packet<?> packet, CallbackInfo info) {
      if (Wrapper.mc.player != null && Wrapper.mc.world != null) {
         PacketEvent.Send event = new PacketEvent.Send(packet);
         FourEClient.getInstance().getEventBus().post(event);
         if (event.isCanceled()) {
            info.cancel();
         }

      }
   }
}
