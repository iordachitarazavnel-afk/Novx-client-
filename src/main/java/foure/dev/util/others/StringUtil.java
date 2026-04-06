package foure.dev.util.others;

import foure.dev.util.Player.PlayerIntersectionUtil;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.Generated;

public final class StringUtil {
   public static String randomString(int length) {
      return (String)IntStream.range(0, length).mapToObj((operand) -> {
         return String.valueOf((char)(new Random()).nextInt(97, 123));
      }).collect(Collectors.joining());
   }

   public static String getBindName(int key) {
      return key < 0 ? "N/A" : PlayerIntersectionUtil.getKeyType(key).createFromCode(key).getTranslationKey().replace("key.keyboard.", "").replace("key.mouse.", "mouse ").replace(".", " ").toUpperCase();
   }

   public static String getDuration(int time) {
      int mins = time / 60;
      String sec = String.format("%02d", time % 60);
      return mins + ":" + sec;
   }

   @Generated
   private StringUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
