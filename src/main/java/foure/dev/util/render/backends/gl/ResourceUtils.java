package foure.dev.util.render.backends.gl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.lwjgl.BufferUtils;

public final class ResourceUtils {
   private ResourceUtils() {
   }

   public static String readText(String path) {
      ClassLoader cl = ResourceUtils.class.getClassLoader();

      try {
         InputStream in = cl.getResourceAsStream(path);

         String var6;
         try {
            if (in == null) {
               throw new IllegalStateException("Resource not found: " + path);
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            try {
               StringBuilder sb = new StringBuilder();

               while(true) {
                  String line;
                  if ((line = br.readLine()) == null) {
                     var6 = sb.toString();
                     break;
                  }

                  sb.append(line).append('\n');
               }
            } catch (Throwable var9) {
               try {
                  br.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }

               throw var9;
            }

            br.close();
         } catch (Throwable var10) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (in != null) {
            in.close();
         }

         return var6;
      } catch (IOException var11) {
         throw new RuntimeException("Failed to read resource: " + path, var11);
      }
   }

   public static ByteBuffer readBinary(String path) {
      ClassLoader cl = ResourceUtils.class.getClassLoader();

      try {
         InputStream in = cl.getResourceAsStream(path);

         ByteBuffer var5;
         try {
            if (in == null) {
               throw new IllegalStateException("Resource not found: " + path);
            }

            byte[] bytes = in.readAllBytes();
            ByteBuffer buffer = BufferUtils.createByteBuffer(bytes.length);
            buffer.put(bytes).flip();
            var5 = buffer;
         } catch (Throwable var7) {
            if (in != null) {
               try {
                  in.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (in != null) {
            in.close();
         }

         return var5;
      } catch (IOException var8) {
         throw new RuntimeException("Failed to read resource: " + path, var8);
      }
   }
}
