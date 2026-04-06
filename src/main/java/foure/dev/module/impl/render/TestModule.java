package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.core.Renderer2D;

@ModuleInfo(
   name = "TestModule",
   category = Category.RENDER
)
public class TestModule extends Function {
   public void onEnable() {
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      Renderer2D renderer = event.renderer();
      int width = event.viewportWidth();
      int height = event.viewportHeight();
   }
}
