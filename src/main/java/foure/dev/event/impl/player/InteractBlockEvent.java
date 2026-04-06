package foure.dev.event.impl.player;

import foure.dev.event.api.Event;
import net.minecraft.util.math.BlockPos;

public class InteractBlockEvent extends Event {
   private final BlockPos pos;

   public InteractBlockEvent(BlockPos pos) {
      this.pos = pos;
   }

   public BlockPos getPos() {
      return this.pos;
   }
}
