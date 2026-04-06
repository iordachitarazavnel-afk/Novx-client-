package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.math.Vec3d;

public class MoveEvent extends Event {
   private Vec3d movement;

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof MoveEvent)) {
         return false;
      } else {
         MoveEvent other = (MoveEvent)o;
         if (!other.canEqual(this)) {
            return false;
         } else if (!super.equals(o)) {
            return false;
         } else {
            Object this$movement = this.getMovement();
            Object other$movement = other.getMovement();
            if (this$movement == null) {
               if (other$movement != null) {
                  return false;
               }
            } else if (!this$movement.equals(other$movement)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof MoveEvent;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = super.hashCode();
      Object $movement = this.getMovement();
      result = result * 59 + ($movement == null ? 43 : $movement.hashCode());
      return result;
   }

   @Generated
   public Vec3d getMovement() {
      return this.movement;
   }

   @Generated
   public void setMovement(Vec3d movement) {
      this.movement = movement;
   }

   @Generated
   public String toString() {
      return "MoveEvent(movement=" + String.valueOf(this.getMovement()) + ")";
   }

   @Generated
   public MoveEvent(Vec3d movement) {
      this.movement = movement;
   }
}
