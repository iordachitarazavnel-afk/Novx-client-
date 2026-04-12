package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(
   name = "HUD",
   category = Category.RENDER,
   desc = "Displays client information"
)
public class HUD extends Function {

   private final BooleanSetting watermark = new BooleanSetting("Watermark", true);
   public final BooleanSetting arraylist = new BooleanSetting("ArrayList", true);
   public final BooleanSetting info = new BooleanSetting("Info", true);
   private final int mainColor = (new Color(90, 20, 140)).getRGB();
   private final int bgColor = (new Color(20, 5, 25, 240)).getRGB();
   private final int textColor = -1;
   private List<Function> cachedModules = new ArrayList();
   private long lastUpdate = 0L;

   public HUD() {
      this.addSettings(new Setting[]{this.watermark, this.arraylist, this.info});
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      Renderer2D r = event.renderer();
      if ((Boolean) this.arraylist.getValue()) {
         this.renderArrayList(r);
      }
      if ((Boolean) this.info.getValue()) {
         this.renderInfo(r);
      }
   }

   private void renderArrayList(Renderer2D r) {
      if (System.currentTimeMillis() - this.lastUpdate > 200L || this.cachedModules.isEmpty()) {
         this.cachedModules = (List) FourEClient.getInstance().getFunctionManager().getModules()
            .stream()
            .filter(Function::isState)
            .sorted(Comparator.comparingDouble((m) -> (double)(-r.getStringWidth(FontRegistry.INTER_MEDIUM, m.getName(), 10.0F))))
            .collect(Collectors.toList());
         this.lastUpdate = System.currentTimeMillis();
      }

      if (this.cachedModules.isEmpty()) {
         return;
      }

      float y = 5.0F;
      float screenW = (float) mc.getWindow().getScaledWidth();
      float itemHeight = 16.0F;
      float gap = 2.0F;
      float minX = screenW;
      float maxY = y;

      for (Function mod : this.cachedModules) {
         float w = r.getStringWidth(FontRegistry.INTER_MEDIUM, mod.getName(), 10.0F) + 10.0F;
         float posX = screenW - w - 5.0F;
         if (posX < minX) {
            minX = posX;
         }
         maxY += itemHeight + gap;
      }

      maxY -= gap;
      r.prepareBlurRegion(minX - 4.0F, y, screenW - minX + 4.0F, maxY - y, 6.0F);

      for (int i = 0; i < this.cachedModules.size(); i++) {
         Function mod = this.cachedModules.get(i);
         String name = mod.getName();
         float fontSize = 10.0F;
         float textWidth = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize);
         float width = textWidth + 10.0F;
         float x = screenW - width - 5.0F;

         // albastru → rosu cycling
         float time = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
         float t = (time + (float) i * 0.06F) % 1.0F;
         Color color1;
         if (t < 0.5F) {
            float blend = t * 2.0F;
            color1 = new Color(
               Math.min(255, (int)(30  + blend * 225)),
               Math.min(255, (int)(80  - blend * 80)),
               Math.min(255, (int)(255 - blend * 255)),
               255);
         } else {
            float blend = (t - 0.5F) * 2.0F;
            color1 = new Color(
               Math.min(255, (int)(255 - blend * 225)),
               Math.min(255, (int)(0   + blend * 80)),
               Math.min(255, (int)(0   + blend * 255)),
               255);
         }
         Color color2 = color1.darker();
         int c1 = color1.getRGB();
         int c2 = color2.getRGB();

         r.blurRegion(x - 2.0F, y, width + 4.0F, itemHeight, 4.0F, 1.0F);
         r.gradient(x, y, width, itemHeight, 4.0F,
            (new Color(10, 5, 20, 180)).getRGB(),
            (new Color(20, 10, 40, 180)).getRGB(),
            (new Color(20, 10, 40, 180)).getRGB(),
            (new Color(10, 5, 20, 180)).getRGB());
         float barW = 2.0F;
         r.gradient(x + width - barW, y, barW, itemHeight, 0.0F, c1, c1, c2, c2);
         r.shadow(x + width - barW, y, barW, itemHeight, 8.0F, 3.0F, 1.0F, c1);
         float textY = y + itemHeight / 2.0F + 3.0F;
         r.text(FontRegistry.INTER_MEDIUM, x + 4.5F, textY + 0.5F, fontSize, name, (new Color(0, 0, 0, 150)).getRGB());
         r.text(FontRegistry.INTER_MEDIUM, x + 4.0F, textY, fontSize, name, -1);
         y += itemHeight + gap;
      }
   }

   private void renderInfo(Renderer2D r) {
      float x = 5.0F;
      float y = (float)(mc.getWindow().getScaledHeight() - 15);
      String fps = "FPS: " + mc.getCurrentFps();
      r.text(FontRegistry.INTER_MEDIUM, x, y, 10.0F, fps, -1);
   }
}
