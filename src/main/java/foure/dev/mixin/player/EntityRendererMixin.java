package foure.dev.mixin.player;

import foure.dev.FourEClient;
import foure.dev.module.impl.render.NameTags;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {
   @Inject(
           method = {"getDisplayName"},
           at = {@At("RETURN")},
           cancellable = true
   )
   public void getDisplayName(T entity, CallbackInfoReturnable<Text> cir) {
      if (((NameTags)FourEClient.getInstance().getFunctionManager().getModule(NameTags.class)).isToggled() && entity instanceof PlayerEntity) {
         cir.setReturnValue((Text) null);
      }

   }
}
