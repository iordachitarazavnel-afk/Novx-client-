package foure.dev.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import foure.dev.FourEClient;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.tracy.TracyFrameCapturer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({RenderSystem.class})
public class RenderSystemMixin {
   @Inject(
           method = {"flipFrame"},
           at = {@At("HEAD")}
   )
   private static void flipFrame(Window window, TracyFrameCapturer capturer, CallbackInfo ci) {
      FourEClient.onRender();
   }
}
