package foure.dev.event.impl.presss;

import foure.dev.event.api.Event;
import lombok.Generated;

public class EventMouseButton extends Event {
   private final int button;
   private final int action;

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public EventMouseButton(int button, int action) {
      this.button = button;
      this.action = action;
   }
}
