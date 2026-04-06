package foure.dev.mixin.player;

import foure.dev.FourEClient;
import foure.dev.event.impl.input.ClickSlotEvent;
import foure.dev.event.impl.player.EventAttackEntity;
import foure.dev.event.impl.player.InteractBlockEvent;
import foure.dev.event.impl.player.UsingItemEvent;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult.Success;
import net.minecraft.util.ActionResult.SwingSource;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientPlayerInteractionManager.class})
public class ClientPlayerInteractionManagerMixin {
   @Inject(
           method = {"attackEntity"},
           at = {@At("HEAD")}
   )
   public void attackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
      EventAttackEntity event = new EventAttackEntity(player, target);
      FourEClient.getInstance().getEventBus().post(event);
   }

   @Inject(
           method = {"interactItem"},
           at = {@At("RETURN")}
   )
   public void interactItemHook(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      Object var5 = cir.getReturnValue();
      if (var5 instanceof Success) {
         Success success = (Success)var5;
         if (!success.swingSource().equals(SwingSource.CLIENT)) {
            UsingItemEvent event = new UsingItemEvent((byte)0);
            FourEClient.getInstance().getEventBus().post(event);
         }
      }

   }

   @Inject(
           method = {"clickSlot"},
           at = {@At("HEAD")},
           cancellable = true
   )
   public void clickSlotHook(int syncId, int slotId, int button, SlotActionType actionType, PlayerEntity player, CallbackInfo info) {
      ClickSlotEvent event = new ClickSlotEvent(syncId, slotId, button, actionType);
      FourEClient.getInstance().getEventBus().post(event);
      if (event.isCanceled()) {
         info.cancel();
      }

   }

   @Inject(
           method = {"interactBlock"},
           at = {@At("HEAD")}
   )
   public void interactBlockHook(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> info) {
      InteractBlockEvent event = new InteractBlockEvent(hitResult.getBlockPos());
      FourEClient.getInstance().getEventBus().post(event);
   }
}
