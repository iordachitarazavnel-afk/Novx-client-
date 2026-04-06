package foure.dev.loader;

import java.lang.reflect.Method;

public class LoaderBootstrap {
   public static void launch(byte[] jarData, String[] args) {
      try {
         System.out.println("[Loader] Initializing Memory ClassLoader...");
         MemoryClassLoader memoryLoader = new MemoryClassLoader(LoaderBootstrap.class.getClassLoader(), jarData);
         String mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient";
         System.out.println("[Loader] Loading Main Class: " + mainClass);
         Class<?> mainClazz = memoryLoader.loadClass(mainClass);
         Method mainMethod = mainClazz.getMethod("main", String[].class);
         Thread.currentThread().setContextClassLoader(memoryLoader);
         System.out.println("[Loader] Launching Client...");
         mainMethod.invoke((Object)null, args);
      } catch (Exception var6) {
         var6.printStackTrace();
         System.err.println("[Loader] Failed to launch from memory: " + var6.getMessage());
      }

   }
}
