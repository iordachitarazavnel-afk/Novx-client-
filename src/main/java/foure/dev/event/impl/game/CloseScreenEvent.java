package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;

public class CloseScreenEvent extends Event {
   private Screen screen;

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof CloseScreenEvent)) {
         return false;
      } else {
         CloseScreenEvent other = (CloseScreenEvent)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else {
            Object this$screen = this.getScreen();
            Object other$screen = other.getScreen();
            if (this$screen == null) {
               if (other$screen != null) {
                  return false;
               }
            } else if (!this$screen.equals(other$screen)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof CloseScreenEvent;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = super.hashCode();
      Object $screen = this.getScreen();
      result = result * 59 + ($screen == null ? 43 : $screen.hashCode());
      return result;
   }

   @Generated
   public Screen getScreen() {
      return this.screen;
   }

   @Generated
   public void setScreen(Screen screen) {
      this.screen = screen;
   }

   @Generated
   public String toString() {
      return "CloseScreenEvent(screen=" + String.valueOf(this.getScreen()) + ")";
   }

   @Generated
   public CloseScreenEvent(Screen screen) {
      this.screen = screen;
   }
}
