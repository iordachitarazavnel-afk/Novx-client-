package foure.dev.util.tunnelbasefinder;

public enum State {
   NONE,
   MINING,
   GOABOVEHAZARD,
   YRECOVERY,
   PEARL,
   DIGGING,
   BUYXP,
   BUYPEARL,
   AUTOMEND,
   AUTOEAT,
   BUYOBI,
   BUYCARROT,
   TOTEMPOP;

   // $FF: synthetic method
   private static State[] $values() {
      return new State[]{NONE, MINING, GOABOVEHAZARD, YRECOVERY, PEARL, DIGGING, BUYXP, BUYPEARL, AUTOMEND, AUTOEAT, BUYOBI, BUYCARROT, TOTEMPOP};
   }
}
