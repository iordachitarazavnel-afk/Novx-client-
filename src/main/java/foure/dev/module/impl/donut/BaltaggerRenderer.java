package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.Render3DEvent;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class BaltaggerRenderer {
   private static final BaltaggerRenderer INSTANCE = new BaltaggerRenderer();
   private final Map<String, Long> moneyCache = new ConcurrentHashMap();
   private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
   private final Set<String> failedRequests = ConcurrentHashMap.newKeySet();
   private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final Gson gson = new Gson();
   private boolean registered = false;

   private BaltaggerRenderer() {
   }

   public static BaltaggerRenderer getInstance() {
      return INSTANCE;
   }

   public static void register() {
      if (!INSTANCE.registered) {
         FourEClient.getInstance().getEventBus().register(INSTANCE);
         INSTANCE.registered = true;
      }

   }

   public void onTick(MinecraftClient client) {
      if (client.world != null && client.player != null) {
         Baltagger mod = (Baltagger)FourEClient.getInstance().getFunctionManager().getModule(Baltagger.class);
         if (mod != null && mod.isToggled()) {
            if (!mod.getApiKey().isEmpty()) {
               Iterator var3 = client.world.getPlayers().iterator();

               while(true) {
                  AbstractClientPlayerEntity player;
                  do {
                     do {
                        if (!var3.hasNext()) {
                           return;
                        }

                        player = (AbstractClientPlayerEntity)var3.next();
                     } while(player == null);
                  } while(!mod.shouldShowSelf() && player == client.player);

                  String playerName = player.getName().getString();
                  if (playerName != null && !playerName.isEmpty() && !this.moneyCache.containsKey(playerName) && !this.pendingRequests.contains(playerName) && !this.failedRequests.contains(playerName)) {
                     this.fetchPlayerMoney(playerName, mod);
                  }
               }
            }
         }
      }
   }

   public boolean shouldCancelNametagRendering(Entity entity) {
      return false;
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null && client.player != null) {
         Baltagger mod = (Baltagger)FourEClient.getInstance().getFunctionManager().getModule(Baltagger.class);
         if (mod != null && mod.isToggled()) {
            if (!mod.getApiKey().isEmpty()) {
               Matrix4f matrix = event.getMatrix();
               Camera camera = event.getCamera();
               Immediate vertexConsumers = client.getBufferBuilders().getEntityVertexConsumers();
               float tickDelta = event.getTickCounter().getTickProgress(true);
               Vec3d camPos = client.player.getCameraPosVec(tickDelta);
               double camX = camPos.x;
               double camY = camPos.y;
               double camZ = camPos.z;
               TextRenderer tr = client.textRenderer;
               Iterator var16 = client.world.getPlayers().iterator();

               while(true) {
                  AbstractClientPlayerEntity player;
                  do {
                     do {
                        if (!var16.hasNext()) {
                           return;
                        }

                        player = (AbstractClientPlayerEntity)var16.next();
                     } while(player == null);
                  } while(player == client.player && !mod.shouldShowSelf());

                  String name = player.getName().getString();
                  if (name != null && !name.isEmpty()) {
                     Long money = this.getMoney(name);
                     if (money != null) {
                        String moneyText = this.formatMoney(money);
                        int moneyColor = mod.getMoneyColor() | -16777216;
                        double x = MathHelper.lerp((double)tickDelta, player.lastRenderX, player.getX()) - camX;
                        double y = MathHelper.lerp((double)tickDelta, player.lastRenderY, player.getY()) - camY + (double)player.getHeight() + 0.6D;
                        double z = MathHelper.lerp((double)tickDelta, player.lastRenderZ, player.getZ()) - camZ;
                        new MatrixStack();
                        Matrix4f modelMatrix = new Matrix4f(matrix);
                        modelMatrix.translate((float)x, (float)y, (float)z);
                        modelMatrix.rotate((float)Math.toRadians((double)(-camera.getYaw())), 0.0F, 1.0F, 0.0F);
                        modelMatrix.rotate((float)Math.toRadians((double)camera.getPitch()), 1.0F, 0.0F, 0.0F);
                        modelMatrix.scale(-0.025F, -0.025F, 0.025F);
                        int hearts = Math.round(player.getHealth() + player.getAbsorptionAmount());
                        int heartColor = -65536;
                        String fullText = hearts + " ❤ " + moneyText;
                        int totalWidth = tr.getWidth(fullText);
                        float startX = (float)(-totalWidth) / 2.0F;
                        Objects.requireNonNull(tr);
                        int overlayY = -9;
                        int currentX = 0;
                        String heartsText = String.valueOf(hearts);
                        tr.draw(heartsText, startX + (float)currentX, (float)overlayY, -1, false, modelMatrix, vertexConsumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                        currentX = currentX + tr.getWidth(heartsText);
                        tr.draw(" ❤ ", startX + (float)currentX, (float)overlayY, heartColor, false, modelMatrix, vertexConsumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                        currentX += tr.getWidth(" ❤ ");
                        tr.draw(moneyText, startX + (float)currentX, (float)overlayY, moneyColor, false, modelMatrix, vertexConsumers, TextLayerType.SEE_THROUGH, 0, 15728880);
                     }
                  }
               }
            }
         }
      }
   }

   private void fetchPlayerMoney(String playerName, Baltagger mod) {
      if (!mod.getApiKey().isEmpty()) {
         this.pendingRequests.add(playerName);
         HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.donutsmp.net/v1/stats/" + playerName)).header("accept", "application/json").header("Authorization", mod.getApiKey()).timeout(Duration.ofSeconds(10L)).GET().build();
         this.httpClient.sendAsync(request, BodyHandlers.ofString()).thenAccept((response) -> {
            this.pendingRequests.remove(playerName);
            if (response.statusCode() == 200) {
               try {
                  JsonObject json = (JsonObject)this.gson.fromJson((String)response.body(), JsonObject.class);
                  JsonObject result = json.getAsJsonObject("result");
                  String moneyStr = result.get("money").getAsString();
                  double moneyDouble = Double.parseDouble(moneyStr);
                  long money = (long)moneyDouble;
                  this.moneyCache.put(playerName, money);
               } catch (Exception var10) {
                  this.failedRequests.add(playerName);
               }
            } else {
               this.failedRequests.add(playerName);
            }

         }).exceptionally((e) -> {
            this.pendingRequests.remove(playerName);
            this.failedRequests.add(playerName);
            return null;
         });
      }
   }

   public void clearCache() {
      this.moneyCache.clear();
      this.pendingRequests.clear();
      this.failedRequests.clear();
   }

   public Long getMoney(String playerName) {
      return (Long)this.moneyCache.get(playerName);
   }

   public String formatMoney(long money) {
      double value;
      if (money >= 1000000000L) {
         value = (double)money / 1.0E9D;
         return this.formatCompact(value, "B");
      } else if (money >= 1000000L) {
         value = (double)money / 1000000.0D;
         return this.formatCompact(value, "M");
      } else if (money >= 1000L) {
         value = (double)money / 1000.0D;
         return this.formatCompact(value, "k");
      } else {
         return "$" + money;
      }
   }

   private String formatCompact(double value, String unit) {
      String s = String.format("%.1f", value);
      if (s.endsWith(".0")) {
         s = s.substring(0, s.length() - 2);
      }

      return "$" + s + unit;
   }
}
