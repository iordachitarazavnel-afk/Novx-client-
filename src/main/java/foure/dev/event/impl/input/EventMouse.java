package foure.dev.event.impl.input;

import foure.dev.event.api.Event;
import lombok.Generated;

public class EventMouse extends Event {
   private final double mouseX;
   private final double mouseY;
   private final int button;
   private final int action;

   @Generated
   public double getMouseX() {
      return this.mouseX;
   }

   @Generated
   public double getMouseY() {
      return this.mouseY;
   }

   @Generated
   public int getButton() {
      return this.button;
   }

   @Generated
   public int getAction() {
      return this.action;
   }

   @Generated
   public EventMouse(double mouseX, double mouseY, int button, int action) {
      this.mouseX = mouseX;
      this.mouseY = mouseY;
      this.button = button;
      this.action = action;
   }
}
