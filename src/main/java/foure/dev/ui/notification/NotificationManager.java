package foure.dev.ui.notification;

import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.MinecraftClient;

public class NotificationManager {
   private static final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList();
   private static final int MAX_NOTIFICATIONS = 5;

   public static void add(String title, String message, NotificationType type) {
      Iterator var3 = notifications.iterator();

      while(var3.hasNext()) {
         Notification n = (Notification)var3.next();
         if (n.getTitle().equalsIgnoreCase(title) && !n.isExiting()) {
            n.forceExit();
         }
      }

      notifications.add(new Notification(title, message, type));
   }

   public static void render(Renderer2D renderer) {
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc != null && mc.getWindow() != null) {
         float screenWidth = (float)mc.getWindow().getScaledWidth();
         float screenHeight = (float)mc.getWindow().getScaledHeight();
         float bottomOffset = 60.0F;
         float itemHeight = 35.0F;
         float padding = 8.0F;
         if (notifications.size() > 10) {
            notifications.remove(0);
         }

         int stackIndex = 0;
         Iterator var8 = notifications.iterator();

         while(var8.hasNext()) {
            Notification n = (Notification)var8.next();
            n.update();
            if (n.shouldRemove()) {
               notifications.remove(n);
            } else {
               FontObject fontTitle = FontRegistry.INTER_SEMIBOLD;
               FontObject fontDesc = FontRegistry.INTER_MEDIUM;
               String var10000 = n.getTitle();
               String fullText = var10000 + " " + n.getMessage();
               float width = Math.max(160.0F, renderer.getStringWidth(fontDesc, fullText, 8.0F) + 45.0F);
               float animVal = n.getAnimationProgress();
               float eased = easeOutBack(animVal);
               float targetX = screenWidth - width - 15.0F;
               float stackY;
               if (!n.isExiting()) {
                  stackY = screenHeight - bottomOffset - (itemHeight + padding) * (float)stackIndex;
                  ++stackIndex;
               } else {
                  stackY = screenHeight - bottomOffset - (itemHeight + padding) * (float)stackIndex - 20.0F;
               }

               float renderY;
               if (!n.isExiting()) {
                  renderY = stackY + (1.0F - eased) * 40.0F;
               } else {
                  renderY = stackY - (1.0F - animVal) * 50.0F;
               }

               renderer.pushAlpha(animVal);
               float scale = n.isExiting() ? 1.0F - (1.0F - animVal) * 0.4F : 0.8F + 0.2F * eased;
               renderer.pushScale(scale, scale, targetX + width / 2.0F, renderY + itemHeight / 2.0F);
               int accent = getColor(n.getType());
               renderer.shadow(targetX, renderY, width, itemHeight, 20.0F, 3.0F, 1.5F, (new Color(80, 40, 160, 100)).getRGB());
               renderer.gradient(targetX, renderY, width, itemHeight, 8.0F, (new Color(45, 30, 70, 240)).getRGB(), (new Color(30, 20, 50, 240)).getRGB(), (new Color(25, 15, 40, 245)).getRGB(), (new Color(40, 25, 60, 245)).getRGB());
               renderer.gradient(targetX + 1.0F, renderY + 1.0F, width - 2.0F, itemHeight - 2.0F, 7.0F, (new Color(100, 80, 180, 20)).getRGB(), (new Color(0, 0, 0, 0)).getRGB(), (new Color(0, 0, 0, 0)).getRGB(), (new Color(100, 80, 180, 20)).getRGB());
               renderer.rectOutline(targetX, renderY, width, itemHeight, 8.0F, (new Color(130, 90, 220, 120)).getRGB(), 1.5F);
               renderer.rect(targetX + 6.0F, renderY + 8.0F, 3.0F, itemHeight - 16.0F, 1.5F, accent);
               renderer.shadow(targetX + 6.0F, renderY + 8.0F, 3.0F, itemHeight - 16.0F, 5.0F, 2.0F, 1.0F, accent);
               renderer.text(fontTitle, targetX + 18.0F, renderY + 10.0F, 9.0F, n.getTitle(), (new Color(240, 230, 255)).getRGB(), "l");
               renderer.text(fontDesc, targetX + 18.0F, renderY + 22.0F, 7.5F, n.getMessage(), (new Color(170, 160, 190)).getRGB(), "l");
               renderer.popScale();
               renderer.popAlpha();
            }
         }

      }
   }

   private static int getColor(NotificationType type) {
      int var10000;
      switch(type) {
      case SUCCESS:
         var10000 = (new Color(80, 255, 160)).getRGB();
         break;
      case DISABLE:
      case ERROR:
         var10000 = (new Color(255, 60, 80)).getRGB();
         break;
      case INFO:
         var10000 = (new Color(80, 180, 255)).getRGB();
         break;
      case WARNING:
         var10000 = (new Color(255, 210, 60)).getRGB();
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   private static float easeOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      return 1.0F + c3 * (float)Math.pow((double)(x - 1.0F), 3.0D) + c1 * (float)Math.pow((double)(x - 1.0F), 2.0D);
   }
}
