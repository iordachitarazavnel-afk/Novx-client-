package foure.dev.event.impl.presss;

import foure.dev.event.api.Event;
import lombok.Generated;

public class EventPress extends Event {
   int key;
   int action;

   @Generated
   public int getKey() {
      return this.key;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public EventPress(int key, int action) {
      this.key = key;
      this.action = action;
   }
}
