package foure.dev.mixin.game;

import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.HotBarUpdateEvent;
import foure.dev.ui.clickgui.ClickGuiScreen;
import foure.dev.util.input.MouseHandler;
import foure.dev.util.others.Lisener.Counter;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MinecraftClient.class})
public class MinecraftClientMixin {
   @Inject(
           method = {"tick"},
           at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      EventUpdate eventUpdate = new EventUpdate();
      eventUpdate.call();
      MouseHandler.handleMouse();
      Counter.updateFPS();
      if (eventUpdate.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
           method = {"handleInputEvents"},
           at = {@At(
                   value = "INVOKE",
                   target = "Lnet/minecraft/client/network/ClientPlayerEntity;getInventory()Lnet/minecraft/entity/player/PlayerInventory;"
           )},
           cancellable = true
   )
   public void handleInputEventsHook(CallbackInfo ci) {
      HotBarUpdateEvent event = new HotBarUpdateEvent();
      FourEClient.getInstance().getEventBus().post(event);
      if (event.isCanceled()) {
         ci.cancel();
      }

   }

   @Inject(
           method = {"handleInputEvents"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void handleInputEvents(CallbackInfo ci) {
      ClickGuiScreen clickGui = (ClickGuiScreen)FourEClient.getInstance().getFunctionManager().getModule(ClickGuiScreen.class);
      if (clickGui != null && clickGui.isOpen()) {
         ci.cancel();
      }

   }

   @Inject(
           method = {"getWindowTitle"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void updateWindowTitle(CallbackInfoReturnable<String> cir) {
      cir.setReturnValue("4e CLIENT 1.21.11");
   }
}
