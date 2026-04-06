package foure.dev.util.render.text;

import foure.dev.FourEClient;
import foure.dev.event.impl.render.TextFactoryEvent;
import foure.dev.util.render.backends.gl.GlBackend;
import foure.dev.util.render.utils.ColorUtils;
import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;
import java.util.Objects;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class TextRenderer {
   private static final float[] IDENTITY_TRANSFORM = new float[]{1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
   private final GlBackend backend;
   private final MsdfFont font;
   private static final Char2IntArrayMap COLOR_CODES = new Char2IntArrayMap() {
      {
         this.put('0', -16777216);
         this.put('1', -16777046);
         this.put('2', -16733696);
         this.put('3', -16733526);
         this.put('4', -5636096);
         this.put('5', -5635926);
         this.put('6', -22016);
         this.put('7', -5592406);
         this.put('8', -11184811);
         this.put('9', -11184641);
         this.put('a', -11141291);
         this.put('b', -11141121);
         this.put('c', -43691);
         this.put('d', -43521);
         this.put('e', -171);
         this.put('f', -1);
      }
   };

   public TextRenderer(GlBackend backend, MsdfFont font) {
      this.backend = (GlBackend)Objects.requireNonNull(backend, "backend");
      this.font = (MsdfFont)Objects.requireNonNull(font, "font");
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul) {
      String processedText = this.processTextThroughEvent(text);
      this.drawText(x, y, size, processedText, rgbaPremul, "l", IDENTITY_TRANSFORM);
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, float[] transform) {
      String processedText = this.processTextThroughEvent(text);
      this.drawText(x, y, size, processedText, rgbaPremul, "l", transform);
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey) {
      String processedText = this.processTextThroughEvent(text);
      this.drawText(x, y, size, processedText, rgbaPremul, alignKey, IDENTITY_TRANSFORM);
   }

   public void drawText(Text text, double x, double y, float size) {
      StringBuilder sb = new StringBuilder();
      this.findStyle(sb, text);
      String processedText = this.processTextThroughEvent(sb.toString());
      this.drawText((float)x, (float)y, size, processedText, ColorUtils.getText());
   }

   public void drawText(float x, float y, float size, String text, int rgbaPremul, String alignKey, float[] transform) {
      if (!(size <= 0.0F)) {
         String content = text == null ? "" : text;
         if (!content.isEmpty()) {
            float[] matrix = transform != null && transform.length >= 6 ? transform : IDENTITY_TRANSFORM;
            float scale = size / Math.max(1.0E-6F, this.font.emSize());
            float lineHeight = this.font.lineHeight() * scale;
            float baselineY = y;
            String align = alignKey == null ? "l" : alignKey.toLowerCase();
            int defaultColor = rgbaPremul;
            int texture = this.font.textureId();
            float pxRange = this.font.distanceRange();
            String[] lines = content.split("\\n", -1);
            String[] var18 = lines;
            int var19 = lines.length;

            for(int var20 = 0; var20 < var19; ++var20) {
               String line = var18[var20];
               float width = this.measureLineWidth(line, scale);
               float startX = x;
               if (!"c".equals(align) && !"center".equals(align)) {
                  if ("r".equals(align) || "right".equals(align)) {
                     startX = x - width;
                  }
               } else {
                  startX = x - width * 0.5F;
               }

               this.drawTextLineWithColors(startX, baselineY, scale, line, defaultColor, matrix, texture, pxRange);
               baselineY += lineHeight;
            }

         }
      }
   }

   private void drawTextLine(float x, float baseline, float scale, String line, int color, float[] matrix, int texture, float pxRange) {
      if (!line.isEmpty()) {
         float penX = x;
         float baselineY = baseline;
         int prevCodepoint = -1;
         int i = 0;

         while(true) {
            while(i < line.length()) {
               char ch = line.charAt(i);
               if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
                  i += 10;
               } else {
                  int cp = line.codePointAt(i);
                  int cpLen = Character.charCount(cp);
                  i += cpLen;
                  MsdfFont.Glyph glyph = this.font.glyph(cp);
                  int glyphCode = cp;
                  if (glyph == null) {
                     glyph = this.font.glyph(63);
                     glyphCode = 63;
                     if (glyph == null) {
                        continue;
                     }
                  }

                  if (prevCodepoint != -1) {
                     penX += this.font.kerning(prevCodepoint, glyphCode) * scale;
                  }

                  if (glyph.renderable) {
                     float x0 = penX + glyph.planeLeft * scale;
                     float y0 = baselineY - glyph.planeTop * scale;
                     float x1 = penX + glyph.planeRight * scale;
                     float y1 = baselineY - glyph.planeBottom * scale;
                     float width = x1 - x0;
                     float height = y1 - y0;
                     if (width > 0.0F && height > 0.0F) {
                        this.backend.enqueueMsdfGlyph(texture, pxRange, x0, y0, width, height, glyph.u0, glyph.v1, glyph.u1, glyph.v0, color, matrix);
                     }
                  }

                  penX += glyph.advance * scale;
                  prevCodepoint = glyphCode;
               }
            }

            return;
         }
      }
   }

   private void drawTextLineWithColors(float x, float baseline, float scale, String line, int defaultColor, float[] matrix, int texture, float pxRange) {
      if (!line.isEmpty()) {
         float penX = x;
         float baselineY = baseline;
         int prevCodepoint = -1;
         int currentColor = defaultColor;
         StringBuilder colorBuffer = new StringBuilder();
         boolean colorFormat = false;
         boolean customColorFormat = false;
         int i = 0;

         while(true) {
            while(i < line.length()) {
               char ch = line.charAt(i);
               if (ch == 167 && i + 1 < line.length()) {
                  colorFormat = true;
                  ++i;
               } else if (colorFormat) {
                  colorFormat = false;
                  char colorCode = Character.toLowerCase(line.charAt(i));
                  if (COLOR_CODES.containsKey(colorCode)) {
                     currentColor = COLOR_CODES.get(colorCode);
                  } else if (colorCode == 'r') {
                     currentColor = defaultColor;
                  }

                  ++i;
               } else {
                  int customColor;
                  if (ch == 9167) {
                     if (customColorFormat) {
                        try {
                           customColor = Integer.parseInt(colorBuffer.toString());
                           currentColor = -16777216 | customColor;
                        } catch (NumberFormatException var28) {
                        }

                        colorBuffer.setLength(0);
                     }

                     customColorFormat = !customColorFormat;
                     ++i;
                  } else if (customColorFormat) {
                     colorBuffer.append(ch);
                     ++i;
                  } else {
                     customColor = line.codePointAt(i);
                     int cpLen = Character.charCount(customColor);
                     i += cpLen;
                     MsdfFont.Glyph glyph = this.font.glyph(customColor);
                     int glyphCode = customColor;
                     if (glyph == null) {
                        glyph = this.font.glyph(63);
                        glyphCode = 63;
                        if (glyph == null) {
                           continue;
                        }
                     }

                     if (prevCodepoint != -1) {
                        penX += this.font.kerning(prevCodepoint, glyphCode) * scale;
                     }

                     if (glyph.renderable) {
                        float x0 = penX + glyph.planeLeft * scale;
                        float y0 = baselineY - glyph.planeTop * scale;
                        float x1 = penX + glyph.planeRight * scale;
                        float y1 = baselineY - glyph.planeBottom * scale;
                        float width = x1 - x0;
                        float height = y1 - y0;
                        if (width > 0.0F && height > 0.0F) {
                           this.backend.enqueueMsdfGlyph(texture, pxRange, x0, y0, width, height, glyph.u0, glyph.v1, glyph.u1, glyph.v0, currentColor, matrix);
                        }
                     }

                     penX += glyph.advance * scale;
                     prevCodepoint = glyphCode;
                  }
               }
            }

            return;
         }
      }
   }

   public TextRenderer.TextMetrics measureText(String text, float size) {
      String processedText = this.processTextThroughEvent(text);
      if (size <= 0.0F) {
         return new TextRenderer.TextMetrics(0.0F, 0.0F);
      } else {
         String content = processedText == null ? "" : processedText;
         if (content.isEmpty()) {
            return new TextRenderer.TextMetrics(0.0F, 0.0F);
         } else {
            float scale = size / Math.max(1.0E-6F, this.font.emSize());
            float lineHeight = this.font.lineHeight() * scale;
            String[] lines = content.split("\\n", -1);
            float maxWidth = 0.0F;
            String[] var9 = lines;
            int var10 = lines.length;

            for(int var11 = 0; var11 < var10; ++var11) {
               String line = var9[var11];
               maxWidth = Math.max(maxWidth, this.measureLineWidth(line, scale));
            }

            float height = Math.max(lineHeight * (float)lines.length, lineHeight);
            return new TextRenderer.TextMetrics(maxWidth, height);
         }
      }
   }

   private String processTextThroughEvent(String text) {
      if (text != null && !text.isEmpty()) {
         try {
            TextFactoryEvent event = new TextFactoryEvent(text);
            FourEClient.getInstance().getEventBus().post(event);
            return event.getText();
         } catch (Exception var3) {
            return text;
         }
      } else {
         return text;
      }
   }

   private float measureLineWidth(String line, float scale) {
      if (line.isEmpty()) {
         return 0.0F;
      } else {
         float penX = 0.0F;
         int prevCodepoint = -1;
         StringBuilder colorBuffer = new StringBuilder();
         boolean colorFormat = false;
         boolean customColorFormat = false;
         int i = 0;

         while(true) {
            while(i < line.length()) {
               char ch = line.charAt(i);
               if (ch == '\\' && i + 9 < line.length() && line.charAt(i + 1) == 'c') {
                  i += 10;
               } else if (ch == 167 && i + 1 < line.length()) {
                  colorFormat = true;
                  ++i;
               } else if (colorFormat) {
                  colorFormat = false;
                  ++i;
               } else if (ch == 9167) {
                  if (customColorFormat) {
                     colorBuffer.setLength(0);
                  }

                  customColorFormat = !customColorFormat;
                  ++i;
               } else if (customColorFormat) {
                  colorBuffer.append(ch);
                  ++i;
               } else {
                  int cp = line.codePointAt(i);
                  int cpLen = Character.charCount(cp);
                  i += cpLen;
                  MsdfFont.Glyph glyph = this.font.glyph(cp);
                  int glyphCode = cp;
                  if (glyph == null) {
                     glyph = this.font.glyph(63);
                     glyphCode = 63;
                     if (glyph == null) {
                        continue;
                     }
                  }

                  if (prevCodepoint != -1) {
                     penX += this.font.kerning(prevCodepoint, glyphCode) * scale;
                  }

                  penX += glyph.advance * scale;
                  prevCodepoint = glyphCode;
               }
            }

            return penX;
         }
      }
   }

   public void drawCenteredText(float x, float y, float size, String text, int color) {
      String processedText = this.processTextThroughEvent(text);
      TextRenderer.TextMetrics metrics = this.measureText(processedText, size);
      this.drawText(x - metrics.width() / 2.0F, y, size, processedText, color);
   }

   public void drawGradientText(float x, float y, float size, String text, int colorStart, int colorEnd) {
      if (text != null && !text.isEmpty()) {
         String processedText = this.processTextThroughEvent(text);
         String cleanText = this.removeColorCodes(processedText);
         int length = cleanText.length();
         StringBuilder gradientText = new StringBuilder();

         for(int i = 0; i < length; ++i) {
            float progress = length > 1 ? (float)i / (float)(length - 1) : 0.0F;
            int color = this.interpolateColor(colorStart, colorEnd, progress);
            gradientText.append(ColorUtils.formatting(color)).append(cleanText.charAt(i));
         }

         this.drawText(x, y, size, gradientText.toString(), colorStart);
      }
   }

   public void findStyle(StringBuilder sb, Text component) {
      Style style = component.getStyle();
      if (component.getSiblings().isEmpty()) {
         if (style.getColor() != null) {
            sb.append(ColorUtils.formatting(style.getColor().getRgb()));
         }

         sb.append(component.getString()).append(Formatting.RESET);
      } else {
         component.getWithStyle(style).forEach((text) -> {
            this.findStyle(sb, text);
         });
      }

   }

   public String removeColorCodes(String text) {
      if (text != null && !text.isEmpty()) {
         StringBuilder result = new StringBuilder();
         boolean colorFormat = false;
         boolean customColorFormat = false;
         StringBuilder colorBuffer = new StringBuilder();

         for(int i = 0; i < text.length(); ++i) {
            char ch = text.charAt(i);
            if (ch == 167 && i + 1 < text.length()) {
               colorFormat = true;
            } else if (colorFormat) {
               colorFormat = false;
            } else if (ch == 9167) {
               if (customColorFormat) {
                  colorBuffer.setLength(0);
               }

               customColorFormat = !customColorFormat;
            } else if (customColorFormat) {
               colorBuffer.append(ch);
            } else {
               result.append(ch);
            }
         }

         return result.toString();
      } else {
         return text;
      }
   }

   private int interpolateColor(int colorStart, int colorEnd, float t) {
      float startAlpha = (float)(colorStart >> 24 & 255) / 255.0F;
      float startRed = (float)(colorStart >> 16 & 255) / 255.0F;
      float startGreen = (float)(colorStart >> 8 & 255) / 255.0F;
      float startBlue = (float)(colorStart & 255) / 255.0F;
      float endAlpha = (float)(colorEnd >> 24 & 255) / 255.0F;
      float endRed = (float)(colorEnd >> 16 & 255) / 255.0F;
      float endGreen = (float)(colorEnd >> 8 & 255) / 255.0F;
      float endBlue = (float)(colorEnd & 255) / 255.0F;
      float alpha = startAlpha + t * (endAlpha - startAlpha);
      float red = startRed + t * (endRed - startRed);
      float green = startGreen + t * (endGreen - startGreen);
      float blue = startBlue + t * (endBlue - startBlue);
      return (int)(alpha * 255.0F) << 24 | (int)(red * 255.0F) << 16 | (int)(green * 255.0F) << 8 | (int)(blue * 255.0F);
   }

   public static record TextMetrics(float width, float height) {
      public TextMetrics(float width, float height) {
         this.width = width;
         this.height = height;
      }

      public float width() {
         return this.width;
      }

      public float height() {
         return this.height;
      }
   }
}
