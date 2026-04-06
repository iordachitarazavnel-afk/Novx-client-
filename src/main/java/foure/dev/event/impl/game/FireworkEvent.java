package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.math.Vec3d;

public class FireworkEvent extends Event {
   public Vec3d vector;

   @Generated
   public FireworkEvent(Vec3d vector) {
      this.vector = vector;
   }

   @Generated
   public Vec3d getVector() {
      return this.vector;
   }

   @Generated
   public void setVector(Vec3d vector) {
      this.vector = vector;
   }
}
