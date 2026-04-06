package foure.dev.util.Player;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Function;
import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

public class PlayerServis {
   private int serverSlot;
   private float serverYaw;
   private float serverPitch;
   private float fallDistance;
   private double serverX;
   private double serverY;
   private double serverZ;
   private boolean serverOnGround;
   private boolean serverSprinting;
   private boolean serverSneaking;
   private boolean serverHorizontalCollision;

   public PlayerServis() {
      FourEClient.getInstance().getEventBus().register(this);
   }

   @Subscribe
   public void onPacketSend(PacketEvent.Send e) {
      if (!Function.fullNullCheck()) {
         Packet var3 = e.getPacket();
         if (var3 instanceof PlayerMoveC2SPacket) {
            PlayerMoveC2SPacket packet = (PlayerMoveC2SPacket)var3;
            if (packet.changesPosition()) {
               this.serverX = packet.getX(Wrapper.mc.player.getX());
               this.serverY = packet.getY(Wrapper.mc.player.getY());
               this.serverZ = packet.getZ(Wrapper.mc.player.getZ());
            }

            if (packet.changesLook()) {
               this.serverYaw = packet.getYaw(Wrapper.mc.player.getYaw());
               this.serverPitch = packet.getPitch(Wrapper.mc.player.getPitch());
            }

            this.serverOnGround = packet.isOnGround();
            this.serverHorizontalCollision = packet.horizontalCollision();
         }

         var3 = e.getPacket();
         if (var3 instanceof UpdateSelectedSlotC2SPacket) {
            UpdateSelectedSlotC2SPacket packet = (UpdateSelectedSlotC2SPacket)var3;
            this.serverSlot = packet.getSelectedSlot();
         }

         var3 = e.getPacket();
         if (var3 instanceof ClientCommandC2SPacket) {
            ClientCommandC2SPacket packet = (ClientCommandC2SPacket)var3;
            switch(packet.getMode()) {
            case START_SPRINTING:
               this.serverSprinting = true;
               break;
            case STOP_SPRINTING:
               this.serverSprinting = false;
            }
         }

      }
   }

   @Generated
   public int getServerSlot() {
      return this.serverSlot;
   }

   @Generated
   public float getServerYaw() {
      return this.serverYaw;
   }

   @Generated
   public float getServerPitch() {
      return this.serverPitch;
   }

   @Generated
   public float getFallDistance() {
      return this.fallDistance;
   }

   @Generated
   public double getServerX() {
      return this.serverX;
   }

   @Generated
   public double getServerY() {
      return this.serverY;
   }

   @Generated
   public double getServerZ() {
      return this.serverZ;
   }

   @Generated
   public boolean isServerOnGround() {
      return this.serverOnGround;
   }

   @Generated
   public boolean isServerSprinting() {
      return this.serverSprinting;
   }

   @Generated
   public boolean isServerSneaking() {
      return this.serverSneaking;
   }

   @Generated
   public boolean isServerHorizontalCollision() {
      return this.serverHorizontalCollision;
   }
}
