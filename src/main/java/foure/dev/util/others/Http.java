package foure.dev.util.others;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Http {
   private static final Gson GSON = new Gson();

   public static Http.Request getName(String url) {
      return new Http.Request("GET", url);
   }

   public static Http.Request post(String url) {
      return new Http.Request("POST", url);
   }

   public static class Request {
      private final String method;
      private final String url;
      private String body;
      private String contentType;
      private String authorization;

      private Request(String method, String url) {
         this.method = method;
         this.url = url;
      }

      public Http.Request bodyJson(String json) {
         this.body = json;
         this.contentType = "application/json";
         return this;
      }

      public Http.Request bodyForm(String form) {
         this.body = form;
         this.contentType = "application/x-www-form-urlencoded";
         return this;
      }

      public Http.Request bearer(String token) {
         this.authorization = "Bearer " + token;
         return this;
      }

      public <T> T sendJson(Class<T> responseClass) {
         try {
            HttpURLConnection conn = (HttpURLConnection)(new URL(this.url)).openConnection();
            conn.setRequestMethod(this.method);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            if (this.contentType != null) {
               conn.setRequestProperty("Content-Type", this.contentType);
            }

            if (this.authorization != null) {
               conn.setRequestProperty("Authorization", this.authorization);
            }

            if (this.body != null) {
               conn.setDoOutput(true);
               OutputStream os = conn.getOutputStream();

               try {
                  byte[] input = this.body.getBytes(StandardCharsets.UTF_8);
                  os.write(input, 0, input.length);
               } catch (Throwable var7) {
                  if (os != null) {
                     try {
                        os.close();
                     } catch (Throwable var6) {
                        var7.addSuppressed(var6);
                     }
                  }

                  throw var7;
               }

               if (os != null) {
                  os.close();
               }
            }

            int responseCode = conn.getResponseCode();
            return responseCode >= 200 && responseCode < 300 ? Http.GSON.fromJson(new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8), responseClass) : null;
         } catch (IOException var8) {
            System.err.println("HTTP request failed: " + var8.getMessage());
            return null;
         }
      }
   }
}
