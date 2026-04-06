package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

@ModuleInfo(
   name = "AutoJumpReset",
   category = Category.MOVEMENT,
   desc = "Jumps when you take damage to reduce knockback"
)
public class AutoJumpReset extends Function {
   @Subscribe
   public void onPacket(PacketEvent event) {
      if (!fullNullCheck()) {
         Packet var3 = event.getPacket();
         if (var3 instanceof EntityVelocityUpdateS2CPacket) {
            EntityVelocityUpdateS2CPacket velocityPacket = (EntityVelocityUpdateS2CPacket)var3;
            if (velocityPacket.getEntityId() == mc.player.getId() && mc.player.isOnGround()) {
               mc.player.jump();
            }
         }

      }
   }
}
