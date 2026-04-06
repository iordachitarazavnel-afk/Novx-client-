package foure.dev.util.math;

import foure.dev.util.wrapper.Wrapper;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public class ProjectionUtil implements Wrapper {
   public static Vec3d interpolateEntity(Entity entity, float tickDelta) {
      double x = MathHelper.lerp((double)tickDelta, entity.lastRenderX, entity.getX());
      double y = MathHelper.lerp((double)tickDelta, entity.lastRenderY, entity.getY());
      double z = MathHelper.lerp((double)tickDelta, entity.lastRenderZ, entity.getZ());
      return new Vec3d(x, y, z);
   }

   public static Vec3d toScreen(Vec3d worldPos) {
      Camera camera = mc.gameRenderer.getCamera();
      if (camera == null) {
         return null;
      } else {
         Vec3d camPos = camera.getCameraPos();
         Vector4f pos = new Vector4f((float)(worldPos.x - camPos.x), (float)(worldPos.y - camPos.y), (float)(worldPos.z - camPos.z), 1.0F);
         Matrix4f view = MatrixCapture.viewMatrix;
         Matrix4f proj = MatrixCapture.projectionMatrix;
         pos.mul(view);
         pos.mul(proj);
         if (pos.w <= 0.0F) {
            return null;
         } else {
            pos.div(pos.w);
            float x = (pos.x * 0.5F + 0.5F) * (float)mc.getWindow().getScaledWidth();
            float y = (1.0F - (pos.y * 0.5F + 0.5F)) * (float)mc.getWindow().getScaledHeight();
            return new Vec3d((double)x, (double)y, 0.0D);
         }
      }
   }
}
