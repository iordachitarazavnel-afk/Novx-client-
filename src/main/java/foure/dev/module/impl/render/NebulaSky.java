package foure.dev.module.impl.render;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.util.render.backends.gl.ShaderProgram;
import java.awt.Color;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(
   name = "NebulaSky",
   category = Category.RENDER,
   desc = "Changes the sky to a nebula"
)
public class NebulaSky extends Function {
   public final ColorSetting color = new ColorSetting("Color", this, new Color(255, 255, 255, 255));
   private final Identifier nebulaTexture = Identifier.of("foure", "textures/nebula_sky.png");
   private ShaderProgram shader;
   private int vao = -1;
   private int vbo = -1;
   private boolean initialized = false;
   private static final String VERTEX_SHADER = "#version 330 core\nlayout(location = 0) in vec3 aPos;\nlayout(location = 1) in vec2 aUv;\nuniform mat4 uView;\nuniform mat4 uProj;\nout vec2 vUv;\nvoid main() {\n    gl_Position = uProj * uView * vec4(aPos, 1.0);\n    vUv = aUv;\n}\n";
   private static final String FRAGMENT_SHADER = "#version 330 core\nin vec2 vUv;\nuniform sampler2D uTexture;\nuniform vec4 uColor;\nout vec4 FragColor;\nvoid main() {\n    FragColor = texture(uTexture, vUv) * uColor;\n}\n";

   public NebulaSky() {
      this.addSettings(new Setting[]{this.color});
   }

   private void init() {
      if (!this.initialized) {
         this.shader = new ShaderProgram("#version 330 core\nlayout(location = 0) in vec3 aPos;\nlayout(location = 1) in vec2 aUv;\nuniform mat4 uView;\nuniform mat4 uProj;\nout vec2 vUv;\nvoid main() {\n    gl_Position = uProj * uView * vec4(aPos, 1.0);\n    vUv = aUv;\n}\n", "#version 330 core\nin vec2 vUv;\nuniform sampler2D uTexture;\nuniform vec4 uColor;\nout vec4 FragColor;\nvoid main() {\n    FragColor = texture(uTexture, vUv) * uColor;\n}\n");
         float s = 1.0F;
         float[] vertices = new float[]{-s, s, s, 0.0F, 0.0F, -s, -s, s, 0.0F, 1.0F, s, -s, s, 1.0F, 1.0F, s, -s, s, 1.0F, 1.0F, s, s, s, 1.0F, 0.0F, -s, s, s, 0.0F, 0.0F, -s, s, -s, 0.0F, 0.0F, s, s, -s, 1.0F, 0.0F, s, -s, -s, 1.0F, 1.0F, s, -s, -s, 1.0F, 1.0F, -s, -s, -s, 0.0F, 1.0F, -s, s, -s, 0.0F, 0.0F, -s, s, -s, 0.0F, 0.0F, -s, -s, -s, 0.0F, 1.0F, -s, -s, s, 1.0F, 1.0F, -s, -s, s, 1.0F, 1.0F, -s, s, s, 1.0F, 0.0F, -s, s, -s, 0.0F, 0.0F, s, s, s, 0.0F, 0.0F, s, -s, s, 0.0F, 1.0F, s, -s, -s, 1.0F, 1.0F, s, -s, -s, 1.0F, 1.0F, s, s, -s, 1.0F, 0.0F, s, s, s, 0.0F, 0.0F, -s, s, -s, 0.0F, 0.0F, -s, s, s, 0.0F, 1.0F, s, s, s, 1.0F, 1.0F, s, s, s, 1.0F, 1.0F, s, s, -s, 1.0F, 0.0F, -s, s, -s, 0.0F, 0.0F, -s, -s, s, 0.0F, 0.0F, -s, -s, -s, 0.0F, 1.0F, s, -s, -s, 1.0F, 1.0F, s, -s, -s, 1.0F, 1.0F, s, -s, s, 1.0F, 0.0F, -s, -s, s, 0.0F, 0.0F};
         this.vao = GL30.glGenVertexArrays();
         this.vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vao);
         GL15.glBindBuffer(34962, this.vbo);
         FloatBuffer fb = BufferUtils.createFloatBuffer(vertices.length);
         fb.put(vertices).flip();
         GL15.glBufferData(34962, fb, 35044);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 20, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 2, 5126, false, 20, 12L);
         GL20.glEnableVertexAttribArray(1);
         GL30.glBindVertexArray(0);
         this.initialized = true;
      }
   }

   public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix, float tickDelta, Camera camera) {
      this.init();
      int prevProgram = GL11.glGetInteger(35725);
      int prevVAO = GL11.glGetInteger(34229);
      int prevVBO = GL11.glGetInteger(34964);
      int prevTex = GL11.glGetInteger(32873);
      boolean depth = GL11.glIsEnabled(2929);
      boolean blend = GL11.glIsEnabled(3042);
      boolean cull = GL11.glIsEnabled(2884);
      this.shader.use();
      MemoryStack stack = MemoryStack.stackPush();

      try {
         FloatBuffer fb = stack.mallocFloat(16);
         Matrix4f skyView = new Matrix4f(viewMatrix);
         skyView.m30(0.0F);
         skyView.m31(0.0F);
         skyView.m32(0.0F);
         skyView.get(fb);
         GL20.glUniformMatrix4fv(this.shader.getUniformLocation("uView"), false, fb);
         projectionMatrix.get(fb);
         GL20.glUniformMatrix4fv(this.shader.getUniformLocation("uProj"), false, fb);
         Color c = (Color)this.color.getValue();
         GL20.glUniform4f(this.shader.getUniformLocation("uColor"), (float)c.getRed() / 255.0F, (float)c.getGreen() / 255.0F, (float)c.getBlue() / 255.0F, (float)c.getAlpha() / 255.0F);
      } catch (Throwable var17) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (stack != null) {
         stack.close();
      }

      int texId = this.getTextureId(this.nebulaTexture);
      if (texId != -1) {
         GL13.glActiveTexture(33984);
         GL11.glBindTexture(3553, texId);
         GL20.glUniform1i(this.shader.getUniformLocation("uTexture"), 0);
      }

      GL11.glDisable(2929);
      GL11.glEnable(3042);
      GL11.glBlendFunc(770, 771);
      GL11.glDisable(2884);
      GL30.glBindVertexArray(this.vao);
      GL11.glDrawArrays(4, 0, 36);
      if (depth) {
         GL11.glEnable(2929);
      } else {
         GL11.glDisable(2929);
      }

      if (blend) {
         GL11.glEnable(3042);
      } else {
         GL11.glDisable(3042);
      }

      if (cull) {
         GL11.glEnable(2884);
      } else {
         GL11.glDisable(2884);
      }

      GL11.glBindTexture(3553, prevTex);
      GL30.glBindVertexArray(prevVAO);
      GL15.glBindBuffer(34962, prevVBO);
      GL20.glUseProgram(prevProgram);
   }

   private int getTextureId(Identifier id) {
      AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(id);
      if (tex != null) {
         try {
            Method[] var3 = tex.getClass().getMethods();
            int var4 = var3.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               Method m = var3[var5];
               if (m.getParameterCount() == 0 && m.getReturnType() == Integer.TYPE) {
                  String name = m.getName();
                  if (name.equals("getGlId") || name.equals("getId") || name.contains("textureId") || name.equals("getGlRef")) {
                     return (Integer)m.invoke(tex);
                  }
               }
            }
         } catch (Exception var8) {
            var8.printStackTrace();
         }
      }

      return -1;
   }
}
