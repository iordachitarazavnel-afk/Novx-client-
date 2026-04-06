package foure.dev.util.render;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public class PlayerHeadManager {
   private static final PlayerHeadManager INSTANCE = new PlayerHeadManager();
   private final Map<String, Identifier> headCache = new HashMap();
   private final Map<String, BufferedImage> pendingImages = new HashMap();
   private final ExecutorService executor = Executors.newFixedThreadPool(2);

   private PlayerHeadManager() {
   }

   public static PlayerHeadManager getInstance() {
      return INSTANCE;
   }

   public Identifier getHead(String username) {
      if (this.headCache.containsKey(username)) {
         return (Identifier)this.headCache.get(username);
      } else if (this.pendingImages.containsKey(username)) {
         BufferedImage image = (BufferedImage)this.pendingImages.remove(username);
         this.registerTexture(username, image);
         return (Identifier)this.headCache.get(username);
      } else {
         this.fetchHead(username);
         return null;
      }
   }

   private void fetchHead(String username) {
      if (!this.headCache.containsKey(username)) {
         this.executor.submit(() -> {
            try {
               URL url = new URL("https://vzge.me/face/256/" + username);
               BufferedImage image = ImageIO.read(url);
               if (image != null) {
                  synchronized(this.pendingImages) {
                     this.pendingImages.put(username, image);
                  }
               }
            } catch (Exception var7) {
               var7.printStackTrace();
            }

         });
      }
   }

   private void registerTexture(String username, BufferedImage image) {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(image, "png", baos);
         byte[] bytes = baos.toByteArray();
         NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
         NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> {
            return "player_head_" + username;
         }, nativeImage);
         MinecraftClient mc = MinecraftClient.getInstance();
         Identifier id = Identifier.of("hysteria", "player_head_" + username + "_" + System.currentTimeMillis());
         mc.getTextureManager().registerTexture(id, texture);
         this.headCache.put(username, id);
      } catch (Exception var9) {
         var9.printStackTrace();
      }

   }
}
