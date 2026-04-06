package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import lombok.Generated;

public class RotationUpdateEvent extends Event {
   byte type;

   @Generated
   public byte getType() {
      return this.type;
   }

   @Generated
   public RotationUpdateEvent(byte type) {
      this.type = type;
   }
}
