package foure.dev.util;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public class PingUtils {
   private static final AtomicLong cachedPing = new AtomicLong(-1L);
   private static long lastPingTime = 0L;

   public static long getCachedPing() {
      return cachedPing.get();
   }

   public static void updatePingAsync() {
      long now = System.currentTimeMillis();
      if (now - lastPingTime >= 1000L) {
         lastPingTime = now;
         (new Thread(() -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getCurrentServerEntry() != null) {
               ServerInfo server = mc.getCurrentServerEntry();
               String address = server.address;
               String[] parts = address.split(":");
               String host = parts[0];
               int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

               try {
                  Socket socket = new Socket();

                  try {
                     long start = System.nanoTime();
                     socket.connect(new InetSocketAddress(host, port), 1000);
                     long end = System.nanoTime();
                     cachedPing.set((end - start) / 1000000L);
                  } catch (Throwable var12) {
                     try {
                        socket.close();
                     } catch (Throwable var11) {
                        var12.addSuppressed(var11);
                     }

                     throw var12;
                  }

                  socket.close();
               } catch (Exception var13) {
                  cachedPing.set(-1L);
               }

            } else {
               cachedPing.set(-1L);
            }
         })).start();
      }
   }
}
