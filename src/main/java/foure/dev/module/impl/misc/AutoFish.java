package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventPlayerTick;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "AutoFish",
   category = Category.MISC,
   desc = "Automatically catches fish"
)
public class AutoFish extends Function {
   private int recatchDelay = 0;

   @Subscribe
   public void onTick(EventPlayerTick event) {
      if (!fullNullCheck()) {
         if (this.recatchDelay > 0) {
            --this.recatchDelay;
            if (this.recatchDelay == 0 && mc.player.getMainHandStack().getItem() == Items.FISHING_ROD) {
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            }
         }

      }
   }

   @Subscribe
   public void onPacketReceive(PacketEvent.Receive event) {
      if (!fullNullCheck()) {
         Packet var3 = event.getPacket();
         if (var3 instanceof PlaySoundS2CPacket) {
            PlaySoundS2CPacket packet = (PlaySoundS2CPacket)var3;
            if (((SoundEvent)packet.getSound().value()).equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH) && mc.player.fishHook != null) {
               double dist = mc.player.fishHook.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
               if (dist <= 1.0D) {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                  this.recatchDelay = 20;
               }
            }
         }

      }
   }
}
