package foure.dev.util.math;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class TimerUtil {
   private long lastMS = Instant.now().toEpochMilli();
   private long startTime;

   public void reset() {
      this.lastMS = Instant.now().toEpochMilli();
   }

   public boolean hasTimeElapsed(long time, boolean reset) {
      if (Instant.now().toEpochMilli() - this.lastMS > time) {
         if (reset) {
            this.reset();
         }

         return true;
      } else {
         return false;
      }
   }

   public long getLastMS() {
      return this.lastMS;
   }

   public void updateLastMS() {
      this.lastMS = Instant.now().toEpochMilli();
   }

   public boolean hasTimeElapsed(long time) {
      return Instant.now().toEpochMilli() - this.lastMS > time;
   }

   public long getTime() {
      return Instant.now().toEpochMilli() - this.lastMS;
   }

   public void setTime(long time) {
      this.lastMS = time;
   }

   public void sleep(int millis) {
      try {
         Thread.sleep((long)millis);
      } catch (InterruptedException var3) {
         Thread.currentThread().interrupt();
      }

   }

   public static long getCurrentTimeMillis() {
      return Instant.now().toEpochMilli();
   }

   public boolean hasSecondsElapsed(long seconds, boolean reset) {
      long timeInSeconds = ChronoUnit.SECONDS.between(Instant.ofEpochMilli(this.lastMS), Instant.now());
      if (timeInSeconds > seconds) {
         if (reset) {
            this.reset();
         }

         return true;
      } else {
         return false;
      }
   }

   public void setTimeInFuture(long futureTimeMillis) {
      this.lastMS = Math.max(futureTimeMillis, Instant.now().toEpochMilli());
   }

   public boolean isTimeBefore(long targetTimeMillis) {
      return Instant.now().toEpochMilli() < targetTimeMillis;
   }

   public long getTimeDifference(long otherTimeMillis) {
      return Instant.now().toEpochMilli() - otherTimeMillis;
   }

   public String formatTime(long timeMillis) {
      return Instant.ofEpochMilli(timeMillis).toString();
   }

   public boolean finished(double delay) {
      return (double)System.currentTimeMillis() - delay >= (double)this.startTime;
   }

   public boolean every(double delay) {
      boolean finished = this.finished(delay);
      if (finished) {
         this.reset();
      }

      return finished;
   }

   public int elapsedTime() {
      long elapsed = System.currentTimeMillis() - this.startTime;
      if (elapsed > 2147483647L) {
         return Integer.MAX_VALUE;
      } else {
         return elapsed < -2147483648L ? Integer.MIN_VALUE : (int)elapsed;
      }
   }

   public TimerUtil setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
      return this;
   }
}
