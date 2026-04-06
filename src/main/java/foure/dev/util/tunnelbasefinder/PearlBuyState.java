package foure.dev.util.tunnelbasefinder;

public enum PearlBuyState {
   NONE,
   OPENSHOP,
   WAIT1,
   CLICKGEAR,
   WAIT2,
   CLICKPEARL,
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
   private static PearlBuyState[] $values() {
      return new PearlBuyState[]{NONE, OPENSHOP, WAIT1, CLICKGEAR, WAIT2, CLICKPEARL, WAIT3, CLICKSTACK, WAIT4, DROPITEMS, WAIT5, BUY, WAIT6, CLOSE, WAIT7, RESET};
   }
}
