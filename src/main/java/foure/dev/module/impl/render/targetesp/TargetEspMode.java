package foure.dev.module.impl.render.targetesp;

import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;

public abstract class TargetEspMode {
   protected MinecraftClient mc = MinecraftClient.getInstance();
   protected LivingEntity currentTarget;

   public void onUpdate() {
   }

   public abstract void onRender3D(Render3DEvent var1);

   public void onRender2D(RenderEvent event) {
   }

   public void updateTarget() {
      TargetEspModule module = TargetEspModule.getInstance();
      if (module != null) {
         this.currentTarget = module.getTarget();
      } else {
         this.currentTarget = null;
      }

   }

   public boolean canDraw() {
      return this.currentTarget != null && this.currentTarget.isAlive();
   }
}
