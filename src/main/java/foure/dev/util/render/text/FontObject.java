package foure.dev.util.render.text;

import java.util.Objects;

public final class FontObject {
   public final String id;

   public FontObject(String id) {
      this.id = (String)Objects.requireNonNull(id, "id");
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         FontObject that = (FontObject)o;
         return this.id.equals(that.id);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.id.hashCode();
   }

   public String toString() {
      return "FontObject(" + this.id + ")";
   }
}
