package foure.dev.util.tunnelbasefinder;

public enum XpBuyState {
   NONE,
   OPENSHOP,
   WAIT1,
   CLICKGEAR,
   WAIT2,
   CLICKXP,
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
   private static XpBuyState[] $values() {
      return new XpBuyState[]{NONE, OPENSHOP, WAIT1, CLICKGEAR, WAIT2, CLICKXP, WAIT3, CLICKSTACK, WAIT4, DROPITEMS, WAIT5, BUY, WAIT6, CLOSE, WAIT7, RESET};
   }
}
