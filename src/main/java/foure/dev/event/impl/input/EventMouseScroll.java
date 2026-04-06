package foure.dev.event.impl.input;

import foure.dev.event.api.Event;
import lombok.Generated;

public class EventMouseScroll extends Event {
   private final double delta;

   @Generated
   public double getDelta() {
      return this.delta;
   }

   @Generated
   public EventMouseScroll(double delta) {
      this.delta = delta;
   }
}
