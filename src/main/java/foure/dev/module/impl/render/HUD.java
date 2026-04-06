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
      if ((Boolean)this.arraylist.getValue()) {
         this.renderArrayList(r);
      }

      if ((Boolean)this.info.getValue()) {
         this.renderInfo(r);
      }

   }

   private void renderWatermark(Renderer2D r) {
      String text = "4E Client";
      float fontSize = 12.0F;
      float textW = r.getStringWidth(FontRegistry.INTER_MEDIUM, text, fontSize);
      float paddingX = 16.0F;
      float w = textW + paddingX;
      float h = 22.0F;
      float screenW = (float)mc.getWindow().getScaledWidth();
      float x = (screenW - w) / 2.0F;
      float y = 6.0F;
      r.gradient(x, y, w, h, 6.0F, (new Color(90, 20, 140, 200)).getRGB(), (new Color(60, 10, 100, 200)).getRGB(), (new Color(30, 5, 50, 200)).getRGB(), (new Color(70, 15, 120, 200)).getRGB());
      r.gradient(x + 1.5F, y + 1.5F, w - 3.0F, h - 3.0F, 5.0F, (new Color(140, 40, 200, 30)).getRGB(), (new Color(90, 20, 140, 30)).getRGB(), (new Color(60, 10, 100, 30)).getRGB(), (new Color(110, 30, 180, 30)).getRGB());
      r.rectOutline(x, y, w, h, 6.0F, (new Color(110, 30, 180, 180)).getRGB(), 1.5F);
      r.shadow(x, y, w, h, 12.0F, 2.0F, 1.0F, (new Color(90, 20, 140, 100)).getRGB());
      float textX = x + (w - textW) / 2.0F;
      float textY = y + h / 2.0F + 3.0F;
      r.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize, text, -1);
   }

   private void renderArrayList(Renderer2D r) {
      if (System.currentTimeMillis() - this.lastUpdate > 200L || this.cachedModules.isEmpty()) {
         this.cachedModules = (List)FourEClient.getInstance().getFunctionManager().getModules().stream().filter(Function::isState).sorted(Comparator.comparingDouble((m) -> {
            return (double)(-r.getStringWidth(FontRegistry.INTER_MEDIUM, m.getName(), 10.0F));
         })).collect(Collectors.toList());
         this.lastUpdate = System.currentTimeMillis();
      }

      if (!this.cachedModules.isEmpty()) {
         float y = 5.0F;
         float screenW = (float)mc.getWindow().getScaledWidth();
         float itemHeight = 16.0F;
         float gap = 2.0F;
         float minX = screenW;
         float maxY = y;

         Function mod;
         float fontSize;
         for(Iterator var9 = this.cachedModules.iterator(); var9.hasNext(); maxY += itemHeight + gap) {
            mod = (Function)var9.next();
            float w = r.getStringWidth(FontRegistry.INTER_MEDIUM, mod.getName(), 10.0F) + 10.0F;
            fontSize = screenW - w - 5.0F;
            if (fontSize < minX) {
               minX = fontSize;
            }
         }

         maxY -= gap;
         r.prepareBlurRegion(minX - 4.0F, y, screenW - minX + 4.0F, maxY - y, 6.0F);

         for(int i = 0; i < this.cachedModules.size(); ++i) {
            mod = (Function)this.cachedModules.get(i);
            String name = mod.getName();
            fontSize = 10.0F;
            float textWidth = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize);
            float width = textWidth + 10.0F;
            float x = screenW - width - 5.0F;
            float time = (float)(System.currentTimeMillis() % 3000L) / 3000.0F;
            float hue = (time + (float)i * 0.05F) % 1.0F;
            Color dynamicColor = Color.getHSBColor(0.7F + hue * 0.2F, 0.7F, 1.0F);
            int color1 = dynamicColor.getRGB();
            int color2 = dynamicColor.darker().getRGB();
            r.blurRegion(x - 2.0F, y, width + 4.0F, itemHeight, 4.0F, 1.0F);
            r.gradient(x, y, width, itemHeight, 4.0F, (new Color(20, 5, 30, 180)).getRGB(), (new Color(40, 15, 60, 180)).getRGB(), (new Color(40, 15, 60, 180)).getRGB(), (new Color(20, 5, 30, 180)).getRGB());
            float barW = 2.0F;
            r.gradient(x + width - barW, y, barW, itemHeight, 0.0F, color1, color1, color2, color2);
            r.shadow(x + width - barW, y, barW, itemHeight, 8.0F, 3.0F, 1.0F, color1);
            float textY = y + itemHeight / 2.0F + 3.0F;
            r.text(FontRegistry.INTER_MEDIUM, x + 4.5F, textY + 0.5F, fontSize, name, (new Color(0, 0, 0, 150)).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + 4.0F, textY, fontSize, name, -1);
            y += itemHeight + gap;
         }

      }
   }

   private void renderInfo(Renderer2D r) {
      float x = 5.0F;
      float y = (float)(mc.getWindow().getScaledHeight() - 15);
      String fps = "FPS: " + mc.getCurrentFps();
      r.text(FontRegistry.INTER_MEDIUM, x, y, 10.0F, fps, -1);
   }
}
