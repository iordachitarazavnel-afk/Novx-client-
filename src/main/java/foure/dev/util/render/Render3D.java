package foure.dev.util.render;

import foure.dev.util.render.backends.gl.Simple3DBackend;
import foure.dev.util.world.Dir;
import java.awt.Color;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Vector3d;

public class Render3D {
   private static final Simple3DBackend backend = new Simple3DBackend();
   private static double camX;
   private static double camY;
   private static double camZ;

   public static void setLineWidth(float width) {
      backend.setLineWidth(width);
   }

   public static void setDepthTest(boolean enabled) {
      backend.setDepthTest(enabled);
   }

   public static void begin(Camera camera) {
      camX = camera.getCameraPos().x;
      camY = camera.getCameraPos().y;
      camZ = camera.getCameraPos().z;
      backend.begin();
   }

   public static void end(Matrix4f view, Matrix4f proj) {
      backend.end(view, proj);
   }

   public static void drawBoxOutline(Box box, Color color) {
      drawBoxOutline(box, color, 0);
   }

   public static void drawBoxOutline(Box box, Color color, int excludeDir) {
      float minX = (float)(box.minX - camX);
      float minY = (float)(box.minY - camY);
      float minZ = (float)(box.minZ - camZ);
      float maxX = (float)(box.maxX - camX);
      float maxY = (float)(box.maxY - camY);
      float maxZ = (float)(box.maxZ - camZ);
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      if (excludeDir == 0) {
         backend.line(minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
         backend.line(maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
         backend.line(maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
         backend.line(minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
         backend.line(minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
         backend.line(maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
         backend.line(maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
         backend.line(minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
         backend.line(minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
         backend.line(maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
         backend.line(maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
         backend.line(minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
      } else {
         if (Dir.isNot(excludeDir, (byte)32) && Dir.isNot(excludeDir, (byte)8)) {
            backend.line(minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)32) && Dir.isNot(excludeDir, (byte)16)) {
            backend.line(minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)64) && Dir.isNot(excludeDir, (byte)8)) {
            backend.line(maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)64) && Dir.isNot(excludeDir, (byte)16)) {
            backend.line(maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)32) && Dir.isNot(excludeDir, (byte)4)) {
            backend.line(minX, minY, minZ, minX, minY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)64) && Dir.isNot(excludeDir, (byte)4)) {
            backend.line(maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)8) && Dir.isNot(excludeDir, (byte)4)) {
            backend.line(minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)16) && Dir.isNot(excludeDir, (byte)4)) {
            backend.line(minX, minY, maxZ, maxX, minY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)32) && Dir.isNot(excludeDir, (byte)2)) {
            backend.line(minX, maxY, minZ, minX, maxY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)64) && Dir.isNot(excludeDir, (byte)2)) {
            backend.line(maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)8) && Dir.isNot(excludeDir, (byte)2)) {
            backend.line(minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)16) && Dir.isNot(excludeDir, (byte)2)) {
            backend.line(minX, maxY, maxZ, maxX, maxY, maxZ, r, g, b, a);
         }
      }

   }

   public static void drawBoxFill(Box box, Color color) {
      drawBoxFill(box, color, 0);
   }

   public static void drawBoxFill(Box box, Color color, int excludeDir) {
      float minX = (float)(box.minX - camX);
      float minY = (float)(box.minY - camY);
      float minZ = (float)(box.minZ - camZ);
      float maxX = (float)(box.maxX - camX);
      float maxY = (float)(box.maxY - camY);
      float maxZ = (float)(box.maxZ - camZ);
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      if (excludeDir == 0) {
         backend.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
         backend.quad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
         backend.quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
         backend.quad(maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
         backend.quad(minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
         backend.quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
      } else {
         if (Dir.isNot(excludeDir, (byte)4)) {
            backend.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)2)) {
            backend.quad(minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, minX, maxY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)8)) {
            backend.quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)16)) {
            backend.quad(maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, minX, minY, maxZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)32)) {
            backend.quad(minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, minX, minY, minZ, r, g, b, a);
         }

         if (Dir.isNot(excludeDir, (byte)64)) {
            backend.quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
         }
      }

   }

   public static void drawTracer(Vector3d start, Box target, Color color) {
      double cx = target.minX + (target.maxX - target.minX) / 2.0D - camX;
      double cy = target.minY + (target.maxY - target.minY) / 2.0D - camY;
      double cz = target.minZ + (target.maxZ - target.minZ) / 2.0D - camZ;
      double sx = start.x - camX;
      double sy = start.y - camY;
      double sz = start.z - camZ;
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      backend.line((float)sx, (float)sy, (float)sz, (float)cx, (float)cy, (float)cz, r, g, b, a);
   }

   public static void debugNDC() {
      backend.debugNDC();
   }

   public static void drawCircle(double x, double y, double z, double radius, Color color) {
      float r = (float)color.getRed() / 255.0F;
      float g = (float)color.getGreen() / 255.0F;
      float b = (float)color.getBlue() / 255.0F;
      float a = (float)color.getAlpha() / 255.0F;
      int segments = 64;
      double increment = 6.283185307179586D / (double)segments;
      float cx = (float)(x - camX);
      float cy = (float)(y - camY);
      float cz = (float)(z - camZ);

      for(int i = 0; i < segments; ++i) {
         double angle1 = (double)i * increment;
         double angle2 = (double)(i + 1) * increment;
         float x1 = (float)((double)cx + Math.cos(angle1) * radius);
         float z1 = (float)((double)cz + Math.sin(angle1) * radius);
         float x2 = (float)((double)cx + Math.cos(angle2) * radius);
         float z2 = (float)((double)cz + Math.sin(angle2) * radius);
         backend.line(x1, cy, z1, x2, cy, z2, r, g, b, a);
      }

   }
}
