package foure.dev.ui.clickgui.elements;

import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;

public class ColorElement implements SettingElement {
   private final ColorSetting setting;
   private boolean expanded = false;
   private static final float PADDING = 6.0F;
   private static final float COMPONENT_GAP = 6.0F;
   private static final float HEADER_HEIGHT = 18.0F;
   private static final float SB_HEIGHT = 80.0F;
   private static final float SLIDER_HEIGHT = 10.0F;
   private static final float SLIDER_RADIUS = 4.0F;
   private static final float CORNER_RADIUS = 6.0F;
   private static final int CHECKER_LIGHT = -12829636;
   private static final int CHECKER_DARK = -14013910;
   private static final float CHECKER_SIZE = 4.0F;
   private static final float SLIDER_CURSOR_WIDTH = 4.0F;
   private static final int SLIDER_CURSOR_COLOR = -1;
   private boolean draggingSB = false;
   private boolean draggingHue = false;
   private boolean draggingAlpha = false;

   public ColorElement(ColorSetting setting) {
      this.setting = setting;
   }

   public void render(Renderer2D r, FontObject font, float x, float y, float width, float alpha) {
      int textAlpha = (int)(255.0F * alpha);
      r.text(font, x + 4.0F, y + 9.0F + 3.0F, 8.0F, this.setting.getName(), (new Color(255, 255, 255, textAlpha)).getRGB(), "l");
      float swatchSize = 12.0F;
      float swatchX = x + width - swatchSize - 4.0F;
      float swatchY = y + (18.0F - swatchSize) / 2.0F;
      this.renderCheckerboard(r, swatchX, swatchY, swatchSize, swatchSize, 4.0F, alpha);
      r.rect(swatchX, swatchY, swatchSize, swatchSize, 3.0F, this.applyAlpha(((Color)this.setting.getValue()).getRGB(), alpha));
      if (this.expanded) {
         float startY = y + 18.0F + 6.0F;
         float contentX = x + 6.0F;
         float contentWidth = width - 12.0F;
         this.renderSBPicker(r, contentX, startY, contentWidth, 80.0F, alpha);
         float currentY = startY + 80.0F + 6.0F;
         this.renderHueSlider(r, contentX, currentY, contentWidth, 10.0F, alpha);
         currentY += 16.0F;
         this.renderAlphaSlider(r, contentX, currentY, contentWidth, 10.0F, alpha);
      }
   }

   private void renderHueSlider(Renderer2D r, float x, float y, float w, float h, float alpha) {
      r.pushRoundedClipRect(x, y, w, h, 4.0F, 4.0F, 4.0F, 4.0F);
      float[] hueStops = new float[]{0.0F, 0.16666667F, 0.33333334F, 0.5F, 0.6666667F, 0.8333333F, 1.0F};

      for(int i = 0; i < hueStops.length - 1; ++i) {
         float h1 = hueStops[i];
         float h2 = hueStops[i + 1];
         int c1 = this.applyAlpha(Color.getHSBColor(h1, 1.0F, 1.0F).getRGB(), alpha);
         int c2 = this.applyAlpha(Color.getHSBColor(h2, 1.0F, 1.0F).getRGB(), alpha);
         float xPos = x + w * h1;
         float nextXPos = x + w * h2;
         float segWidth = nextXPos - xPos + 1.0F;
         r.gradient(xPos, y, segWidth, h, 0.0F, 0.0F, 0.0F, 0.0F, c1, c2, c2, c1);
      }

      r.popClipRect();
      float huePos = this.setting.getHsb()[0] * w;
      this.renderSliderCursor(r, x + huePos, y + h / 2.0F, alpha);
   }

   private void renderSBPicker(Renderer2D r, float x, float y, float w, float h, float alpha) {
      int colorHue = this.applyAlpha(Color.getHSBColor(this.setting.getHsb()[0], 1.0F, 1.0F).getRGB(), alpha);
      int colorWhite = this.applyAlpha(-1, alpha);
      int colorBlack = this.applyAlpha(-16777216, alpha);
      int colorTransparent = this.applyAlpha(0, alpha);
      r.gradient(x, y, w, h, 6.0F, colorWhite, colorHue, colorHue, colorWhite);
      r.gradient(x, y, w, h, 6.0F, colorTransparent, colorTransparent, colorBlack, colorBlack);
      float sat = this.setting.getHsb()[1];
      float bri = this.setting.getHsb()[2];
      float var10000 = x + sat * w;
      var10000 = y + (1.0F - bri) * h;
   }

