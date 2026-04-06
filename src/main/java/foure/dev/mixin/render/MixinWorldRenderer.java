package foure.dev.mixin.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.util.math.MatrixCapture;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({WorldRenderer.class})
public class MixinWorldRenderer {
   @Inject(
           method = {"render"},
           at = {@At("HEAD")}
   )
   private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f basicProjectionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
      if (projectionMatrix != null) {
         MatrixCapture.projectionMatrix.set(projectionMatrix);
         MatrixCapture.viewMatrix.set(positionMatrix);
      }
   }

   @Inject(
           method = {"render"},
           at = {@At("RETURN")}
   )
   private void onRenderReturn(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f basicProjectionMatrix, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
      if (projectionMatrix != null) {
         FourEClient.getInstance().getEventBus().post(new Render3DEvent(positionMatrix, projectionMatrix, tickCounter, camera));
      }

   }
}
