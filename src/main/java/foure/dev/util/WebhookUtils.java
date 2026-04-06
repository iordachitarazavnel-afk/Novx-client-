package foure.dev.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public class WebhookUtils {
   private static final String WEBHOOK_AVATAR = "https://i.imgur.com/0K2q9D9.png";
   private static final String WEBHOOK_NAME = "Hysteria Alert";
   private static final String FOOTER_TEXT = "Hysteria Client";
   private static final int EMBED_COLOR = 16711680;
   private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10L)).build();
   private final String webhookUrl;
   private final List<WebhookUtils.Field> fields = new ArrayList();
   private String title = "";
   private String description = "";
   private String selfPingId = "";
   private boolean includeCoords = false;
   private boolean includeUsername = false;
   private String username = "";
   private boolean includeServer = false;
   private boolean includeTime = false;
   private File screenshotFile = null;

   public WebhookUtils(String webhookUrl) {
      this.webhookUrl = webhookUrl;
   }

   public WebhookUtils setTitle(String title) {
      this.title = title;
      return this;
   }

   public WebhookUtils setDescription(String description) {
      this.description = description;
      return this;
   }

   public WebhookUtils setSelfPing(String discordId) {
      this.selfPingId = discordId;
      return this;
   }

   public WebhookUtils addCoords() {
      this.includeCoords = true;
      return this;
   }

   public WebhookUtils addCoords(BlockPos pos) {
      this.includeCoords = true;
      if (pos != null) {
         this.fields.add(new WebhookUtils.Field("Coordinates", String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()), true));
      }

      return this;
   }

   public WebhookUtils addUsername() {
      this.includeUsername = true;
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.player != null) {
         this.username = mc.player.getName().getString();
      }

      return this;
   }

   public WebhookUtils addUsername(String username) {
      this.includeUsername = true;
      this.username = username;
      return this;
   }

   public WebhookUtils addField(String name, String value, boolean inline) {
      this.fields.add(new WebhookUtils.Field(name, value, inline));
      return this;
   }

   public WebhookUtils addServer() {
      this.includeServer = true;
      return this;
   }

   public WebhookUtils addTime() {
      this.includeTime = true;
      return this;
   }

   public WebhookUtils setScreenshot(File screenshotFile) {
      this.screenshotFile = screenshotFile;
      return this;
   }

   public void send() {
      if (this.webhookUrl != null && !this.webhookUrl.trim().isEmpty()) {
         CompletableFuture.runAsync(() -> {
            try {
               MinecraftClient mc = MinecraftClient.getInstance();
               String messageContent = "";
               if (!this.selfPingId.trim().isEmpty()) {
                  messageContent = String.format("<@%s>", this.selfPingId.trim());
               }

               if (this.includeCoords && mc != null && mc.player != null) {
                  BlockPos pos = mc.player.getBlockPos();
                  if (this.fields.stream().noneMatch((f) -> {
                     return f.name.equals("Coordinates");
                  })) {
                     this.fields.add(new WebhookUtils.Field("Coordinates", String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()), true));
                  }
               }

               if (this.includeUsername && this.username.isEmpty() && mc != null && mc.player != null) {
                  this.username = mc.player.getName().getString();
               }

               if (this.includeServer) {
                  String serverInfo = mc != null && mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Unknown Server";
                  if (this.fields.stream().noneMatch((f) -> {
                     return f.name.equals("Server");
                  })) {
                     this.fields.add(new WebhookUtils.Field("Server", serverInfo, true));
                  }
               }

               if (this.includeTime) {
                  long timestampSeconds = System.currentTimeMillis() / 1000L;
                  if (this.fields.stream().noneMatch((f) -> {
                     return f.name.equals("Time");
                  })) {
                     this.fields.add(new WebhookUtils.Field("Time", "<t:" + timestampSeconds + ":R>", true));
                  }
               }

               StringBuilder fieldsJson = new StringBuilder();
               if (this.includeUsername && !this.username.isEmpty()) {
                  fieldsJson.append(String.format("{\n    \"name\": \"Username\",\n    \"value\": \"%s\",\n    \"inline\": true\n}", this.escapeJson(this.username)));
               }

               for(int i = 0; i < this.fields.size(); ++i) {
                  if (i > 0 || this.includeUsername) {
                     fieldsJson.append(",");
                  }

                  WebhookUtils.Field field = (WebhookUtils.Field)this.fields.get(i);
                  fieldsJson.append(String.format("{\n    \"name\": \"%s\",\n    \"value\": \"%s\",\n    \"inline\": %s\n}", this.escapeJson(field.name), this.escapeJson(field.value), field.inline));
               }

               String thumbnailUrl = "";
               if (this.includeUsername && !this.username.isEmpty()) {
                  thumbnailUrl = String.format("https://vzge.me/bust/%s.png?y=-40", this.username);
               }

               String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
               String imageJson = "";
               if (this.screenshotFile != null && this.screenshotFile.exists()) {
                  imageJson = ",\n                            \"image\": {\n                                \"url\": \"attachment://screenshot.png\"\n                            }";
               }

               String jsonPayload = String.format("{\n    \"content\": \"%s\",\n    \"username\": \"%s\",\n    \"avatar_url\": \"%s\",\n    \"embeds\": [{\n        \"title\": \"%s\",\n        \"description\": \"%s\",\n        \"color\": %d,\n        \"fields\": [%s],\n        \"footer\": {\n            \"text\": \"%s\"\n        },\n        \"timestamp\": \"%sZ\"%s%s\n    }]\n}", this.escapeJson(messageContent), this.escapeJson("Hysteria Alert"), "https://i.imgur.com/0K2q9D9.png", this.escapeJson(this.title), this.escapeJson(this.description), 16711680, fieldsJson, this.escapeJson("Hysteria Client"), timestamp, !thumbnailUrl.isEmpty() ? String.format(",\n                            \"thumbnail\": {\n                                \"url\": \"%s\"\n                            }", thumbnailUrl) : "", imageJson);
               if (this.screenshotFile != null && this.screenshotFile.exists()) {
                  this.sendWithScreenshot(jsonPayload, this.screenshotFile);
               } else {
                  HttpRequest request = HttpRequest.newBuilder().uri(URI.create(this.webhookUrl)).header("Content-Type", "application/json").header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36").POST(BodyPublishers.ofString(jsonPayload)).timeout(Duration.ofSeconds(30L)).build();
                  System.out.println("[Webhook] Sending payload to " + this.webhookUrl);
                  HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
                  PrintStream var10000 = System.out;
                  int var10001 = response.statusCode();
                  var10000.println("[Webhook] Response: " + var10001 + " " + (String)response.body());
               }
            } catch (Exception var10) {
               var10.printStackTrace();
            }

         });
      }
   }

   private void sendWithScreenshot(String jsonPayload, File screenshotFile) {
      try {
         String boundary = "----Boundary" + System.currentTimeMillis();
         HttpURLConnection conn = (HttpURLConnection)(new URI(this.webhookUrl)).toURL().openConnection();
         conn.setDoOutput(true);
         conn.setRequestMethod("POST");
         conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
         conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
         conn.setConnectTimeout(30000);
         conn.setReadTimeout(30000);
         OutputStream out = conn.getOutputStream();

         try {
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n".getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            out.write("Content-Disposition: form-data; name=\"file1\"; filename=\"screenshot.png\"\r\n".getBytes(StandardCharsets.UTF_8));
            out.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            FileInputStream fis = new FileInputStream(screenshotFile);

            try {
               byte[] buffer = new byte[4096];

               int bytesRead;
               while((bytesRead = fis.read(buffer)) != -1) {
                  out.write(buffer, 0, bytesRead);
               }
            } catch (Throwable var11) {
               try {
                  fis.close();
               } catch (Throwable var10) {
                  var11.addSuppressed(var10);
               }

               throw var11;
            }

            fis.close();
            out.write("\r\n".getBytes(StandardCharsets.UTF_8));
            out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
         } catch (Throwable var12) {
            if (out != null) {
               try {
                  out.close();
               } catch (Throwable var9) {
                  var12.addSuppressed(var9);
               }
            }

            throw var12;
         }

         if (out != null) {
            out.close();
         }

         System.out.println("[Webhook] Sending screenshot payload to " + this.webhookUrl);
         int responseCode = conn.getResponseCode();
         System.out.println("[Webhook] Response Code: " + responseCode);
         conn.disconnect();
         Files.deleteIfExists(screenshotFile.toPath());
      } catch (Exception var13) {
         var13.printStackTrace();
      }

   }

   private String escapeJson(String str) {
      return str == null ? "" : str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   public static class Field {
      public String name;
      public String value;
      public boolean inline;

      public Field(String name, String value, boolean inline) {
         this.name = name;
         this.value = value;
         this.inline = inline;
      }
   }
}
