package foure.dev.util.render.backends.gl;

import java.nio.FloatBuffer;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

public class Simple3DBackend {
   private ShaderProgram shader;
   private int vao;
   private int vbo;
   private boolean initialized = false;
   private FloatBuffer buffer;
   private int vertexCount;
   private static final int MAX_VERTICES = 10000;
   private static final int STRIDE_FLOATS = 7;
   private float lineWidth = 1.0F;
   private boolean depthTestEnabled = false;
   private final String VERTEX_SHADER = "#version 330 core\nlayout(location = 0) in vec3 aPos;\nlayout(location = 1) in vec4 aColor;\nuniform mat4 uView;\nuniform mat4 uProj;\nuniform int uDebug;\nout vec4 vColor;\nvoid main() {\n    if (uDebug == 1) {\n        gl_Position = vec4(aPos, 1.0);\n    } else {\n        gl_Position = uProj * uView * vec4(aPos, 1.0);\n    }\n    vColor = aColor;\n}\n";
   private final String FRAGMENT_SHADER = "#version 330 core\nin vec4 vColor;\nout vec4 FragColor;\nvoid main() {\n    FragColor = vColor;\n}\n";
   private int vaoQuad;
   private int vboQuad;
   private FloatBuffer bufferQuad;
   private int vertexCountQuad;

   public void setLineWidth(float width) {
      this.lineWidth = width;
   }

   public void setDepthTest(boolean enabled) {
      this.depthTestEnabled = enabled;
   }

