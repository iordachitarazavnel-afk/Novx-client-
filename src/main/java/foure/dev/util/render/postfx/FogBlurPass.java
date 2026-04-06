package foure.dev.util.render.postfx;

import foure.dev.util.render.backends.gl.GlState;
import foure.dev.util.render.backends.gl.ShaderProgram;
import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public final class FogBlurPass {
   private static final float LARGE_RADIUS_THRESHOLD = 12.0F;
   private static final float MEDIUM_RADIUS_THRESHOLD = 4.0F;
   private static final float LARGE_RADIUS_SCALE = 1.0F;
   private static final float MEDIUM_RADIUS_SCALE = 0.5F;
   private static final float SMALL_RADIUS_SCALE = 0.25F;
   private final DownsampleBlur blur = new DownsampleBlur(32856, 5121);
   private final RenderTarget compositeTarget = new RenderTarget();
   private final RenderTarget scaledColorTarget = new RenderTarget();
   private final FullScreenQuad quad = new FullScreenQuad();
   private final ShaderProgram program = ShaderProgram.fromResources("assets/hysteria/shaders/blur/blur_fullscreen.vert", "assets/hysteria/shaders/postfx/fog_blur.frag");
   private final ShaderProgram copyProgram = ShaderProgram.fromResources("assets/hysteria/shaders/blur/blur_fullscreen.vert", "assets/hysteria/shaders/postfx/fog_copy.frag");
   private final int uSourceLoc;
   private final int uBlurredLoc;
   private final int uDepthLoc;
   private final int uNearFarLoc;
   private final int uInverseProjectionLoc;
   private final int uInverseProjectionValidLoc;
   private final int uThresholdLoc;
   private final int uTintColorLoc;
   private final int uTintStrengthLoc;
   private final int uBlurredTexelSizeLoc;
   private final int uBlurResolutionScaleLoc;
   private final int uCopySourceLoc;
   private boolean destroyed;

   public FogBlurPass() {
      this.uSourceLoc = this.program.getUniformLocation("uSource");
      this.uBlurredLoc = this.program.getUniformLocation("uBlurred");
      this.uDepthLoc = this.program.getUniformLocation("uDepth");
      this.uNearFarLoc = this.program.getUniformLocation("uNearFar");
      this.uInverseProjectionLoc = this.program.getUniformLocation("uInverseProjection");
      this.uInverseProjectionValidLoc = this.program.getUniformLocation("uInverseProjectionValid");
      this.uThresholdLoc = this.program.getUniformLocation("uThreshold");
      this.uTintColorLoc = this.program.getUniformLocation("uTintColor");
      this.uTintStrengthLoc = this.program.getUniformLocation("uTintStrength");
      this.uBlurredTexelSizeLoc = this.program.getUniformLocation("uBlurredTexelSize");
      this.uBlurResolutionScaleLoc = this.program.getUniformLocation("uBlurResolutionScale");
      this.uCopySourceLoc = this.copyProgram.getUniformLocation("uSource");
   }

   public FogBlurPass.Result render(FogBlurPass.TextureBinding colorBinding, FogBlurPass.TextureBinding depthBinding, int width, int height, float blurRadius, Matrix4f inverseProjection, boolean inverseProjectionValid, float nearPlane, float farPlane, float minThreshold, float maxThreshold, float tintR, float tintG, float tintB, float tintStrength) {
      if (colorBinding != null && colorBinding.textureId() > 0 && width > 0 && height > 0) {
         if (this.destroyed) {
            return new FogBlurPass.Result(colorBinding.textureId(), 0, width, height);
         } else if (!colorBinding.isTexture2D()) {
            return new FogBlurPass.Result(0, 0, 0, 0);
         } else if (depthBinding != null && depthBinding.isTexture2D()) {
            int colorTexture = colorBinding.textureId();
            int depthTexture = depthBinding.textureId();
            float sanitizedNear = Math.max(1.0E-4F, nearPlane);
            float sanitizedFar = Math.max(sanitizedNear + 0.001F, farPlane);
            float thresholdMin = Math.min(minThreshold, maxThreshold);
            float thresholdMax = Math.max(minThreshold, maxThreshold);
            float sanitizedTintStrength = Math.max(0.0F, Math.min(1.0F, tintStrength));
            float sanitizedTintR = Math.max(0.0F, Math.min(1.0F, tintR));
            float sanitizedTintG = Math.max(0.0F, Math.min(1.0F, tintG));
            float sanitizedTintB = Math.max(0.0F, Math.min(1.0F, tintB));
            boolean sanitizedInverseProjectionValid = inverseProjectionValid && inverseProjection != null;
            Matrix4f projectionMatrix = sanitizedInverseProjectionValid ? new Matrix4f(inverseProjection) : new Matrix4f();
            float sanitizedBlurRadius = Math.max(0.0F, blurRadius);
            float resolutionScale = chooseResolutionScale(sanitizedBlurRadius);
            int downscaledWidth = Math.max(1, Math.round((float)width * resolutionScale));
            int downscaledHeight = Math.max(1, Math.round((float)height * resolutionScale));
            this.quad.ensure();
            GlState.Snapshot snapshot = GlState.push();

            FogBlurPass.Result var70;
            try {
               GL11.glDisable(3089);
               GL11.glDisable(2884);
               GL11.glDisable(3042);
               GL11.glDisable(2929);
               GL11.glDisable(36281);
               int blurSourceTexture = colorTexture;
               int blurSourceWidth = width;
               int blurSourceHeight = height;
               boolean downscalePass = resolutionScale < 1.0F && (downscaledWidth != width || downscaledHeight != height);
               float blurResolutionScale = downscalePass ? resolutionScale : 1.0F;
               if (downscalePass) {
                  this.scaledColorTarget.ensure(downscaledWidth, downscaledHeight);
                  GL30.glBindFramebuffer(36160, this.scaledColorTarget.fbo);
                  GL11.glViewport(0, 0, downscaledWidth, downscaledHeight);
                  GL11.glDrawBuffer(36064);
                  this.copyProgram.use();
                  if (this.uCopySourceLoc >= 0) {
                     GL20.glUniform1i(this.uCopySourceLoc, 0);
                  }

                  TextureUnitGuard unit0 = TextureUnitGuard.capture(0, 3553, colorBinding.target());

                  try {
                     GL13.glActiveTexture(33984);
                     GL11.glBindTexture(3553, colorTexture);
                     this.quad.bindAndDraw();
                  } catch (Throwable var63) {
                     if (unit0 != null) {
                        try {
                           unit0.close();
                        } catch (Throwable var61) {
                           var63.addSuppressed(var61);
                        }
                     }

                     throw var63;
                  }

                  if (unit0 != null) {
                     unit0.close();
                  }

                  GL20.glUseProgram(0);
                  GL30.glBindFramebuffer(36160, 0);
                  GL13.glActiveTexture(33984);
                  GL11.glBindTexture(3553, 0);
                  blurSourceTexture = this.scaledColorTarget.colorTex;
                  blurSourceWidth = downscaledWidth;
                  blurSourceHeight = downscaledHeight;
               }

               float scaledBlurRadius = sanitizedBlurRadius * blurResolutionScale;
               int blurredTexture = this.blur.blurFromColorTexture(blurSourceTexture, blurSourceWidth, blurSourceHeight, scaledBlurRadius, false);
               if (blurredTexture == 0) {
                  var70 = new FogBlurPass.Result(colorTexture, 0, width, height);
                  return var70;
               }

               this.compositeTarget.ensure(width, height);
               GL11.glDisable(3089);
               GL11.glDisable(2884);
               GL11.glDisable(3042);
               GL11.glDisable(2929);
               GL11.glDisable(36281);
               GL30.glBindFramebuffer(36160, this.compositeTarget.fbo);
               GL11.glViewport(0, 0, width, height);
               GL11.glDrawBuffer(36064);
               this.program.use();
               if (this.uSourceLoc >= 0) {
                  GL20.glUniform1i(this.uSourceLoc, 0);
               }

               if (this.uBlurredLoc >= 0) {
                  GL20.glUniform1i(this.uBlurredLoc, 1);
               }

               if (this.uDepthLoc >= 0) {
                  GL20.glUniform1i(this.uDepthLoc, 2);
               }

               if (this.uNearFarLoc >= 0) {
                  GL20.glUniform2f(this.uNearFarLoc, sanitizedNear, sanitizedFar);
               }

               if (this.uInverseProjectionValidLoc >= 0) {
                  GL20.glUniform1i(this.uInverseProjectionValidLoc, sanitizedInverseProjectionValid ? 1 : 0);
               }

               if (this.uInverseProjectionLoc >= 0 && sanitizedInverseProjectionValid) {
                  MemoryStack stack = MemoryStack.stackPush();

                  try {
                     FloatBuffer buffer = stack.mallocFloat(16);
                     projectionMatrix.get(buffer);
                     GL20.glUniformMatrix4fv(this.uInverseProjectionLoc, false, buffer);
                  } catch (Throwable var62) {
                     if (stack != null) {
                        try {
                           stack.close();
                        } catch (Throwable var59) {
                           var62.addSuppressed(var59);
                        }
                     }

                     throw var62;
                  }

                  if (stack != null) {
                     stack.close();
                  }
               }

               if (this.uThresholdLoc >= 0) {
                  GL20.glUniform2f(this.uThresholdLoc, thresholdMin, thresholdMax);
               }

               if (this.uTintColorLoc >= 0) {
                  GL20.glUniform3f(this.uTintColorLoc, sanitizedTintR, sanitizedTintG, sanitizedTintB);
               }

               if (this.uTintStrengthLoc >= 0) {
                  GL20.glUniform1f(this.uTintStrengthLoc, sanitizedTintStrength);
               }

               if (this.uBlurredTexelSizeLoc >= 0) {
                  GL20.glUniform2f(this.uBlurredTexelSizeLoc, 1.0F / (float)Math.max(1, blurSourceWidth), 1.0F / (float)Math.max(1, blurSourceHeight));
               }

               if (this.uBlurResolutionScaleLoc >= 0) {
                  GL20.glUniform1f(this.uBlurResolutionScaleLoc, blurResolutionScale);
               }

               TextureUnitGuard unit0 = TextureUnitGuard.capture(0, 3553, colorBinding.target());

               try {
                  TextureUnitGuard unit1 = TextureUnitGuard.capture(1, 3553);

                  try {
                     TextureUnitGuard unit2 = TextureUnitGuard.capture(2, 3553, depthBinding.target());

                     try {
                        GL13.glActiveTexture(33984);
                        GL11.glBindTexture(colorBinding.target(), colorTexture);
                        GL13.glActiveTexture(33985);
                        GL11.glBindTexture(3553, blurredTexture);
                        GL13.glActiveTexture(33986);
                        GL11.glBindTexture(depthBinding.target(), depthTexture);
                        this.quad.bindAndDraw();
                     } catch (Throwable var64) {
                        if (unit2 != null) {
                           try {
                              unit2.close();
                           } catch (Throwable var60) {
                              var64.addSuppressed(var60);
                           }
                        }

                        throw var64;
                     }

                     if (unit2 != null) {
                        unit2.close();
                     }
                  } catch (Throwable var65) {
                     if (unit1 != null) {
                        try {
                           unit1.close();
                        } catch (Throwable var58) {
                           var65.addSuppressed(var58);
                        }
                     }

                     throw var65;
                  }

                  if (unit1 != null) {
                     unit1.close();
                  }
               } catch (Throwable var66) {
                  if (unit0 != null) {
                     try {
                        unit0.close();
                     } catch (Throwable var57) {
                        var66.addSuppressed(var57);
                     }
                  }

                  throw var66;
               }

               if (unit0 != null) {
                  unit0.close();
               }

               var70 = new FogBlurPass.Result(this.compositeTarget.colorTex, this.compositeTarget.fbo, width, height);
            } finally {
               GL20.glUseProgram(0);
               GL30.glBindFramebuffer(36160, 0);
               GL13.glActiveTexture(33984);
               GL11.glBindTexture(3553, 0);
               GlState.pop(snapshot);
            }

            return var70;
         } else {
            return new FogBlurPass.Result(colorBinding.textureId(), 0, width, height);
         }
      } else {
         return new FogBlurPass.Result(0, 0, 0, 0);
      }
   }

   public void destroy() {
      if (!this.destroyed) {
         this.destroyed = true;
         this.blur.destroy();
         this.compositeTarget.destroy();
         this.scaledColorTarget.destroy();
         this.quad.destroy();
         this.program.delete();
         this.copyProgram.delete();
      }
   }

   private static float chooseResolutionScale(float blurRadius) {
      float sanitized = Math.max(0.0F, blurRadius);
      if (sanitized >= 12.0F) {
         return 1.0F;
      } else {
         return sanitized >= 4.0F ? 0.5F : 0.25F;
      }
   }

   public static record TextureBinding(int textureId, int target) {
      public TextureBinding(int textureId, int target) {
         this.textureId = textureId;
         this.target = target;
      }

      public boolean isValid() {
         return this.textureId > 0 && this.target != 0;
      }

      public boolean isTexture2D() {
         return this.textureId > 0 && this.target == 3553;
      }

      public int textureId() {
         return this.textureId;
      }

      public int target() {
         return this.target;
      }
   }

   public static record Result(int colorTexture, int framebuffer, int width, int height) {
      public Result(int colorTexture, int framebuffer, int width, int height) {
         this.colorTexture = colorTexture;
         this.framebuffer = framebuffer;
         this.width = width;
         this.height = height;
      }

      public int colorTexture() {
         return this.colorTexture;
      }

      public int framebuffer() {
         return this.framebuffer;
      }

      public int width() {
         return this.width;
      }

      public int height() {
         return this.height;
      }
   }
}
