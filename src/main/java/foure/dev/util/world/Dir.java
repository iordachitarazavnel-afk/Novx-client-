package foure.dev.util.world;

import net.minecraft.util.math.Direction;

public class Dir {
   public static final byte UP = 2;
   public static final byte DOWN = 4;
   public static final byte NORTH = 8;
   public static final byte SOUTH = 16;
   public static final byte WEST = 32;
   public static final byte EAST = 64;

   private Dir() {
   }

   public static byte getName(Direction dir) {
      byte var10000;
      switch(dir) {
      case UP:
         var10000 = 2;
         break;
      case DOWN:
         var10000 = 4;
         break;
      case NORTH:
         var10000 = 8;
         break;
      case SOUTH:
         var10000 = 16;
         break;
      case WEST:
         var10000 = 32;
         break;
      case EAST:
         var10000 = 64;
         break;
      default:
         throw new MatchException((String)null, (Throwable)null);
      }

      return var10000;
   }

   public static boolean is(int dir, byte b) {
      return (dir & b) == b;
   }

   public static boolean isNot(int dir, byte b) {
      return (dir & b) != b;
   }
}
