package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import lombok.Generated;

public class UsingItemEvent extends Event {
   byte type;

   @Generated
   public byte getType() {
      return this.type;
   }

   @Generated
   public void setType(byte type) {
      this.type = type;
   }

   @Generated
   public UsingItemEvent(byte type) {
      this.type = type;
   }
}
