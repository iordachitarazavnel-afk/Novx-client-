package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class EventAttackEntity extends Event {
   private final PlayerEntity player;
   private final Entity target;

   @Generated
   public EventAttackEntity(PlayerEntity player, Entity target) {
      this.player = player;
      this.target = target;
   }

   @Generated
   public PlayerEntity getPlayer() {
      return this.player;
   }

   @Generated
   public Entity getTarget() {
      return this.target;
   }
}
