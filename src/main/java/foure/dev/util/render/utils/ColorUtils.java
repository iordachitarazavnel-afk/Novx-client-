package foure.dev.util.render.utils;

import foure.dev.util.wrapper.Wrapper;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import lombok.Generated;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4i;

public final class ColorUtils implements Wrapper {
   private static final long CACHE_EXPIRATION_TIME = 60000L;
   private static final ConcurrentHashMap<ColorUtils.ColorKey, ColorUtils.CacheEntry> colorCache = new ConcurrentHashMap();
   private static final ScheduledExecutorService cacheCleaner = Executors.newScheduledThreadPool(1);
   private static final DelayQueue<ColorUtils.CacheEntry> cleanupQueue = new DelayQueue();
   public static final Pattern FORMATTING_CODE_PATTERN = Pattern.compile("(?i)§[0-9a-f-or]");
   public static Char2IntArrayMap colorCodes = new Char2IntArrayMap() {
      {
         this.put('0', 0);
         this.put('1', 170);
         this.put('2', 43520);
         this.put('3', 43690);
         this.put('4', 11141120);
         this.put('5', 11141290);
         this.put('6', 16755200);
         this.put('7', 11184810);
         this.put('8', 5592405);
         this.put('9', 5592575);
         this.put('A', 5635925);
         this.put('B', 5636095);
         this.put('C', 16733525);
         this.put('D', 16733695);
         this.put('E', 16777045);
         this.put('F', 16777215);
      }
   };
   public static final int RED;
   public static final int GREEN;
   public static final int BLUE;
   public static final int YELLOW;
   public static final int WHITE;
   public static final int BLACK;
   public static final int HALF_BLACK;
   public static final int LIGHT_RED;

   public static float[] getRGBa(int color) {
      return new float[]{(float)(color >> 16 & 255) / 255.0F, (float)(color >> 8 & 255) / 255.0F, (float)(color & 255) / 255.0F, (float)(color >> 24 & 255) / 255.0F};
   }

   public static int rgba(int r, int g, int b, int a) {
      return a << 24 | r << 16 | g << 8 | b;
   }

   public static int red(int c) {
      return c >> 16 & 255;
   }

   public static int green(int c) {
      return c >> 8 & 255;
   }

   public static int blue(int c) {
      return c & 255;
   }

   public static int alpha(int c) {
      return c >> 24 & 255;
   }

   public static float redf(int c) {
      return (float)red(c) / 255.0F;
   }

   public static float greenf(int c) {
      return (float)green(c) / 255.0F;
   }

   public static float bluef(int c) {
      return (float)blue(c) / 255.0F;
   }

   public static float alphaf(int c) {
      return (float)alpha(c) / 255.0F;
   }

   public static int[] getRGBA(int c) {
      return new int[]{red(c), green(c), blue(c), alpha(c)};
   }

   public static int[] getRGB(int c) {
      return new int[]{red(c), green(c), blue(c)};
   }

