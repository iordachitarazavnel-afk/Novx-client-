package foure.dev.mixin.game;

import foure.dev.event.impl.presss.EventMouseButton;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Mouse.class})
public class MouseButtonMixin {
   @Inject(
           method = {"onMouseButton"},
           at = {@At("HEAD")}
   )
   private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
      (new EventMouseButton(button, action)).call();
   }
}
