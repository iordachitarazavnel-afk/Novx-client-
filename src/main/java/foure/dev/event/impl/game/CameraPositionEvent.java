package foure.dev.event.impl.game;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.math.Vec3d;

public class CameraPositionEvent extends Event {
   private Vec3d pos;

   @Generated
   public Vec3d getPos() {
      return this.pos;
   }

   @Generated
   public void setPos(Vec3d pos) {
      this.pos = pos;
   }

   @Generated
   public CameraPositionEvent(Vec3d pos) {
      this.pos = pos;
   }
}
