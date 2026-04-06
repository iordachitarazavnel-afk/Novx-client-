package foure.dev.event.impl.input;

import foure.dev.event.api.Event;
import lombok.Generated;
import net.minecraft.util.PlayerInput;

public class InputEvent extends Event {
   private PlayerInput input;

   public void setJumping(boolean jump) {
      this.input = new PlayerInput(this.input.forward(), this.input.backward(), this.input.left(), this.input.right(), jump, this.input.sneak(), this.input.sprint());
   }

   public void setDirectional(boolean forward, boolean backward, boolean left, boolean right) {
      this.input = new PlayerInput(forward, backward, left, right, this.input.jump(), this.input.sneak(), this.input.sprint());
   }

   public void inputNone() {
      this.input = new PlayerInput(false, false, false, false, false, false, false);
   }

   public int forward() {
      return this.input.forward() ? 1 : (this.input.backward() ? -1 : 0);
   }

   public float sideways() {
      return this.input.left() ? 1.0F : (this.input.right() ? -1.0F : 0.0F);
   }

   @Generated
   public PlayerInput getInput() {
      return this.input;
   }

   @Generated
   public void setInput(PlayerInput input) {
      this.input = input;
   }

   @Generated
   public InputEvent(PlayerInput input) {
      this.input = input;
   }
}
