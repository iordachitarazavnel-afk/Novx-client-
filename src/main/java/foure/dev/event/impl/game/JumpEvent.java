package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.entity.player.PlayerEntity;

public class JumpEvent extends Event {
   private PlayerEntity player;

   @Generated
   public PlayerEntity getPlayer() {
      return this.player;
   }

   @Generated
   public JumpEvent(PlayerEntity player) {
      this.player = player;
   }
}