   public void init() {
      if (!this.initialized) {
         this.shader = new ShaderProgram("#version 330 core\nlayout(location = 0) in vec3 aPos;\nlayout(location = 1) in vec4 aColor;\nuniform mat4 uView;\nuniform mat4 uProj;\nuniform int uDebug;\nout vec4 vColor;\nvoid main() {\n    if (uDebug == 1) {\n        gl_Position = vec4(aPos, 1.0);\n    } else {\n        gl_Position = uProj * uView * vec4(aPos, 1.0);\n    }\n    vColor = aColor;\n}\n", "#version 330 core\nin vec4 vColor;\nout vec4 FragColor;\nvoid main() {\n    FragColor = vColor;\n}\n");
         this.vao = GL30.glGenVertexArrays();
         this.vbo = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vao);
         GL15.glBindBuffer(34962, this.vbo);
         GL15.glBufferData(34962, 280000L, 35048);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 28, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 4, 5126, false, 28, 12L);
         GL20.glEnableVertexAttribArray(1);
         this.vaoQuad = GL30.glGenVertexArrays();
         this.vboQuad = GL15.glGenBuffers();
         GL30.glBindVertexArray(this.vaoQuad);
         GL15.glBindBuffer(34962, this.vboQuad);
         GL15.glBufferData(34962, 280000L, 35048);
         GL20.glVertexAttribPointer(0, 3, 5126, false, 28, 0L);
         GL20.glEnableVertexAttribArray(0);
         GL20.glVertexAttribPointer(1, 4, 5126, false, 28, 12L);
         GL20.glEnableVertexAttribArray(1);
         GL30.glBindVertexArray(0);
         this.buffer = BufferUtils.createFloatBuffer(70000);
         this.bufferQuad = BufferUtils.createFloatBuffer(70000);
         this.initialized = true;
      }
   }

   public void begin() {
      if (!this.initialized) {
         this.init();
      }

      this.buffer.clear();
      this.vertexCount = 0;
      this.bufferQuad.clear();
      this.vertexCountQuad = 0;
   }

   public void line(float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
      if (this.vertexCount + 2 <= 10000) {
         this.buffer.put(x1).put(y1).put(z1).put(r).put(g).put(b).put(a);
         this.buffer.put(x2).put(y2).put(z2).put(r).put(g).put(b).put(a);
         this.vertexCount += 2;
      }
   }

   public void quad(float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float r, float g, float b, float a) {
      if (this.vertexCountQuad + 6 <= 10000) {
         this.bufferQuad.put(x1).put(y1).put(z1).put(r).put(g).put(b).put(a);
         this.bufferQuad.put(x2).put(y2).put(z2).put(r).put(g).put(b).put(a);
         this.bufferQuad.put(x4).put(y4).put(z4).put(r).put(g).put(b).put(a);
         this.bufferQuad.put(x2).put(y2).put(z2).put(r).put(g).put(b).put(a);
         this.bufferQuad.put(x3).put(y3).put(z3).put(r).put(g).put(b).put(a);
         this.bufferQuad.put(x4).put(y4).put(z4).put(r).put(g).put(b).put(a);
         this.vertexCountQuad += 6;
      }
   }

   public void debugNDC() {
      if (this.vertexCountQuad + 3 <= 10000) {
         this.bufferQuad.put(-0.5F).put(-0.5F).put(0.0F).put(1.0F).put(0.0F).put(0.0F).put(1.0F);
         this.bufferQuad.put(0.5F).put(-0.5F).put(0.0F).put(1.0F).put(0.0F).put(0.0F).put(1.0F);
         this.bufferQuad.put(0.0F).put(0.5F).put(0.0F).put(1.0F).put(0.0F).put(0.0F).put(1.0F);
         this.vertexCountQuad += 3;
      }
   }

   public void end(Matrix4f view, Matrix4f proj) {
      if (this.initialized) {
         if (this.vertexCount != 0 || this.vertexCountQuad != 0) {
            this.buffer.flip();
            this.bufferQuad.flip();
            int prevProgram = GL11.glGetInteger(35725);
            int prevVAO = GL11.glGetInteger(34229);
            int prevVBO = GL11.glGetInteger(34964);
            boolean blend = GL11.glIsEnabled(3042);
            boolean depth = GL11.glIsEnabled(2929);
            boolean cull = GL11.glIsEnabled(2884);
            boolean scissor = GL11.glIsEnabled(3089);
            boolean stencil = GL11.glIsEnabled(2960);
            float prevLineWidth = GL11.glGetFloat(2849);
            this.shader.use();
            MemoryStack stack = MemoryStack.stackPush();

            try {
               FloatBuffer fb = stack.mallocFloat(16);
               view.get(fb);
               GL20.glUniformMatrix4fv(this.shader.getUniformLocation("uView"), false, fb);
               proj.get(fb);
               GL20.glUniformMatrix4fv(this.shader.getUniformLocation("uProj"), false, fb);
               if (this.vertexCount <= 0 && this.vertexCountQuad > 0) {
               }
            } catch (Throwable var16) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var15) {
                     var16.addSuppressed(var15);
                  }
               }

               throw var16;
            }

            if (stack != null) {
               stack.close();
            }

            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            if (this.depthTestEnabled) {
               GL11.glEnable(2929);
               GL11.glDepthFunc(515);
            } else {
               GL11.glDisable(2929);
            }

            GL11.glDisable(2884);
            GL11.glDisable(3089);
            GL11.glDisable(2960);
            GL11.glLineWidth(this.lineWidth);
            int debugLoc = this.shader.getUniformLocation("uDebug");
            if (this.vertexCount > 0) {
               GL20.glUniform1i(debugLoc, 0);
               GL30.glBindVertexArray(this.vao);
               GL15.glBindBuffer(34962, this.vbo);
               GL15.glBufferSubData(34962, 0L, this.buffer);
               GL11.glDrawArrays(1, 0, this.vertexCount);
            }

            if (this.vertexCountQuad > 0) {
               if (this.vertexCountQuad == 3) {
                  GL20.glUniform1i(debugLoc, 1);
               } else {
                  GL20.glUniform1i(debugLoc, 0);
               }

               GL30.glBindVertexArray(this.vaoQuad);
               GL15.glBindBuffer(34962, this.vboQuad);
               GL15.glBufferSubData(34962, 0L, this.bufferQuad);
               GL11.glDrawArrays(4, 0, this.vertexCountQuad);
            }

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

            if (scissor) {
               GL11.glEnable(3089);
            } else {
               GL11.glDisable(3089);
            }

            if (stencil) {
               GL11.glEnable(2960);
            } else {
               GL11.glDisable(2960);
            }

            GL11.glLineWidth(prevLineWidth);
            GL30.glBindVertexArray(prevVAO);
            GL15.glBindBuffer(34962, prevVBO);
            GL20.glUseProgram(prevProgram);
         }
      }
   }
}
