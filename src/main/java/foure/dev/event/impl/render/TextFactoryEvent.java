package foure.dev.event.impl.render;

import foure.dev.event.api.Event;
import lombok.Generated;

public class TextFactoryEvent extends Event {
   private String text;

   public void replaceText(String protect, String replaced) {
      if (this.text != null && !this.text.isEmpty()) {
         if (this.text.contains(protect) && (this.text.equalsIgnoreCase(protect) || this.text.contains(protect + " ") || this.text.contains(" " + protect) || this.text.contains("⏏" + protect) || this.text.contains(protect + "§"))) {
            this.text = this.text.replace(protect, replaced);
         }

      }
   }

   @Generated
   public void setText(String text) {
      this.text = text;
   }

   @Generated
   public String getText() {
      return this.text;
   }

   @Generated
   public TextFactoryEvent(String text) {
      this.text = text;
   }
}
