package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;

@ModuleInfo(
   name = "Watermark",
   category = Category.RENDER,
   desc = "Displays client watermark"
)
public class WatermarkModule extends HudModule {
   public WatermarkModule() {
      this.setX(10.0F);
      this.setY(10.0F);
      this.setWidth(80.0F);
      this.setHeight(22.0F);
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      Renderer2D r = event.renderer();
      String text = "Novx Client";
      float fontSize = 12.0F;
      float textW = r.getStringWidth(FontRegistry.INTER_MEDIUM, text, fontSize);
      float paddingX = 16.0F;
      float w = textW + paddingX;
      float h = 22.0F;
      this.setWidth(w);
      this.setHeight(h);
      float x = this.getX();
      float y = this.getY();

      // roșu
      r.gradient(x, y, w, h, 6.0F,
         (new Color(160, 10, 10, 200)).getRGB(),
         (new Color(120, 5,  5,  200)).getRGB(),
         (new Color(80,  0,  0,  200)).getRGB(),
         (new Color(140, 8,  8,  200)).getRGB());
      r.rectOutline(x, y, w, h, 6.0F,
         (new Color(220, 40, 40, 180)).getRGB(), 1.5F);
      r.shadow(x, y, w, h, 12.0F, 2.0F, 1.0F,
         (new Color(200, 20, 20, 100)).getRGB());

      float textX = x + (w - textW) / 2.0F;
      float textY = y + h / 2.0F + 3.0F;
      r.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, text, -1);
   }
}
