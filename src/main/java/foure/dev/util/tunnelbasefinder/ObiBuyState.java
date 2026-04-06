package foure.dev.util.tunnelbasefinder;

public enum ObiBuyState {
   NONE,
   OPENSHOP,
   WAIT1,
   CLICKGEAR,
   WAIT2,
   CLICKOBI,
   WAIT3,
   CLICKSTACK,
   WAIT4,
   DROPITEMS,
   WAIT5,
   BUY,
   WAIT6,
   CLOSE,
   WAIT7,
   RESET;

   // $FF: synthetic method
   private static ObiBuyState[] $values() {
      return new ObiBuyState[]{NONE, OPENSHOP, WAIT1, CLICKGEAR, WAIT2, CLICKOBI, WAIT3, CLICKSTACK, WAIT4, DROPITEMS, WAIT5, BUY, WAIT6, CLOSE, WAIT7, RESET};
   }
}
