package foure.dev.util.render.utils;

public class Color {
   public int interpolateRGB(int start, int end, double factor) {
      if (factor < 0.0D) {
         factor = 0.0D;
      }

      if (factor > 1.0D) {
         factor = 1.0D;
      }

      int a1 = start >> 24 & 255;
      int r1 = start >> 16 & 255;
      int g1 = start >> 8 & 255;
      int b1 = start & 255;
      int a2 = end >> 24 & 255;
      int r2 = end >> 16 & 255;
      int g2 = end >> 8 & 255;
      int b2 = end & 255;
      if (a1 == 0) {
         a1 = 255;
      }

      if (a2 == 0) {
         a2 = 255;
      }

      int r = (int)Math.round((double)r1 + (double)(r2 - r1) * factor);
      int g = (int)Math.round((double)g1 + (double)(g2 - g1) * factor);
      int b = (int)Math.round((double)b1 + (double)(b2 - b1) * factor);
      int a = (int)Math.round((double)a1 + (double)(a2 - a1) * factor);
      return this.getRGB(r, g, b, a);
   }

   public int getRGB(int r, int g, int b) {
      return this.getRGB(r, g, b, 255);
   }

   public static int getRGB(int hex, double alpha) {
      int a = (int)Math.round(alpha * 255.0D);
      int rgb = hex & 16777215;
      return a << 24 | rgb;
   }

   public int getRGB(int r, int g, int b, int a) {
      return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
   }

   public static int fromRgba(int r, int g, int b, int a) {
      return (new Color()).getRGB(r, g, b, a);
   }

   public static int alpha(int rgba) {
      return rgba >>> 24 & 255;
   }

   public static int red(int rgba) {
      return rgba >>> 16 & 255;
   }

   public static int green(int rgba) {
      return rgba >>> 8 & 255;
   }

   public static int blue(int rgba) {
      return rgba & 255;
   }

   public static int lerp(int startColor, int endColor, float t) {
      if (t <= 0.0F) {
         return startColor;
      } else if (t >= 1.0F) {
         return endColor;
      } else {
         int a1 = startColor >>> 24 & 255;
         int r1 = startColor >>> 16 & 255;
         int g1 = startColor >>> 8 & 255;
         int b1 = startColor & 255;
         int a2 = endColor >>> 24 & 255;
         int r2 = endColor >>> 16 & 255;
         int g2 = endColor >>> 8 & 255;
         int b2 = endColor & 255;
         int a = Math.round((float)a1 + (float)(a2 - a1) * t);
         int r = Math.round((float)r1 + (float)(r2 - r1) * t);
         int g = Math.round((float)g1 + (float)(g2 - g1) * t);
         int b = Math.round((float)b1 + (float)(b2 - b1) * t);
         return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | b & 255;
      }
   }
}
