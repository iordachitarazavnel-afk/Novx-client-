package foure.dev.util.tunnelbasefinder;

public enum CarrotBuyState {
   NONE,
   OPENSHOP,
   WAIT1,
   CLICKGEAR,
   WAIT2,
   CLICKCARROT,
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
   private static CarrotBuyState[] $values() {
      return new CarrotBuyState[]{NONE, OPENSHOP, WAIT1, CLICKGEAR, WAIT2, CLICKCARROT, WAIT3, CLICKSTACK, WAIT4, DROPITEMS, WAIT5, BUY, WAIT6, CLOSE, WAIT7, RESET};
   }
}
