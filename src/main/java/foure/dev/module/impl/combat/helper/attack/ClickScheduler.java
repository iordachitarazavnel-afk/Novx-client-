package foure.dev.module.impl.combat.helper.attack;

import foure.dev.FourEClient;
import foure.dev.util.others.ServerUtil;
import foure.dev.util.wrapper.Wrapper;

public class ClickScheduler implements Wrapper {
   private final int[] funTimeTicks = new int[]{10, 11, 10, 13};
   private final int[] spookyTicks = new int[]{11, 10, 13, 10, 12, 11, 12};
   private final int[] defaultTicks = new int[]{10, 11};
   private long lastClickTime = System.currentTimeMillis();

   public boolean isCooldownComplete(boolean dynamicCooldown, int ticks) {
      boolean dynamic = this.hasTicksElapsedSinceLastClick(this.tickCount() - ticks) || !dynamicCooldown;
      return dynamic && mc.player.getAttackCooldownProgress((float)ticks) > 0.9F;
   }

   public boolean hasTicksElapsedSinceLastClick(int ticks) {
      return (float)this.lastClickPassed() >= (float)((long)ticks * 50L) * (20.0F / ServerUtil.TPS);
   }

   public long lastClickPassed() {
      return System.currentTimeMillis() - this.lastClickTime;
   }

   public void recalculate() {
      this.lastClickTime = System.currentTimeMillis();
   }

   int tickCount() {
      int count = FourEClient.getInstance().getAttackPerpetrator().getAttackHandler().getCount();
      String var2 = ServerUtil.server;
      byte var3 = -1;
      switch(var2.hashCode()) {
      case -912404296:
         if (var2.equals("SpookyTime")) {
            var3 = 1;
         }
         break;
      case 1154553036:
         if (var2.equals("FunTime")) {
            var3 = 0;
         }
      }

      int var10000;
      switch(var3) {
      case 0:
         var10000 = this.funTimeTicks[count % this.funTimeTicks.length];
         break;
      case 1:
         var10000 = this.spookyTicks[count % this.spookyTicks.length];
         break;
      default:
         var10000 = this.defaultTicks[count % this.defaultTicks.length];
      }

      return var10000;
   }
}
