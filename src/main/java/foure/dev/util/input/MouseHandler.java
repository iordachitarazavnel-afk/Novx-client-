package foure.dev.util.input;

import foure.dev.FourEClient;
import foure.dev.module.impl.render.EditHudModule;
import foure.dev.ui.clickgui.NovxClickGui;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

public class MouseHandler {
   private static boolean leftPressed = false;
   private static boolean rightPressed = false;
   private static double lastMouseX = 0.0D;
   private static double lastMouseY = 0.0D;

   public static void handleMouse() {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getWindow() != null) {
         NovxClickGui clickGui = (NovxClickGui)FourEClient.getInstance().getFunctionManager().getModule(NovxClickGui.class);
         EditHudModule editHud = (EditHudModule)FourEClient.getInstance().getFunctionManager().getModule(EditHudModule.class);
         boolean isGuiOpen = clickGui != null && clickGui.isOpen();
         boolean isHudEditOpen = editHud != null && editHud.isOpen();
         if (isGuiOpen || isHudEditOpen) {
            long window = mc.getWindow().getHandle();
            double mouseX = 0.0D;
            double mouseY = 0.0D;

            try {
               double[] xpos = new double[1];
               double[] ypos = new double[1];
               GLFW.glfwGetCursorPos(window, xpos, ypos);
               mouseX = xpos[0] * (double)mc.getWindow().getScaledWidth() / (double)mc.getWindow().getWidth();
               mouseY = ypos[0] * (double)mc.getWindow().getScaledHeight() / (double)mc.getWindow().getHeight();
            } catch (Exception var13) {
               return;
            }

            boolean leftDown = GLFW.glfwGetMouseButton(window, 0) == 1;
            boolean rightDown = GLFW.glfwGetMouseButton(window, 1) == 1;
            if (leftDown && !leftPressed) {
               if (isGuiOpen) {
                  clickGui.handleMouseClick(mouseX, mouseY, 0);
               }

               if (isHudEditOpen) {
                  editHud.handleMouseClick(mouseX, mouseY, 0);
               }

               leftPressed = true;
            } else if (!leftDown && leftPressed) {
               if (isGuiOpen) {
                  clickGui.handleMouseRelease(mouseX, mouseY, 0);
               }

               if (isHudEditOpen) {
                  editHud.handleMouseRelease(mouseX, mouseY, 0);
               }

               leftPressed = false;
            }

            if (rightDown && !rightPressed) {
               if (isGuiOpen) {
                  clickGui.handleMouseClick(mouseX, mouseY, 1);
               }

               if (isHudEditOpen) {
                  editHud.handleMouseClick(mouseX, mouseY, 1);
               }

               rightPressed = true;
            } else if (!rightDown && rightPressed) {
               if (isGuiOpen) {
                  clickGui.handleMouseRelease(mouseX, mouseY, 1);
               }

               if (isHudEditOpen) {
                  editHud.handleMouseRelease(mouseX, mouseY, 1);
               }

               rightPressed = false;
            }

            if (leftPressed && (mouseX != lastMouseX || mouseY != lastMouseY)) {
               if (isGuiOpen) {
                  clickGui.handleMouseDrag(mouseX, mouseY, 0, mouseX - lastMouseX, mouseY - lastMouseY);
               }

               if (isHudEditOpen) {
                  editHud.handleMouseDrag(mouseX, mouseY, 0, mouseX - lastMouseX, mouseY - lastMouseY);
               }
            }

            lastMouseX = mouseX;
            lastMouseY = mouseY;
         }
      }
   }
}
