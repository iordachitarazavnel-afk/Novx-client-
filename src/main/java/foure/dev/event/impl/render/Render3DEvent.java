package foure.dev.event.impl.render;

import lombok.Generated;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix4f;

public class Render3DEvent {
   private final Matrix4f matrix;
   private final Matrix4f projectionMatrix;
   private final RenderTickCounter tickCounter;
   private final Camera camera;

   @Generated
   public Matrix4f getMatrix() {
      return this.matrix;
   }

   @Generated
   public Matrix4f getProjectionMatrix() {
      return this.projectionMatrix;
   }

   @Generated
   public RenderTickCounter getTickCounter() {
      return this.tickCounter;
   }

   @Generated
   public Camera getCamera() {
      return this.camera;
   }

   @Generated
   public Render3DEvent(Matrix4f matrix, Matrix4f projectionMatrix, RenderTickCounter tickCounter, Camera camera) {
      this.matrix = matrix;
      this.projectionMatrix = projectionMatrix;
      this.tickCounter = tickCounter;
      this.camera = camera;
   }
}
