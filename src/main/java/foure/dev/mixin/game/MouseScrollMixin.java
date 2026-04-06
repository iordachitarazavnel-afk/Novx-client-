package foure.dev.mixin.game;

import foure.dev.event.impl.input.EventMouseScroll;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseScrollMixin {
   @Inject(
           method = {"onMouseScroll"},
           at = {@At("HEAD")}
   )
   private void onScroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
      (new EventMouseScroll(yOffset)).call();
   }
}
