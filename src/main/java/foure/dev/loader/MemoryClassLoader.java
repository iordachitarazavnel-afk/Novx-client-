package foure.dev.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.SecureClassLoader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class MemoryClassLoader extends SecureClassLoader {
   private final Map<String, byte[]> classes = new HashMap();
   private final Map<String, byte[]> resources = new HashMap();

   public MemoryClassLoader(ClassLoader parent, byte[] jarBytes) throws IOException {
      super(parent);
      this.loadJar(jarBytes);
   }

   private void loadJar(byte[] jarBytes) throws IOException {
      JarInputStream jis = new JarInputStream(new ByteArrayInputStream(jarBytes));

      JarEntry entry;
      try {
         while((entry = jis.getNextJarEntry()) != null) {
            if (!entry.isDirectory()) {
               ByteArrayOutputStream buffer = new ByteArrayOutputStream();
               byte[] data = new byte[1024];

               int nRead;
               while((nRead = jis.read(data, 0, data.length)) != -1) {
                  buffer.write(data, 0, nRead);
               }

               String name = entry.getName();
               if (name.endsWith(".class")) {
                  String className = name.replace('/', '.').replace(".class", "");
                  this.classes.put(className, buffer.toByteArray());
               } else {
                  this.resources.put(name, buffer.toByteArray());
               }
            }
         }
      } catch (Throwable var10) {
         try {
            jis.close();
         } catch (Throwable var9) {
            var10.addSuppressed(var9);
         }

         throw var10;
      }

      jis.close();
   }

   protected Class<?> findClass(String name) throws ClassNotFoundException {
      byte[] bytes = (byte[])this.classes.get(name);
      return bytes != null ? this.defineClass(name, bytes, 0, bytes.length) : super.findClass(name);
   }

   public URL findResource(String name) {
      if (this.resources.containsKey(name)) {
         try {
            return new URL("memory", (String)null, -1, name, new MemoryClassLoader.MemoryURLStreamHandler((byte[])this.resources.get(name)));
         } catch (MalformedURLException var3) {
            var3.printStackTrace();
         }
      }

      return super.findResource(name);
   }

   public Enumeration<URL> findResources(String name) throws IOException {
      URL url = this.findResource(name);
      return url != null ? Collections.enumeration(Collections.singleton(url)) : super.findResources(name);
   }

   private static class MemoryURLStreamHandler extends URLStreamHandler {
      private final byte[] data;

      public MemoryURLStreamHandler(byte[] data) {
         this.data = data;
      }

      protected URLConnection openConnection(URL u) {
         return new URLConnection(u) {

            @Override
            public void connect() {
               // no-op
            }

            @Override
            public InputStream getInputStream() {
               return new ByteArrayInputStream(data);
            }
         };
      }
   }
}
