package foure.dev.event.api;

import foure.dev.FourEClient;
import lombok.Generated;

public class Event {
   private boolean canceled;
   private boolean pre;

   public void cancel() {
      this.canceled = true;
   }

   public void resume() {
      this.canceled = false;
   }

   public void call() {
      if (!FourEClient.getInstance().isPanic()) {
         FourEClient.getInstance().getEventBus().post(this);
      }

   }

   @Generated
   public boolean isCanceled() {
      return this.canceled;
   }

   @Generated
   public boolean isPre() {
      return this.pre;
   }

   @Generated
   public void setPre(boolean pre) {
      this.pre = pre;
   }
}
