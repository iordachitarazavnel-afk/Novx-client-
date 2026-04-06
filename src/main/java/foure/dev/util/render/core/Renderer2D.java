package foure.dev.util.render.core;

import foure.dev.util.render.backends.gl.GlBackend;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.TextRenderer;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class Renderer2D {
   private static final float MIN_BLUR_STRENGTH = 0.5F;
   private static final float BLUR_STRENGTH_EPSILON = 0.05F;
   private final GlBackend backend;
   private final ArrayDeque<Renderer2D.ClipState> clipStack = new ArrayDeque();
   private final ArrayDeque<Float> alphaStack = new ArrayDeque();
   private final TransformStack transformStack = new TransformStack();
   private Map<String, TextRenderer> idToTextRenderer = new HashMap();
   private final ShapeBatcher batcher;
   private boolean frameBegun = false;
   private int frameWidth = 0;
   private int frameHeight = 0;
   private double guiScale = 1.0D;
   private boolean blurPrepared = false;
   private float blurPreparedStrength = 0.0F;
   private int blurPreparedWidth = 0;
   private int blurPreparedHeight = 0;
   private boolean regionBlurPrepared = false;
   private float regionBlurPreparedStrength = 0.0F;
   private int regionBlurCaptureX = 0;
   private int regionBlurCaptureY = 0;
   private int regionBlurCaptureWidth = 0;
   private int regionBlurCaptureHeight = 0;
   private static final int[] COLOR_CODES = new int[32];

   public Renderer2D(GlBackend backend) {
      this.backend = backend;
      this.batcher = new ShapeBatcher(backend);
      this.resetAlphaStack();
   }

   public void begin(int width, int height) {
      if (this.frameBegun) {
         throw new IllegalStateException("begin() called while a frame is already active");
      } else {
         this.frameBegun = true;
         this.frameWidth = width;
         this.frameHeight = height;
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.getWindow() != null) {
            this.guiScale = (double)client.getWindow().getScaleFactor();
         } else {
            this.guiScale = 1.0D;
         }

         this.blurPrepared = false;
         this.blurPreparedStrength = 0.0F;
         this.blurPreparedWidth = 0;
         this.blurPreparedHeight = 0;
         this.regionBlurPrepared = false;
         this.regionBlurPreparedStrength = 0.0F;
         this.regionBlurCaptureX = 0;
         this.regionBlurCaptureY = 0;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
         RenderFrameMetrics.getInstance().beginFrame(width, height);
         this.backend.beginFrame(width, height);
         this.backend.setScissorEnabled(false);
         this.clipStack.clear();
         this.transformStack.clear();
         this.transformStack.pushScale((float)this.guiScale, (float)this.guiScale, 0.0F, 0.0F);
         this.resetAlphaStack();
      }
   }

   public double getGuiScale() {
      return this.guiScale;
   }

   private void ensureFrame() {
      if (!this.frameBegun) {
         throw new IllegalStateException("begin() must be called before issuing draw commands");
      }
   }

   public void rect(float x, float y, float w, float h, int rgbaPremul) {
      this.ensureFrame();
      this.batcher.enqueueRect(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, this.modulateColor(rgbaPremul), this.transformStack.current());
   }

   public void rect(float x, float y, float w, float h, float rounding, int rgbaPremul) {
      this.rect(x, y, w, h, rounding, rounding, rounding, rounding, rgbaPremul);
   }

   public void rect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft, int rgbaPremul) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      normalizeCornerRadii(w, h, radii);
      this.batcher.enqueueRect(x, y, w, h, radii[0], radii[1], radii[2], radii[3], this.modulateColor(rgbaPremul), this.transformStack.current());
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, -1, true, false);
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, true, false);
   }

   public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba, boolean flipVertically) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, flipVertically, false);
   }

   public void drawPremultipliedRgbaTexture(int texture, float x, float y, float w, float h) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, -1, true, true);
   }

   public void drawPremultipliedRgbaTexture(int texture, float x, float y, float w, float h, int tintRgba, boolean flipVertically) {
      this.drawRgbaTextureInternal(texture, x, y, w, h, tintRgba, flipVertically, true);
   }

   private void drawRgbaTextureInternal(int texture, float x, float y, float w, float h, int tintRgba, boolean flipVertically, boolean preservePremultipliedColor) {
      this.ensureFrame();
      if (texture > 0) {
         float v0 = flipVertically ? 1.0F : 0.0F;
         float v1 = flipVertically ? 0.0F : 1.0F;
         this.backend.drawRgbaTexturedQuad(texture, x, y, w, h, 0.0F, v0, 1.0F, v1, this.modulateColor(tintRgba), this.transformStack.current(), preservePremultipliedColor);
      }
   }

   public void end() {
      if (!this.frameBegun) {
         throw new IllegalStateException("end() called without a matching begin()");
      } else {
         this.batcher.flush();
         this.backend.endFrame();
         RenderFrameMetrics.getInstance().endFrame();
         this.frameBegun = false;
         this.frameWidth = 0;
         this.frameHeight = 0;
         this.guiScale = 1.0D;
         this.blurPrepared = false;
         this.blurPreparedStrength = 0.0F;
         this.blurPreparedWidth = 0;
         this.blurPreparedHeight = 0;
         this.regionBlurPrepared = false;
         this.regionBlurPreparedStrength = 0.0F;
         this.regionBlurCaptureX = 0;
         this.regionBlurCaptureY = 0;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
         this.resetAlphaStack();
      }
   }

   public void flush() {
      this.ensureFrame();
      this.batcher.flush();
   }

   public void pushClipRect(int x, int y, int w, int h) {
      this.pushRoundedClipRect((float)x, (float)y, (float)w, (float)h, 0.0F, 0.0F, 0.0F, 0.0F);
   }

   public void pushRoundedClipRect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft) {
      this.ensureFrame();
      Renderer2D.ClipState incoming = Renderer2D.ClipState.fromRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, this.transformStack.current());
      Renderer2D.ClipState applied;
      if (this.clipStack.isEmpty()) {
         applied = incoming;
      } else {
         Renderer2D.ClipState current = (Renderer2D.ClipState)this.clipStack.peek();
         applied = Renderer2D.ClipState.intersect(current, incoming);
      }

      this.clipStack.push(applied);
      this.applyClipState(applied);
   }

   public void popClipRect() {
      this.ensureFrame();
      if (!this.clipStack.isEmpty()) {
         this.clipStack.pop();
         if (this.clipStack.isEmpty()) {
            this.backend.setScissorEnabled(false);
         } else {
            this.applyClipState((Renderer2D.ClipState)this.clipStack.peek());
         }

      }
   }

   private void applyClipState(Renderer2D.ClipState state) {
      if (state == null) {
         this.backend.setScissorEnabled(false);
      } else {
         this.backend.setScissorEnabled(true);
         this.backend.setScissorRect(state.x(), state.y(), state.w(), state.h(), state.roundTopLeft(), state.roundTopRight(), state.roundBottomRight(), state.roundBottomLeft());
      }
   }

   public void rectOutline(float x, float y, float w, float h, int rgbaPremul, float thickness) {
      this.ensureFrame();
      this.batcher.enqueueRectOutline(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, this.modulateColor(rgbaPremul), Math.max(1.0F, thickness), this.transformStack.current());
   }

   public void rectOutline(float x, float y, float w, float h, float rounding, int rgbaPremul, float thickness) {
      this.rectOutline(x, y, w, h, rounding, rounding, rounding, rounding, rgbaPremul, thickness);
   }

   public void rectOutline(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft, int rgbaPremul, float thickness) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      normalizeCornerRadii(w, h, radii);
      this.batcher.enqueueRectOutline(x, y, w, h, radii[0], radii[1], radii[2], radii[3], this.modulateColor(rgbaPremul), Math.max(1.0F, thickness), this.transformStack.current());
   }

   public void line(float x1, float y1, float x2, float y2, float thickness, int rgbaPremul) {
      this.ensureFrame();
      float dx = x2 - x1;
      float dy = y2 - y1;
      float dist = (float)Math.sqrt((double)(dx * dx + dy * dy));
      if (!(dist <= 0.0F)) {
         float rad = (float)Math.atan2((double)dy, (double)dx);
         float deg = (float)Math.toDegrees((double)rad);
         this.transformStack.pushTranslation(x1, y1);
         this.transformStack.pushRotation(deg);
         this.rect(0.0F, -thickness / 2.0F, dist, thickness, this.modulateColor(rgbaPremul));
         this.transformStack.pop();
         this.transformStack.pop();
      }
   }

   public void quad(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, int color) {
      this.ensureFrame();
      this.batcher.enqueueQuad(x1, y1, x2, y2, x3, y3, x4, y4, this.modulateColor(color), this.transformStack.current());
   }

   public void quadOutline(float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4, float thickness, int color) {
      this.line(x1, y1, x2, y2, thickness, color);
      this.line(x2, y2, x3, y3, thickness, color);
      this.line(x3, y3, x4, y4, thickness, color);
      this.line(x4, y4, x1, y1, thickness, color);
   }

   public void gradient(float x, float y, float w, float h, int c00, int c10, int c11, int c01) {
      this.ensureFrame();
      this.batcher.enqueueGradient(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F, this.modulateColor(c00), this.modulateColor(c10), this.modulateColor(c11), this.modulateColor(c01), this.transformStack.current());
   }

   public void gradient(float x, float y, float w, float h, float rounding, int c00, int c10, int c11, int c01) {
      this.gradient(x, y, w, h, rounding, rounding, rounding, rounding, c00, c10, c11, c01);
   }

   public void gradient(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft, int c00, int c10, int c11, int c01) {
      this.ensureFrame();
      float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
      normalizeCornerRadii(w, h, radii);
      this.batcher.enqueueGradient(x, y, w, h, radii[0], radii[1], radii[2], radii[3], this.modulateColor(c00), this.modulateColor(c10), this.modulateColor(c11), this.modulateColor(c01), this.transformStack.current());
   }

   public void circle(float cx, float cy, float radius, float startDeg, float pct, int rgbaPremul) {
      this.ensureFrame();
      this.batcher.enqueueCircle(cx, cy, radius, startDeg, pct, this.modulateColor(rgbaPremul), this.transformStack.current());
   }

   public void shadow(float x, float y, float w, float h, float rounding, float blurStrength, float spread, int rgbaPremul) {
      this.shadow(x, y, w, h, rounding, rounding, rounding, rounding, blurStrength, spread, rgbaPremul);
   }

   public void shadow(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft, float blurStrength, float spread, int rgbaPremul) {
      this.ensureFrame();
      if (!(w <= 0.0F) && !(h <= 0.0F)) {
         float safeBlur = Math.max(0.0F, blurStrength);
         float safeSpread = Math.max(0.0F, spread);
         if (!(safeBlur <= 0.0F) || !(safeSpread <= 0.0F)) {
            float[] radii = scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
            normalizeCornerRadii(w, h, radii);
            this.backend.drawDropShadowRect(x, y, w, h, radii[0], radii[1], radii[2], radii[3], safeBlur, safeSpread, this.modulateColor(rgbaPremul), this.transformStack.current());
         }
      }
   }

   public void blur(float x, float y, float w, float h, float rounding) {
      this.blur(x, y, w, h, rounding, 1.0F);
   }

   public void blur(float x, float y, float w, float h, float rounding, float alpha) {
      this.ensureFrame();
      if (this.blurPrepared) {
         float opacity = clamp01(alpha) * this.currentAlphaMultiplier();
         if (!(opacity <= 1.0E-4F)) {
            this.backend.drawPreparedBlurRounded(x, y, w, h, Math.max(0.0F, rounding), opacity, this.transformStack.current());
         }
      }
   }

   public void blurRegion(float x, float y, float w, float h, float rounding) {
      this.blurRegion(x, y, w, h, rounding, 1.0F);
   }

   public void blurRegion(float x, float y, float w, float h, float rounding, float alpha) {
      this.ensureFrame();
      if (this.regionBlurPrepared) {
         float opacity = clamp01(alpha) * this.currentAlphaMultiplier();
         if (!(opacity <= 1.0E-4F)) {
            this.backend.drawPreparedRegionBlurRounded(x, y, w, h, Math.max(0.0F, rounding), opacity, this.transformStack.current(), this.regionBlurCaptureX, this.regionBlurCaptureY, this.regionBlurCaptureWidth, this.regionBlurCaptureHeight);
         }
      }
   }

   public void prepareBlur(float strength) {
      this.ensureFrame();
      int width = this.frameWidth;
      int height = this.frameHeight;
      if (width > 0 && height > 0) {
         float radius = Math.max(0.5F, strength);
         boolean alreadyPrepared = this.blurPrepared && this.blurPreparedWidth == width && this.blurPreparedHeight == height && Math.abs(this.blurPreparedStrength - radius) <= 0.05F;
         if (!alreadyPrepared) {
            this.backend.prepareScreenBlur(width, height, radius);
            this.blurPrepared = true;
            this.blurPreparedStrength = radius;
            this.blurPreparedWidth = width;
            this.blurPreparedHeight = height;
         }
      } else {
         this.blurPrepared = false;
         this.blurPreparedWidth = 0;
         this.blurPreparedHeight = 0;
      }
   }

   public void prepareBlurRegion(float x, float y, float w, float h, float strength) {
      this.ensureFrame();
      if (this.frameWidth > 0 && this.frameHeight > 0) {
         if (!(w <= 0.0F) && !(h <= 0.0F)) {
            float[] matrix = this.transformStack.current();
            Renderer2D.Bounds bounds = computeTransformedBounds(matrix, x, y, w, h);
            int captureLeft = clampToViewportFloor(bounds.minX, this.frameWidth);
            int captureTop = clampToViewportFloor(bounds.minY, this.frameHeight);
            int captureRight = clampToViewportCeil(bounds.maxX, this.frameWidth);
            int captureBottom = clampToViewportCeil(bounds.maxY, this.frameHeight);
            int captureWidth = Math.max(0, captureRight - captureLeft);
            int captureHeight = Math.max(0, captureBottom - captureTop);
            if (captureWidth > 0 && captureHeight > 0) {
               float radius = Math.max(0.5F, strength);
               boolean alreadyPrepared = this.regionBlurPrepared && this.regionBlurCaptureX == captureLeft && this.regionBlurCaptureY == captureTop && this.regionBlurCaptureWidth == captureWidth && this.regionBlurCaptureHeight == captureHeight && Math.abs(this.regionBlurPreparedStrength - radius) <= 0.05F;
               if (!alreadyPrepared) {
                  boolean success = this.backend.prepareRegionBlur(captureLeft, captureTop, captureWidth, captureHeight, radius);
                  this.regionBlurPrepared = success;
                  if (success) {
                     this.regionBlurPreparedStrength = radius;
                     this.regionBlurCaptureX = captureLeft;
                     this.regionBlurCaptureY = captureTop;
                     this.regionBlurCaptureWidth = captureWidth;
                     this.regionBlurCaptureHeight = captureHeight;
                  } else {
                     this.regionBlurPreparedStrength = 0.0F;
                     this.regionBlurCaptureWidth = 0;
                     this.regionBlurCaptureHeight = 0;
                  }

               }
            } else {
               this.regionBlurPrepared = false;
               this.regionBlurCaptureWidth = 0;
               this.regionBlurCaptureHeight = 0;
            }
         } else {
            this.regionBlurPrepared = false;
            this.regionBlurCaptureWidth = 0;
            this.regionBlurCaptureHeight = 0;
         }
      } else {
         this.regionBlurPrepared = false;
         this.regionBlurCaptureWidth = 0;
         this.regionBlurCaptureHeight = 0;
      }
   }

   private static int clampToViewportFloor(float value, int viewportMax) {
      int floored = (int)Math.floor((double)value);
      if (floored < 0) {
         return 0;
      } else {
         return floored > viewportMax ? viewportMax : floored;
      }
   }

   private static int clampToViewportCeil(float value, int viewportMax) {
      int ceiled = (int)Math.ceil((double)value);
      if (ceiled < 0) {
         return 0;
      } else {
         return ceiled > viewportMax ? viewportMax : ceiled;
      }
   }

   private static Renderer2D.Bounds computeTransformedBounds(float[] matrix, float x, float y, float w, float h) {
      float x1 = x + w;
      float y1 = y + h;
      float wx0y0x = transformX(matrix, x, y);
      float wx0y0y = transformY(matrix, x, y);
      float wx1y0x = transformX(matrix, x1, y);
      float wx1y0y = transformY(matrix, x1, y);
      float wx1y1x = transformX(matrix, x1, y1);
      float wx1y1y = transformY(matrix, x1, y1);
      float wx0y1x = transformX(matrix, x, y1);
      float wx0y1y = transformY(matrix, x, y1);
      float minX = Math.min(Math.min(wx0y0x, wx1y0x), Math.min(wx1y1x, wx0y1x));
      float maxX = Math.max(Math.max(wx0y0x, wx1y0x), Math.max(wx1y1x, wx0y1x));
      float minY = Math.min(Math.min(wx0y0y, wx1y0y), Math.min(wx1y1y, wx0y1y));
      float maxY = Math.max(Math.max(wx0y0y, wx1y0y), Math.max(wx1y1y, wx0y1y));
      return new Renderer2D.Bounds(minX, minY, maxX, maxY);
   }

   private static float transformX(float[] matrix, float px, float py) {
      return matrix != null && matrix.length >= 6 ? matrix[0] * px + matrix[1] * py + matrix[2] : px;
   }

   private static float transformY(float[] matrix, float px, float py) {
      return matrix != null && matrix.length >= 6 ? matrix[3] * px + matrix[4] * py + matrix[5] : py;
   }

   public void setTransform(float[] m3) {
      this.ensureFrame();
      this.transformStack.clear();
      this.transformStack.pushScale((float)this.guiScale, (float)this.guiScale, 0.0F, 0.0F);
      this.transformStack.replaceTop(m3);
   }

   public void pushRotation(float degrees) {
      this.ensureFrame();
      this.transformStack.pushRotation(degrees);
   }

   public void popRotation() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushTranslation(float tx, float ty) {
      this.ensureFrame();
      this.transformStack.pushTranslation(tx, ty);
   }

   public void popTransform() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushScale(float scale) {
      this.pushScale(scale, scale);
   }

   public void pushScale(float sx, float sy) {
      this.ensureFrame();
      this.transformStack.pushScale(sx, sy, 0.0F, 0.0F);
   }

   public void pushScaleCentered(float scale) {
      this.pushScaleCentered(scale, scale);
   }

   public void pushScaleCentered(float sx, float sy) {
      this.ensureFrame();
      if (this.frameWidth > 0 && this.frameHeight > 0) {
         this.transformStack.pushScale(sx, sy, (float)this.frameWidth * 0.5F, (float)this.frameHeight * 0.5F);
      } else {
         throw new IllegalStateException("Cannot compute frame center before begin(width, height) is called with positive dimensions");
      }
   }

   public void pushScale(float scale, float originX, float originY) {
      this.pushScale(scale, scale, originX, originY);
   }

   public void pushScale(float sx, float sy, float originX, float originY) {
      this.ensureFrame();
      this.transformStack.pushScale(sx, sy, originX, originY);
   }

   public void popScale() {
      this.ensureFrame();
      this.transformStack.pop();
   }

   public void pushAlpha(float alpha) {
      this.ensureFrame();
      float parent = this.currentAlphaMultiplier();
      float clamped = clamp01(alpha);
      this.alphaStack.push(parent * clamped);
   }

   public void popAlpha() {
      this.ensureFrame();
      if (this.alphaStack.size() > 1) {
         this.alphaStack.pop();
      }

   }

   public void registerTextRenderer(String fontId, TextRenderer tr) {
      if (tr != null) {
         this.idToTextRenderer.put(fontId, tr);
      }

   }

   public void registerTextRenderer(FontObject fo, TextRenderer tr) {
      if (tr != null) {
         this.idToTextRenderer.put(fo.id, tr);
      }

   }

   public TransformStack getTransformStack() {
      return this.transformStack;
   }

   public void text(FontObject fo, float x, float y, float size, String s, int rgbaPremul) {
      this.ensureFrame();
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (s != null && !s.isEmpty() && !(size <= 0.0F)) {
         TextRenderer tr = (TextRenderer)this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            float currentX = x;
            int currentColor = rgbaPremul;
            StringBuilder sb = new StringBuilder();

            for(int i = 0; i < s.length(); ++i) {
               char c = s.charAt(i);
               if (c == 167 && i + 1 < s.length()) {
                  if (!sb.isEmpty()) {
                     String part = sb.toString();
                     tr.drawText(currentX, y, size, part, this.modulateColor(currentColor), this.transformStack.current());
                     TextRenderer.TextMetrics m = tr.measureText(part, size);
                     currentX += m != null ? m.width() : 0.0F;
                     sb.setLength(0);
                  }

                  int codeIndex = "0123456789abcdef".indexOf(Character.toLowerCase(s.charAt(i + 1)));
                  if (codeIndex != -1) {
                     int rgb = COLOR_CODES[codeIndex];
                     int alpha = rgbaPremul >> 24 & 255;
                     currentColor = alpha << 24 | rgb;
                  } else if (Character.toLowerCase(s.charAt(i + 1)) == 'r') {
                     currentColor = rgbaPremul;
                  }

                  ++i;
               } else {
                  sb.append(c);
               }
            }

            if (!sb.isEmpty()) {
               tr.drawText(currentX, y, size, sb.toString(), this.modulateColor(currentColor), this.transformStack.current());
            }

         }
      }
   }

   public void text(FontObject fo, float x, float y, float size, Text mcText, int rgbaPremul, String alignKey) {
      this.ensureFrame();
      if (fo != null && mcText != null && !(size <= 0.0F)) {
         TextRenderer tr = (TextRenderer)this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            StringBuilder sb = new StringBuilder();
            tr.findStyle(sb, mcText);
            String styled = sb.toString();
            this.text(fo, x, y, size, styled, rgbaPremul, alignKey);
         }
      }
   }

   public void text(FontObject fo, float x, float y, float size, String s, int rgbaPremul, String alignKey) {
      float w;
      if (!"c".equals(alignKey) && !"center".equals(alignKey)) {
         if (!"r".equals(alignKey) && !"right".equals(alignKey)) {
            this.text(fo, x, y, size, s, rgbaPremul);
         } else {
            w = this.getStringWidth(fo, s, size);
            this.text(fo, x - w, y, size, s, rgbaPremul);
         }
      } else {
         w = this.getStringWidth(fo, s, size);
         this.text(fo, x - w / 2.0F, y, size, s, rgbaPremul);
      }

   }

   public TextRenderer.TextMetrics measureText(FontObject fo, String text, float size) {
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (size <= 0.0F) {
         return new TextRenderer.TextMetrics(0.0F, 0.0F);
      } else {
         TextRenderer tr = (TextRenderer)this.idToTextRenderer.get(fo.id);
         return tr == null ? new TextRenderer.TextMetrics(0.0F, 0.0F) : tr.measureText(text == null ? "" : text, size);
      }
   }

   public TextRenderer.TextMetrics measureText(FontObject fo, Text mcText, float size) {
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (mcText != null && !(size <= 0.0F)) {
         TextRenderer tr = (TextRenderer)this.idToTextRenderer.get(fo.id);
         if (tr == null) {
            return new TextRenderer.TextMetrics(0.0F, 0.0F);
         } else {
            StringBuilder sb = new StringBuilder();
            tr.findStyle(sb, mcText);
            return tr.measureText(sb.toString(), size);
         }
      } else {
         return new TextRenderer.TextMetrics(0.0F, 0.0F);
      }
   }

   public float getStringWidth(FontObject fo, Text mcText, float size) {
      TextRenderer.TextMetrics m = this.measureText(fo, mcText, size);
      return m == null ? 0.0F : m.width();
   }

   public float getStringWidth(FontObject fo, String text, float size) {
      if (text != null && !text.isEmpty()) {
         String stripped = text.replaceAll("§[0-9a-fk-or]", "");
         TextRenderer.TextMetrics metrics = this.measureText(fo, stripped, size);
         return metrics == null ? 0.0F : metrics.width();
      } else {
         return 0.0F;
      }
   }

   private void resetAlphaStack() {
      this.alphaStack.clear();
      this.alphaStack.push(1.0F);
   }

   private float currentAlphaMultiplier() {
      return this.alphaStack.isEmpty() ? 1.0F : (Float)this.alphaStack.peek();
   }

   private int modulateColor(int rgbaPremul) {
      float factor = this.currentAlphaMultiplier();
      if (factor >= 0.999F) {
         return rgbaPremul;
      } else {
         int a = rgbaPremul >>> 24 & 255;
         int r = rgbaPremul >>> 16 & 255;
         int g = rgbaPremul >>> 8 & 255;
         int b = rgbaPremul & 255;
         int na = scaleChannel(a, factor);
         int nr = scaleChannel(r, factor);
         int ng = scaleChannel(g, factor);
         int nb = scaleChannel(b, factor);
         return na << 24 | nr << 16 | ng << 8 | nb;
      }
   }

   private static int scaleChannel(int value, float factor) {
      float scaled = (float)value * factor;
      if (scaled <= 0.0F) {
         return 0;
      } else {
         return scaled >= 255.0F ? 255 : Math.round(scaled);
      }
   }

   private static float clamp01(float value) {
      if (value < 0.0F) {
         return 0.0F;
      } else {
         return value > 1.0F ? 1.0F : value;
      }
   }

   private static float[] scratchRadii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
      return new float[]{topLeft, topRight, bottomRight, bottomLeft};
   }

   private static void normalizeCornerRadii(float w, float h, float[] radii) {
      if (radii != null && radii.length >= 4) {
         float absH;
         for(int i = 0; i < 4; ++i) {
            absH = radii[i];
            if (!Float.isFinite(absH)) {
               absH = 0.0F;
            }

            radii[i] = Math.max(0.0F, absH);
         }

         float absW = Math.abs(w);
         absH = Math.abs(h);
         if (!(absW <= 0.0F) && !(absH <= 0.0F)) {
            enforceRadiusLimit(radii, 0, 1, absW);
            enforceRadiusLimit(radii, 3, 2, absW);
            enforceRadiusLimit(radii, 0, 3, absH);
            enforceRadiusLimit(radii, 1, 2, absH);
         } else {
            Arrays.fill(radii, 0.0F);
         }
      } else {
         throw new IllegalArgumentException("radii");
      }
   }

   private static void enforceRadiusLimit(float[] radii, int a, int b, float limit) {
      float sum = radii[a] + radii[b];
      if (sum > limit && limit > 0.0F) {
         float scale = limit / sum;
         radii[a] *= scale;
         radii[b] *= scale;
      }

   }

   private static boolean nearlyEqual(float a, float b) {
      return Math.abs(a - b) <= 1.0E-4F;
   }

   private static boolean nearlyZero(float value) {
      return Math.abs(value) <= 1.0E-4F;
   }

   private static boolean isIdentityTransform(float[] matrix) {
      if (matrix != null && matrix.length >= 9) {
         return nearlyEqual(matrix[0], 1.0F) && nearlyZero(matrix[1]) && nearlyZero(matrix[2]) && nearlyZero(matrix[3]) && nearlyEqual(matrix[4], 1.0F) && nearlyZero(matrix[5]) && nearlyZero(matrix[6]) && nearlyZero(matrix[7]) && nearlyEqual(matrix[8], 1.0F);
      } else {
         return true;
      }
   }

   private static boolean isAxisAlignedTransform(float[] matrix) {
      if (matrix != null && matrix.length >= 9) {
         return nearlyZero(matrix[1]) && nearlyZero(matrix[3]) && nearlyZero(matrix[6]) && nearlyZero(matrix[7]) && nearlyEqual(matrix[8], 1.0F);
      } else {
         return true;
      }
   }

   private static float transformPointX(float[] matrix, float x, float y) {
      return matrix != null && matrix.length >= 9 ? matrix[0] * x + matrix[1] * y + matrix[2] : x;
   }

   private static float transformPointY(float[] matrix, float x, float y) {
      return matrix != null && matrix.length >= 9 ? matrix[3] * x + matrix[4] * y + matrix[5] : y;
   }

   private static float computeRadiusScale(float[] matrix) {
      if (matrix != null && matrix.length >= 9) {
         float scaleX = Math.abs(matrix[0]);
         float scaleY = Math.abs(matrix[4]);
         float minScale = Math.min(scaleX, scaleY);
         return minScale <= 1.0E-4F ? 0.0F : minScale;
      } else {
         return 1.0F;
      }
   }

   public void gradientText(FontObject fo, float x, float y, float size, String text, int colorStart, int colorEnd) {
      this.gradientText(fo, x, y, size, text, colorStart, colorEnd, "l");
   }

   public void gradientText(FontObject fo, float x, float y, float size, String text, int colorStart, int colorEnd, String alignKey) {
      this.ensureFrame();
      if (fo == null) {
         throw new IllegalArgumentException("FontObject must not be null");
      } else if (text != null && !text.isEmpty() && !(size <= 0.0F)) {
         TextRenderer tr = (TextRenderer)this.idToTextRenderer.get(fo.id);
         if (tr != null) {
            float renderX = x;
            float w;
            if (!"c".equals(alignKey) && !"center".equals(alignKey)) {
               if ("r".equals(alignKey) || "right".equals(alignKey)) {
                  w = this.getStringWidth(fo, text, size);
                  renderX = x - w;
               }
            } else {
               w = this.getStringWidth(fo, text, size);
               renderX = x - w / 2.0F;
            }

            tr.drawGradientText(renderX, y, size, text, this.modulateColor(colorStart), this.modulateColor(colorEnd));
         }
      }
   }

   static {
      for(int i = 0; i < 32; ++i) {
         int base = (i >> 3 & 1) * 85;
         int r = (i >> 2 & 1) * 170 + base;
         int g = (i >> 1 & 1) * 170 + base;
         int b = (i & 1) * 170 + base;
         if (i == 6) {
            r += 85;
         }

         if (i >= 16) {
            r /= 4;
            g /= 4;
            b /= 4;
         }

         COLOR_CODES[i] = (r & 255) << 16 | (g & 255) << 8 | b & 255;
      }

   }

   private static record ClipState(int x, int y, int w, int h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft) {
      private ClipState(int x, int y, int w, int h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.roundTopLeft = roundTopLeft;
         this.roundTopRight = roundTopRight;
         this.roundBottomRight = roundBottomRight;
         this.roundBottomLeft = roundBottomLeft;
      }

      private static Renderer2D.ClipState fromRect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft) {
         return fromRect(x, y, w, h, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft, (float[])null);
      }

      private static Renderer2D.ClipState fromRect(float x, float y, float w, float h, float roundTopLeft, float roundTopRight, float roundBottomRight, float roundBottomLeft, float[] transform) {
         if (Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(w) && Float.isFinite(h)) {
            boolean hasTransform = transform != null && transform.length >= 9 && !Renderer2D.isIdentityTransform(transform);
            float[] radii = Renderer2D.scratchRadii(roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
            Renderer2D.normalizeCornerRadii(Math.abs(w), Math.abs(h), radii);
            float x2;
            float y2;
            if (!hasTransform) {
               x2 = (float)Math.floor((double)Math.min(x, x + w));
               y2 = (float)Math.floor((double)Math.min(y, y + h));
               float right = (float)Math.ceil((double)Math.max(x, x + w));
               float bottom = (float)Math.ceil((double)Math.max(y, y + h));
               int ix = (int)x2;
               int iy = (int)y2;
               int iw = Math.max(0, (int)(right - x2));
               int ih = Math.max(0, (int)(bottom - y2));
               return iw > 0 && ih > 0 ? new Renderer2D.ClipState(ix, iy, iw, ih, radii[0], radii[1], radii[2], radii[3]) : new Renderer2D.ClipState(ix, iy, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
            } else {
               x2 = x + w;
               y2 = y + h;
               float[] xs = new float[]{x, x2, x, x2};
               float[] ys = new float[]{y, y, y2, y2};
               float minX = Float.POSITIVE_INFINITY;
               float minY = Float.POSITIVE_INFINITY;
               float maxX = Float.NEGATIVE_INFINITY;
               float maxY = Float.NEGATIVE_INFINITY;

               float tx;
               float ty;
               for(int i = 0; i < 4; ++i) {
                  tx = Renderer2D.transformPointX(transform, xs[i], ys[i]);
                  ty = Renderer2D.transformPointY(transform, xs[i], ys[i]);
                  if (!Float.isFinite(tx) || !Float.isFinite(ty)) {
                     return new Renderer2D.ClipState(0, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
                  }

                  if (tx < minX) {
                     minX = tx;
                  }

                  if (tx > maxX) {
                     maxX = tx;
                  }

                  if (ty < minY) {
                     minY = ty;
                  }

                  if (ty > maxY) {
                     maxY = ty;
                  }
               }

               float left = (float)Math.floor((double)Math.min(minX, maxX));
               tx = (float)Math.floor((double)Math.min(minY, maxY));
               ty = (float)Math.ceil((double)Math.max(minX, maxX));
               float bottom = (float)Math.ceil((double)Math.max(minY, maxY));
               int ix = (int)left;
               int iy = (int)tx;
               int iw = Math.max(0, (int)(ty - left));
               int ih = Math.max(0, (int)(bottom - tx));
               if (iw > 0 && ih > 0) {
                  if (Renderer2D.isAxisAlignedTransform(transform)) {
                     float radiusScale = Renderer2D.computeRadiusScale(transform);
                     if (radiusScale > 0.0F) {
                        for(int i = 0; i < radii.length; ++i) {
                           radii[i] *= radiusScale;
                        }
                     } else {
                        Arrays.fill(radii, 0.0F);
                     }
                  } else {
                     Arrays.fill(radii, 0.0F);
                  }

                  Renderer2D.normalizeCornerRadii(Math.abs(ty - left), Math.abs(bottom - tx), radii);
                  return new Renderer2D.ClipState(ix, iy, iw, ih, radii[0], radii[1], radii[2], radii[3]);
               } else {
                  return new Renderer2D.ClipState(ix, iy, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
               }
            }
         } else {
            return new Renderer2D.ClipState(0, 0, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
         }
      }

      private static Renderer2D.ClipState intersect(Renderer2D.ClipState a, Renderer2D.ClipState b) {
         if (a == null) {
            return b;
         } else if (b == null) {
            return a;
         } else {
            int nx = Math.max(a.x(), b.x());
            int ny = Math.max(a.y(), b.y());
            int nr = Math.min(a.x() + a.w(), b.x() + b.w());
            int nb = Math.min(a.y() + a.h(), b.y() + b.h());
            int nw = Math.max(0, nr - nx);
            int nh = Math.max(0, nb - ny);
            if (nw > 0 && nh > 0) {
               if (matchesRect(nx, ny, nw, nh, b)) {
                  return new Renderer2D.ClipState(nx, ny, nw, nh, b.roundTopLeft(), b.roundTopRight(), b.roundBottomRight(), b.roundBottomLeft());
               } else {
                  return matchesRect(nx, ny, nw, nh, a) ? new Renderer2D.ClipState(nx, ny, nw, nh, a.roundTopLeft(), a.roundTopRight(), a.roundBottomRight(), a.roundBottomLeft()) : new Renderer2D.ClipState(nx, ny, nw, nh, 0.0F, 0.0F, 0.0F, 0.0F);
               }
            } else {
               return new Renderer2D.ClipState(nx, ny, 0, 0, 0.0F, 0.0F, 0.0F, 0.0F);
            }
         }
      }

      private static boolean matchesRect(int x, int y, int w, int h, Renderer2D.ClipState other) {
         return other != null && other.x() == x && other.y() == y && other.w() == w && other.h() == h;
      }

      public int x() {
         return this.x;
      }

      public int y() {
         return this.y;
      }

      public int w() {
         return this.w;
      }

      public int h() {
         return this.h;
      }

      public float roundTopLeft() {
         return this.roundTopLeft;
      }

      public float roundTopRight() {
         return this.roundTopRight;
      }

      public float roundBottomRight() {
         return this.roundBottomRight;
      }

      public float roundBottomLeft() {
         return this.roundBottomLeft;
      }
   }

   private static record Bounds(float minX, float minY, float maxX, float maxY) {
      private Bounds(float minX, float minY, float maxX, float maxY) {
         this.minX = minX;
         this.minY = minY;
         this.maxX = maxX;
         this.maxY = maxY;
      }

      public float minX() {
         return this.minX;
      }

      public float minY() {
         return this.minY;
      }

      public float maxX() {
         return this.maxX;
      }

      public float maxY() {
         return this.maxY;
      }
   }
}
