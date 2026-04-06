package foure.dev.mixin.game;

import foure.dev.FourEClient;
import foure.dev.event.impl.game.KeyEvent;
import foure.dev.event.impl.presss.EventPress;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Final
   @Shadow
   private MinecraftClient client;

   @Inject(
           at = {@At("HEAD")},
           method = {"onKey"}
   )
   private void onKey(long window, int action, KeyInput input, CallbackInfo ci) {
      EventPress event = new EventPress(input.key(), action);
      event.call();
      FourEClient.getInstance().getEventBus().post(new KeyEvent(this.client.currentScreen, Type.KEYSYM, input.key(), action));
   }
}
