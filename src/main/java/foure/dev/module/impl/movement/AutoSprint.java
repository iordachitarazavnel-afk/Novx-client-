package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;

@ModuleInfo(
   name = "AutoSprint",
   category = Category.MOVEMENT,
   desc = "Automatically sprints"
)
public class AutoSprint extends Function {
   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         if (mc.options.forwardKey.isPressed()) {
            mc.options.sprintKey.setPressed(true);
         } else {
            mc.options.sprintKey.setPressed(false);
         }

      }
   }
}