   private void renderAlphaSlider(Renderer2D r, float x, float y, float w, float h, float alpha) {
      r.pushRoundedClipRect(x, y, w, h, 4.0F, 4.0F, 4.0F, 4.0F);
      this.renderCheckerboard(r, x, y, w, h, 4.0F, alpha);
      r.popClipRect();
      Color c = Color.getHSBColor(this.setting.getHsb()[0], this.setting.getHsb()[1], this.setting.getHsb()[2]);
      int colorFull = this.applyAlpha(c.getRGB(), alpha);
      int colorTrans = this.applyAlpha((new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)).getRGB(), alpha);
      r.gradient(x, y, w, h, 4.0F, colorFull, colorFull, colorTrans, colorTrans);
      float alphaVal = (float)((Color)this.setting.getValue()).getAlpha() / 255.0F;
      float cursorX = (1.0F - alphaVal) * w;
      this.renderSliderCursor(r, x + cursorX, y + h / 2.0F, alpha);
   }

   private void renderSliderCursor(Renderer2D r, float cx, float cy, float alpha) {
      float cw = 4.0F;
      float ch = 12.0F;
      r.rect(cx - cw / 2.0F, cy - ch / 2.0F, cw, ch, 2.0F, this.applyAlpha(-1, alpha));
   }

   private void renderCheckerboard(Renderer2D r, float x, float y, float w, float h, float rad, float alpha) {
      float size = 4.0F;
      boolean rowToggle = false;
      r.rect(x, y, w, h, rad, this.applyAlpha(-14013910, alpha));

      for(float cy = y; cy < y + h; cy += size) {
         boolean colToggle = rowToggle;
         float rowH = Math.min(size, y + h - cy);

         for(float cx = x; cx < x + w; cx += size) {
            if (colToggle) {
               float colW = Math.min(size, x + w - cx);
               r.rect(cx, cy, colW, rowH, 0.0F, this.applyAlpha(-12829636, alpha));
            }

            colToggle = !colToggle;
         }

         rowToggle = !rowToggle;
      }

   }

   public boolean mouseClicked(double mx, double my, int button, float x, float y, float width) {
      if (this.isHovered(mx, my, x, y, width, 18.0F) && button == 1) {
         this.expanded = !this.expanded;
         return true;
      } else if (!this.expanded) {
         return false;
      } else {
         float startY = y + 18.0F + 6.0F;
         float contentX = x + 6.0F;
         float contentWidth = width - 12.0F;
         if (this.isHovered(mx, my, contentX, startY, contentWidth, 80.0F) && button == 0) {
            this.draggingSB = true;
            this.updateSB(mx, my, contentX, startY, contentWidth, 80.0F);
            return true;
         } else {
            float currentY = startY + 80.0F + 6.0F;
            if (this.isHovered(mx, my, contentX, currentY, contentWidth, 10.0F) && button == 0) {
               this.draggingHue = true;
               this.updateHue(mx, contentX, contentWidth);
               return true;
            } else {
               currentY += 16.0F;
               if (this.isHovered(mx, my, contentX, currentY, contentWidth, 10.0F) && button == 0) {
                  this.draggingAlpha = true;
                  this.updateAlpha(mx, contentX, contentWidth);
                  return true;
               } else {
                  return false;
               }
            }
         }
      }
   }

   public void mouseDragged(double mx, double my, int button, float x, float y, float width) {
      if (this.expanded) {
         float startY = y + 18.0F + 6.0F;
         float contentX = x + 6.0F;
         float contentWidth = width - 12.0F;
         if (this.draggingSB) {
            this.updateSB(mx, my, contentX, startY, contentWidth, 80.0F);
         } else if (this.draggingHue) {
            this.updateHue(mx, contentX, contentWidth);
         } else if (this.draggingAlpha) {
            this.updateAlpha(mx, contentX, contentWidth);
         }

      }
   }

   public void mouseReleased(double mouseX, double mouseY, int button) {
      this.draggingSB = false;
      this.draggingHue = false;
      this.draggingAlpha = false;
   }

   private void updateSB(double mx, double my, float x, float y, float w, float h) {
      float sat = (float)((mx - (double)x) / (double)w);
      float bri = 1.0F - (float)((my - (double)y) / (double)h);
      this.setting.setSaturation(Math.max(0.0F, Math.min(1.0F, sat)));
      this.setting.setBrightness(Math.max(0.0F, Math.min(1.0F, bri)));
   }

   private void updateHue(double mx, float x, float w) {
      float hue = (float)((mx - (double)x) / (double)w);
      this.setting.setHue(Math.max(0.0F, Math.min(1.0F, hue)));
   }

   private void updateAlpha(double mx, float x, float w) {
      float pos = (float)((mx - (double)x) / (double)w);
      pos = Math.max(0.0F, Math.min(1.0F, pos));
      float alpha = 1.0F - pos;
      Color c = (Color)this.setting.getValue();
      this.setting.setValue(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 255.0F)));
   }

   private int applyAlpha(int argb, float alpha) {
      int a = argb >> 24 & 255;
      int r = argb >> 16 & 255;
      int g = argb >> 8 & 255;
      int b = argb & 255;
      int newAlpha = (int)((float)a * alpha);
      return newAlpha << 24 | r << 16 | g << 8 | b;
   }

   private boolean isHovered(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   public float getHeight() {
      return this.expanded ? 142.0F : 18.0F;
   }

   public boolean mouseClicked(double mouseX, double mouseY, float x, float y, float width) {
      return false;
   }

   public Setting<?> getSetting() {
      return this.setting;
   }

   public void updateHover(double mouseX, double mouseY, float x, float y, float width) {
   }
}
