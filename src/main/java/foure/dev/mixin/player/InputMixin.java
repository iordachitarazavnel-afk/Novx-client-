package foure.dev.mixin.player;

import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.input.Input;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Input.class})
public class InputMixin implements Wrapper {
}
