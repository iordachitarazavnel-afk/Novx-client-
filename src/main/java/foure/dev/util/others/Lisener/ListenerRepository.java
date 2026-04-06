package foure.dev.util.others.Lisener;

import foure.dev.FourEClient;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Generated;

public class ListenerRepository {
   private final List<Listener> listeners = new ArrayList();

   public void setup() {
      this.registerListeners(new EventListener());
   }

   public void registerListeners(Listener... listeners) {
      this.listeners.addAll(List.of(listeners));
      Arrays.stream(listeners).forEach((listener) -> {
         FourEClient.getInstance().getEventBus().register(listener);
      });
   }

   @Generated
   public List<Listener> getListeners() {
      return this.listeners;
   }
}
