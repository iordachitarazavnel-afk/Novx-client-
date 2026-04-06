package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.Render3D;
import java.awt.Color;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "RenderTest",
   category = Category.RENDER,
   desc = "Test rendering system"
)
public class RenderTest extends Function {
   public void onEnable() {
      super.onEnable();
      System.out.println("[RenderTest] Enabled - you should see a RED box in front of you");
   }

   public void onDisable() {
      super.onDisable();
      System.out.println("[RenderTest] Disabled");
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && mc.player != null) {
         try {
            Render3D.begin(event.getCamera());
            double px = mc.player.getX();
            double py = mc.player.getY();
            double pz = mc.player.getZ();
            float yaw = mc.player.getYaw();
            double radians = Math.toRadians((double)yaw);
            double dx = -Math.sin(radians) * 5.0D;
            double dz = Math.cos(radians) * 5.0D;
            Box testBox = new Box(px + dx - 0.5D, py, pz + dz - 0.5D, px + dx + 0.5D, py + 1.0D, pz + dz + 0.5D);
            Render3D.setLineWidth(3.0F);
            Render3D.setDepthTest(false);
            Render3D.drawBoxFill(testBox, new Color(255, 0, 0, 100), 0);
            Render3D.drawBoxOutline(testBox, new Color(255, 0, 0, 255), 0);
            Matrix4f viewRotation = new Matrix4f(event.getMatrix());
            viewRotation.m30(0.0F);
            viewRotation.m31(0.0F);
            viewRotation.m32(0.0F);
            Render3D.end(viewRotation, event.getProjectionMatrix());
         } catch (Exception var17) {
            System.err.println("[RenderTest] Error:");
            var17.printStackTrace();
         }

      }
   }
}
