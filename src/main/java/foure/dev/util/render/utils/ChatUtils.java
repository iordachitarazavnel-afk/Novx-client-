package foure.dev.util.render.utils;

import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public final class ChatUtils implements Wrapper {
   public static void sendMessage(String message) {
      if (mc.player != null && mc.world != null) {
         MutableText text = Text.literal("");

         for (int i = 0; i < "Novx Client".length(); ++i) {
            text.append(
                    Text.literal(String.valueOf("novx Client".charAt(i)))
                            .setStyle(
                                    Style.EMPTY
                                            .withBold(true)
                                            .withColor(TextColor.fromRgb(
                                                    ColorUtils.gradient(
                                                            ColorUtils.getGlobalColor(),
                                                            java.awt.Color.WHITE,
                                                            (float)i / (float)"4e Client".length()
                                                    ).getRGB()
                                            ))
                            )
            );
         }

         text.append(Text.literal(" -> ").setStyle(Style.EMPTY.withBold(false).withColor(TextColor.fromRgb((new java.awt.Color(200, 200, 200)).getRGB()))));
         text.append(Text.literal(message).setStyle(Style.EMPTY.withBold(false).withColor(TextColor.fromRgb((new java.awt.Color(200, 200, 200)).getRGB()))));
         mc.player.sendMessage(text, false);
      }
   }

   @Generated
   private ChatUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