   public static float[] getRGBAf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c), alphaf(c)};
   }

   public static float[] getRGBf(int c) {
      return new float[]{redf(c), greenf(c), bluef(c)};
   }

   public static int getColor(float red, float green, float blue, float alpha) {
      return getColor(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F));
   }

   public static int getColor(int red, int green, int blue, float alpha) {
      return getColor(red, green, blue, Math.round(alpha * 255.0F));
   }

   public static int getColor(float red, float green, float blue) {
      return getColor(red, green, blue, 1.0F);
   }

   public static int getColor(int brightness, int alpha) {
      return getColor(brightness, brightness, brightness, alpha);
   }

   public static int getColor(int brightness, float alpha) {
      return getColor(brightness, Math.round(alpha * 255.0F));
   }

   public static int getColor(int brightness) {
      return getColor(brightness, brightness, brightness);
   }

   public static int replAlpha(int color, int alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int replAlpha(int color, float alpha) {
      return getColor(red(color), green(color), blue(color), alpha);
   }

   public static int multAlpha(int color, float percent01) {
      return getColor(red(color), green(color), blue(color), Math.round((float)alpha(color) * percent01));
   }

   public static int multColor(int colorStart, int colorEnd, float progress) {
      return getColor(Math.round((float)red(colorStart) * redf(colorEnd) * progress), Math.round((float)green(colorStart) * greenf(colorEnd) * progress), Math.round((float)blue(colorStart) * bluef(colorEnd) * progress), Math.round((float)alpha(colorStart) * alphaf(colorEnd) * progress));
   }

   public static int multRed(int colorStart, int colorEnd, float progress) {
      return getColor(Math.round((float)red(colorStart) * redf(colorEnd) * progress), Math.round((float)green(colorStart) * greenf(colorEnd) * progress), Math.round((float)blue(colorStart) * bluef(colorEnd) * progress), Math.round((float)alpha(colorStart) * alphaf(colorEnd) * progress));
   }

   public static int multDark(int color, float percent01) {
      return getColor(Math.round((float)red(color) * percent01), Math.round((float)green(color) * percent01), Math.round((float)blue(color) * percent01), alpha(color));
   }

   public static int multBright(int color, float percent01) {
      return getColor(Math.min(255, Math.round((float)red(color) / percent01)), Math.min(255, Math.round((float)green(color) / percent01)), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int overCol(int color1, int color2, float percent01) {
      float percent = MathHelper.clamp(percent01, 0.0F, 1.0F);
      return getColor(MathHelper.lerp(percent, red(color1), red(color2)), MathHelper.lerp(percent, green(color1), green(color2)), MathHelper.lerp(percent, blue(color1), blue(color2)), MathHelper.lerp(percent, alpha(color1), alpha(color2)));
   }

   public static Vector4i multRedAndAlpha(Vector4i color, float red, float alpha) {
      return new Vector4i(multRedAndAlpha(color.x, red, alpha), multRedAndAlpha(color.y, red, alpha), multRedAndAlpha(color.w, red, alpha), multRedAndAlpha(color.z, red, alpha));
   }

   public static int multRedAndAlpha(int color, float red, float alpha) {
      return getColor(red(color), Math.min(255, Math.round((float)green(color) / red)), Math.min(255, Math.round((float)blue(color) / red)), Math.round((float)alpha(color) * alpha));
   }

   public static int multRed(int color, float percent01) {
      return getColor(red(color), Math.min(255, Math.round((float)green(color) / percent01)), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int multGreen(int color, float percent01) {
      return getColor(Math.min(255, Math.round((float)green(color) / percent01)), green(color), Math.min(255, Math.round((float)blue(color) / percent01)), alpha(color));
   }

   public static int pack(int red, int green, int blue, int alpha) {
      return (alpha & 255) << 24 | (red & 255) << 16 | (green & 255) << 8 | (blue & 255) << 0;
   }

   public static int[] genGradientForText(int color1, int color2, int length) {
      int[] gradient = new int[length];

      for(int i = 0; i < length; ++i) {
         float pc = (float)i / (float)(length - 1);
         gradient[i] = overCol(color1, color2, pc);
      }

      return gradient;
   }

   public static float[] normalize(java.awt.Color color) {
      return new float[]{(float)color.getRed() / 255.0F, (float)color.getGreen() / 255.0F, (float)color.getBlue() / 255.0F, (float)color.getAlpha() / 255.0F};
   }

   public static float[] normalize(int color) {
      int[] components = unpack(color);
      return new float[]{(float)components[0] / 255.0F, (float)components[1] / 255.0F, (float)components[2] / 255.0F, (float)components[3] / 255.0F};
   }

   public static int applyAlpha(int color, float alpha) {
      int r = color >> 16 & 255;
      int g = color >> 8 & 255;
      int b = color & 255;
      return pack(r, g, b, (int)(255.0F * alpha));
   }

   public static int[] unpack(int color) {
      return new int[]{color >> 16 & 255, color >> 8 & 255, color & 255, color >> 24 & 255};
   }

   public static int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      float hue = (float)angle / 360.0F;
      int color = java.awt.Color.HSBtoRGB(hue, saturation, brightness);
      return getColor(red(color), green(color), blue(color), Math.round(opacity * 255.0F));
   }

   public static int fade(int index) {
      java.awt.Color clientColor = new java.awt.Color(getClientColor());
      return fade(8, index, clientColor.brighter().getRGB(), clientColor.darker().getRGB());
   }

   public static int fade(int speed, int index, int color1, int color2) {
      int angle = (int)((System.currentTimeMillis() / (long)speed + (long)index) % 360L);
      angle = angle >= 180 ? 360 - angle : angle;
      return overCol(color1, color2, (float)angle / 180.0F);
   }

   public static int fade(int index, int color1, int color2) {
      return fade(8, index, color1, color2);
   }

   public static int fade(int speed, int index, java.awt.Color color1, java.awt.Color color2) {
      return fade(speed, index, color1.getRGB(), color2.getRGB());
   }

   public static int fade(int index, java.awt.Color color1, java.awt.Color color2) {
      return fade(8, index, color1.getRGB(), color2.getRGB());
   }

   public static Vector4i roundClientColor(float alpha) {
      return new Vector4i(multAlpha(fade(270), alpha), multAlpha(fade(0), alpha), multAlpha(fade(180), alpha), multAlpha(fade(90), alpha));
   }

   public static int getColor(int red, int green, int blue, int alpha) {
      ColorUtils.ColorKey key = new ColorUtils.ColorKey(red, green, blue, alpha);
      ColorUtils.CacheEntry cacheEntry = (ColorUtils.CacheEntry)colorCache.computeIfAbsent(key, (k) -> {
         ColorUtils.CacheEntry newEntry = new ColorUtils.CacheEntry(k, computeColor(red, green, blue, alpha), 60000L);
         cleanupQueue.offer(newEntry);
         return newEntry;
      });
      return cacheEntry.getColor();
   }

   public static int getColor(int red, int green, int blue) {
      return getColor(red, green, blue, 255);
   }

   private static int computeColor(int red, int green, int blue, int alpha) {
      return MathHelper.clamp(alpha, 0, 255) << 24 | MathHelper.clamp(red, 0, 255) << 16 | MathHelper.clamp(green, 0, 255) << 8 | MathHelper.clamp(blue, 0, 255);
   }

   private static String generateKey(int red, int green, int blue, int alpha) {
      return red + "," + green + "," + blue + "," + alpha;
   }

   public static String formatting(int color) {
      return "⏏" + color + "⏏";
   }

   public static String removeFormatting(String text) {
      return text != null && !text.isEmpty() ? FORMATTING_CODE_PATTERN.matcher(text).replaceAll("") : null;
   }

   public static int getMainGuiColor() {
      return (new java.awt.Color(1579037)).getRGB();
   }

   public static int getGuiRectColor(float alpha) {
      return multAlpha((new java.awt.Color(1710623)).getRGB(), alpha);
   }

   public static int getGuiRectColor2(float alpha) {
      return multAlpha((new java.awt.Color(1973798)).getRGB(), alpha);
   }

   public static int getRect(float alpha) {
      return multAlpha((new java.awt.Color(1579036)).getRGB(), alpha);
   }

   public static int getRectDarker(float alpha) {
      return multAlpha((new java.awt.Color(1579038)).getRGB(), alpha);
   }

   public static int getText(float alpha) {
      return multAlpha(getText(), alpha);
   }

   public static int getText() {
      return (new java.awt.Color(15132390)).getRGB();
   }

   public static int getClientColor() {
      return (new java.awt.Color(1864934)).getRGB();
   }

   public static int getClientColor(float alpha) {
      return multAlpha(getClientColor(), alpha);
   }

   public static int getFriendColor() {
      return (new java.awt.Color(5635925)).getRGB();
   }

   public static int getOutline(float alpha, float bright) {
      return multBright(multAlpha(getOutline(), alpha), bright);
   }

   public static int getOutline(float alpha) {
      return multAlpha(getOutline(), alpha);
   }

   public static int getOutline() {
      return (new java.awt.Color(3618630)).getRGB();
   }

   public static java.awt.Color alpha(java.awt.Color color, int alpha) {
      alpha = Math.max(0, Math.min(255, alpha));
      return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public static int applyOpacity(int color, float opacity) {
      java.awt.Color old = new java.awt.Color(color);
      return applyOpacity(old, opacity).getRGB();
   }

   public static java.awt.Color applyOpacity(java.awt.Color color, float opacity) {
      opacity = Math.min(1.0F, Math.max(0.0F, opacity));
      int alpha = (int)((float)color.getAlpha() * opacity);
      return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   public static java.awt.Color fade(java.awt.Color color1, java.awt.Color color2, float alpha) {
      alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      int r = (int)((float)color1.getRed() * (1.0F - alpha) + (float)color2.getRed() * alpha);
      int g = (int)((float)color1.getGreen() * (1.0F - alpha) + (float)color2.getGreen() * alpha);
      int b = (int)((float)color1.getBlue() * (1.0F - alpha) + (float)color2.getBlue() * alpha);
      int a = (int)((float)color1.getAlpha() * (1.0F - alpha) + (float)color2.getAlpha() * alpha);
      r = Math.max(0, Math.min(255, r));
      g = Math.max(0, Math.min(255, g));
      b = Math.max(0, Math.min(255, b));
      a = Math.max(0, Math.min(255, a));
      return new java.awt.Color(r, g, b, a);
   }

   public static java.awt.Color offset(java.awt.Color color, float alpha) {
      alpha = Math.max(0.0F, Math.min(1.0F, alpha));
      int newAlpha = (int)((float)color.getAlpha() * alpha);
      newAlpha = Math.max(0, Math.min(255, newAlpha));
      return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
   }

   public static java.awt.Color getGlobalColor(int alpha) {
      return new java.awt.Color(118, 86, 211, alpha);
   }

   public static java.awt.Color getGlobalColor() {
      return getGlobalColor(255);
   }

   public static java.awt.Color getGlobalColor1(float amount) {
      amount = Math.max(0.0F, Math.min(1.0F, amount));
      java.awt.Color lightPurple = new java.awt.Color(118, 86, 211);
      java.awt.Color darkPurple = new java.awt.Color(75, 82, 158);
      java.awt.Color color = gradient(darkPurple, lightPurple, amount);
      return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
   }

   public static java.awt.Color getGlobalColor1() {
      return getGlobalColor1(0.0F);
   }

   public static java.awt.Color gradient(java.awt.Color color1, java.awt.Color color2, float amount) {
      amount = Math.max(0.0F, Math.min(1.0F, amount));
      int r = (int)((float)color1.getRed() * (1.0F - amount) + (float)color2.getRed() * amount);
      int g = (int)((float)color1.getGreen() * (1.0F - amount) + (float)color2.getGreen() * amount);
      int b = (int)((float)color1.getBlue() * (1.0F - amount) + (float)color2.getBlue() * amount);
      int a = (int)((float)color1.getAlpha() * (1.0F - amount) + (float)color2.getAlpha() * amount);
      r = Math.max(0, Math.min(255, r));
      g = Math.max(0, Math.min(255, g));
      b = Math.max(0, Math.min(255, b));
      a = Math.max(0, Math.min(255, a));
      return new java.awt.Color(r, g, b, a);
   }

   public static java.awt.Color multiGradient(java.awt.Color[] colors, float progress) {
      if (colors != null && colors.length != 0) {
         if (colors.length == 1) {
            return colors[0];
         } else {
            progress = Math.max(0.0F, Math.min(1.0F, progress));
            float scaledProgress = progress * (float)(colors.length - 1);
            int index = (int)Math.floor((double)scaledProgress);
            float localProgress = scaledProgress - (float)index;
            return index >= colors.length - 1 ? colors[colors.length - 1] : fade(colors[index], colors[index + 1], localProgress);
         }
      } else {
         return java.awt.Color.WHITE;
      }
   }

   public static java.awt.Color fromHSV(float hue, float saturation, float value) {
      hue = (hue % 360.0F + 360.0F) % 360.0F;
      saturation = Math.max(0.0F, Math.min(1.0F, saturation));
      value = Math.max(0.0F, Math.min(1.0F, value));
      float c = value * saturation;
      float x = c * (1.0F - Math.abs(hue / 60.0F % 2.0F - 1.0F));
      float m = value - c;
      float r;
      float g;
      float b;
      if (hue < 60.0F) {
         r = c;
         g = x;
         b = 0.0F;
      } else if (hue < 120.0F) {
         r = x;
         g = c;
         b = 0.0F;
      } else if (hue < 180.0F) {
         r = 0.0F;
         g = c;
         b = x;
      } else if (hue < 240.0F) {
         r = 0.0F;
         g = x;
         b = c;
      } else if (hue < 300.0F) {
         r = x;
         g = 0.0F;
         b = c;
      } else {
         r = c;
         g = 0.0F;
         b = x;
      }

      int red = (int)((r + m) * 255.0F);
      int green = (int)((g + m) * 255.0F);
      int blue = (int)((b + m) * 255.0F);
      return new java.awt.Color(Math.max(0, Math.min(255, red)), Math.max(0, Math.min(255, green)), Math.max(0, Math.min(255, blue)));
   }

   public static String minecraftGradient(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int length = text.length();

      for(int i = 0; i < length; ++i) {
         float progress = (float)i / (float)(length - 1);
         java.awt.Color color = gradient(start, end, progress);
         sb.append(getNearestColorCode(color)).append(text.charAt(i));
      }

      return sb.toString();
   }

   public static String getLetterGradientText(java.awt.Color startColor, java.awt.Color endColor, String text) {
      StringBuilder result = new StringBuilder();
      int length = text.length();

      for(int i = 0; i < length; ++i) {
         float progress = (float)i / (float)(length - 1);
         java.awt.Color currentColor = gradient(startColor, endColor, progress);
         String colorCode = getNearestMinecraftColor(currentColor);
         result.append(colorCode).append(text.charAt(i));
      }

      return result.toString();
   }

   public static String getNearestMinecraftColor(java.awt.Color color) {
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      if (r < 30 && g < 30 && b < 30) {
         return "§0";
      } else if (r > 200 && g > 200 && b > 200) {
         return "§f";
      } else if (r > g && r > b) {
         return g > 150 ? "§6" : "§c";
      } else if (g > r && g > b) {
         return "§a";
      } else if (b > r && b > g) {
         return "§9";
      } else if (r > 200 && g > 200) {
         return "§e";
      } else if (r > 200 && b > 200) {
         return "§d";
      } else {
         return g > 200 && b > 200 ? "§b" : "§f";
      }
   }

   public static String getFullRGBGradient(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int length = text.length();

      for(int i = 0; i < length; ++i) {
         float progress = (float)i / (float)(length - 1);
         java.awt.Color color = gradient(start, end, progress);
         sb.append(getNearestMinecraftColor(color)).append(text.charAt(i));
      }

      return sb.toString();
   }

   public static String getUltraSmoothGradient(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int steps = text.length() * 2;

      for(int i = 0; i < text.length(); ++i) {
         float progress1 = (float)(i * 2) / (float)steps;
         java.awt.Color color1 = gradient(start, end, progress1);
         float progress2 = (float)(i * 2 + 1) / (float)steps;
         java.awt.Color color2 = gradient(start, end, progress2);
         java.awt.Color avgColor = new java.awt.Color((color1.getRed() + color2.getRed()) / 2, (color1.getGreen() + color2.getGreen()) / 2, (color1.getBlue() + color2.getBlue()) / 2);
         sb.append(getNearestMinecraftColor(avgColor)).append(text.charAt(i));
      }

      return sb.toString();
   }

   public static String getHexColor(java.awt.Color color) {
      return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
   }

   public static String getHexGradient(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int length = text.length();

      for(int i = 0; i < length; ++i) {
         float progress = (float)i / (float)(length - 1);
         java.awt.Color color = gradient(start, end, progress);
         sb.append("§x");
         String hex = getHexColor(color).substring(1);
         char[] var9 = hex.toCharArray();
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            char c = var9[var11];
            sb.append("§").append(c);
         }

         sb.append(text.charAt(i));
      }

      return sb.toString();
   }

   public static String getSmoothGradientText(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int steps = text.length() * 2 - 1;

      for(int i = 0; i < text.length(); ++i) {
         float progress1 = (float)(i * 2) / (float)steps;
         java.awt.Color color1 = gradient(start, end, progress1);
         float progress2 = (float)(i * 2 + 1) / (float)steps;
         java.awt.Color color2 = gradient(start, end, progress2);
         java.awt.Color avgColor = new java.awt.Color((color1.getRed() + color2.getRed()) / 2, (color1.getGreen() + color2.getGreen()) / 2, (color1.getBlue() + color2.getBlue()) / 2);
         sb.append(getNearestColorCode(avgColor)).append(text.charAt(i));
      }

      return sb.toString();
   }

   public static String getGradientText(java.awt.Color start, java.awt.Color end, String text) {
      StringBuilder sb = new StringBuilder();
      int length = text.length();

      for(int i = 0; i < length; ++i) {
         float progress = (float)i / (float)Math.max(1, length - 1);
         java.awt.Color color = gradient(start, end, progress);
         sb.append(getNearestColorCode(color)).append(text.charAt(i));
      }

      return sb.toString();
   }

   private static String getNearestColorCode(java.awt.Color color) {
      int r = color.getRed();
      int g = color.getGreen();
      int b = color.getBlue();
      if (r < 30 && g < 30 && b < 30) {
         return "§0";
      } else if (r > 200 && g < 50 && b < 50) {
         return "§c";
      } else if (r < 50 && g > 200 && b < 50) {
         return "§a";
      } else if (r < 50 && g < 50 && b > 200) {
         return "§9";
      } else if (r > 200 && g > 200 && b < 50) {
         return "§e";
      } else if (r > 200 && g < 50 && b > 200) {
         return "§d";
      } else if (r < 50 && g > 200 && b > 200) {
         return "§b";
      } else if (r > 200 && g > 200 && b > 200) {
         return "§f";
      } else {
         return r > 150 && g > 100 && b < 50 ? "§6" : "§f";
      }
   }

   public static java.awt.Color weightedGradient(java.awt.Color[] colors, float[] weights, float progress) {
      if (colors != null && weights != null && colors.length == weights.length && colors.length != 0) {
         progress = Math.max(0.0F, Math.min(1.0F, progress));
         float totalWeight = 0.0F;
         float[] var4 = weights;
         int i = weights.length;

         float localProgress;
         for(int var6 = 0; var6 < i; ++var6) {
            localProgress = var4[var6];
            totalWeight += localProgress;
         }

         if (totalWeight <= 0.0F) {
            return colors[0];
         } else {
            float accumulated = 0.0F;

            for(i = 0; i < weights.length; ++i) {
               float normalizedWeight = weights[i] / totalWeight;
               if (progress <= accumulated + normalizedWeight) {
                  localProgress = (progress - accumulated) / normalizedWeight;
                  localProgress = Math.max(0.0F, Math.min(1.0F, localProgress));
                  if (i == weights.length - 1) {
                     return colors[i];
                  }

                  return fade(colors[i], colors[i + 1], localProgress);
               }

               accumulated += normalizedWeight;
            }

            return colors[colors.length - 1];
         }
      } else {
         return java.awt.Color.WHITE;
      }
   }

   @Generated
   private ColorUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   static {
      cacheCleaner.scheduleWithFixedDelay(() -> {
         for(ColorUtils.CacheEntry entry = (ColorUtils.CacheEntry)cleanupQueue.poll(); entry != null; entry = (ColorUtils.CacheEntry)cleanupQueue.poll()) {
            if (entry.isExpired()) {
               colorCache.remove(entry.getKey());
            }
         }

      }, 0L, 1L, TimeUnit.SECONDS);
      RED = getColor(255, 0, 0);
      GREEN = getColor(0, 255, 0);
      BLUE = getColor(0, 0, 255);
      YELLOW = getColor(255, 255, 0);
      WHITE = getColor(255);
      BLACK = getColor(0);
      HALF_BLACK = getColor(0, 0.5F);
      LIGHT_RED = getColor(255, 85, 85);
   }

   private static class ColorKey {
      final int red;
      final int green;
      final int blue;
      final int alpha;

      @Generated
      public int getRed() {
         return this.red;
      }

      @Generated
      public int getGreen() {
         return this.green;
      }

      @Generated
      public int getBlue() {
         return this.blue;
      }

      @Generated
      public int getAlpha() {
         return this.alpha;
      }

      @Generated
      public ColorKey(int red, int green, int blue, int alpha) {
         this.red = red;
         this.green = green;
         this.blue = blue;
         this.alpha = alpha;
      }

      @Generated
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof ColorUtils.ColorKey)) {
            return false;
         } else {
            ColorUtils.ColorKey other = (ColorUtils.ColorKey)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.getRed() != other.getRed()) {
               return false;
            } else if (this.getGreen() != other.getGreen()) {
               return false;
            } else if (this.getBlue() != other.getBlue()) {
               return false;
            } else {
               return this.getAlpha() == other.getAlpha();
            }
         }
      }

      @Generated
      protected boolean canEqual(Object other) {
         return other instanceof ColorUtils.ColorKey;
      }

      @Generated
      public int hashCode() {
         int PRIME = 1;
         int result = 1;
         result = result * 59 + this.getRed();
         result = result * 59 + this.getGreen();
         result = result * 59 + this.getBlue();
         result = result * 59 + this.getAlpha();
         return result;
      }
   }

   private static class CacheEntry implements Delayed {
      private final ColorUtils.ColorKey key;
      private final int color;
      private final long expirationTime;

      CacheEntry(ColorUtils.ColorKey key, int color, long ttl) {
         this.key = key;
         this.color = color;
         this.expirationTime = System.currentTimeMillis() + ttl;
      }

      public long getDelay(TimeUnit unit) {
         long delay = this.expirationTime - System.currentTimeMillis();
         return unit.convert(delay, TimeUnit.MILLISECONDS);
      }

      public int compareTo(Delayed other) {
         return other instanceof ColorUtils.CacheEntry ? Long.compare(this.expirationTime, ((ColorUtils.CacheEntry)other).expirationTime) : 0;
      }

      public boolean isExpired() {
         return System.currentTimeMillis() > this.expirationTime;
      }

      @Generated
      public ColorUtils.ColorKey getKey() {
         return this.key;
      }

      @Generated
      public int getColor() {
         return this.color;
      }

      @Generated
      public long getExpirationTime() {
         return this.expirationTime;
      }
   }
}
