package foure.dev.mixin.client;

import foure.dev.ui.screens.MainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({TitleScreen.class})
public class MixinTitleScreen {
   @Inject(
           method = {"init"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onInit(CallbackInfo ci) {
      MinecraftClient.getInstance().setScreen(MainMenu.INSTANCE);
      ci.cancel();
   }
}
