package foure.dev.ui.clickgui.elements;

import foure.dev.FourEClient;
import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.ui.clickgui.ClickGuiScreen;
import foure.dev.util.input.KeyNameUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingsPopup {
   private float x;
   private float y;
   private float width = 150.0F;
   private float height;
   private Function currentModule;
   private boolean active;
   private final List<SettingElement> elements = new ArrayList();
   private boolean dragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private long startTime = System.currentTimeMillis();

   public void open(Function module, float x, float y) {
      this.currentModule = module;
      this.x = x;
      this.y = y;
      this.active = true;
      this.elements.clear();
      this.startTime = System.currentTimeMillis();
      Iterator var4 = module.getSettings().iterator();

      while(var4.hasNext()) {
         Setting<?> setting = (Setting)var4.next();
         if (setting instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting)setting;
            this.elements.add(new BooleanElement(bs));
         } else if (setting instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting)setting;
            this.elements.add(new NumberElement(ns));
         } else if (setting instanceof MultiBoxSetting) {
            MultiBoxSetting ms = (MultiBoxSetting)setting;
            this.elements.add(new MultiBoxElement(ms));
         } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting)setting;
            this.elements.add(new ModeElement(ms));
         } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting)setting;
            this.elements.add(new ColorElement(cs));
         } else if (setting instanceof StringSetting) {
            StringSetting ss = (StringSetting)setting;
            this.elements.add(new TextElement(ss));
         }
      }

      this.recalcHeight();
   }

   private void recalcHeight() {
      float h = 26.0F;
      h += 28.0F;

      SettingElement e;
      for(Iterator var2 = this.elements.iterator(); var2.hasNext(); h += e.getHeight()) {
         e = (SettingElement)var2.next();
      }

      this.height = h + 10.0F;
   }

   public void close() {
      this.active = false;
      this.currentModule = null;
      this.dragging = false;
   }

   public void render(Renderer2D r, FontObject font, double mx, double my) {
      if (this.active && this.currentModule != null) {
         this.recalcHeight();
         float time = (float)(System.currentTimeMillis() - this.startTime) / 1000.0F;
         float pulse = (float)(Math.sin((double)(time * 2.0F)) * 0.5D + 0.5D);
         r.shadow(this.x, this.y, this.width, this.height, 20.0F, 3.0F, 1.2F, (new Color(0, 200, 255, (int)(40.0F + pulse * 15.0F))).getRGB());
         r.rect(this.x, this.y, this.width, this.height, 10.0F, (new Color(5, 10, 20, 120)).getRGB());
         r.rect(this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, this.height - 2.0F, 9.0F, (new Color(0, 40, 60, 40)).getRGB());
         r.rectOutline(this.x, this.y, this.width, this.height, 10.0F, (new Color(0, 200, 255, (int)(100.0F + pulse * 30.0F))).getRGB(), 1.5F);
         float headerHeight = 24.0F;
         r.gradient(this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, headerHeight, 9.0F, (new Color(0, 100, 140, 120)).getRGB(), (new Color(0, 100, 140, 120)).getRGB(), (new Color(0, 60, 90, 120)).getRGB(), (new Color(0, 60, 90, 120)).getRGB());
         r.gradient(this.x + 3.0F, this.y + 3.0F, this.width - 6.0F, 2.0F, 5.0F, (new Color(0, 255, 255, 100)).getRGB(), (new Color(0, 255, 255, 100)).getRGB(), (new Color(0, 255, 255, 0)).getRGB(), (new Color(0, 255, 255, 0)).getRGB());
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width / 2.0F, this.y + headerHeight / 2.0F + 2.0F, 7.5F, this.currentModule.getName(), (new Color(0, 200, 255, 120)).getRGB(), "c");
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width / 2.0F, this.y + headerHeight / 2.0F + 2.0F, 7.5F, this.currentModule.getName(), (new Color(255, 255, 255)).getRGB(), "c");
         float keybindY = this.y + headerHeight + 4.0F;
         float keybindHeight = 22.0F;
         r.text(font, this.x + 6.0F, keybindY + keybindHeight / 2.0F + 1.0F, 6.5F, "BIND:", (new Color(0, 200, 255, 200)).getRGB(), "l");
         float buttonX = this.x + 42.0F;
         float buttonWidth = this.width - 48.0F;
         boolean keybindHovered = mx >= (double)buttonX && mx <= (double)(buttonX + buttonWidth) && my >= (double)keybindY && my <= (double)(keybindY + keybindHeight);
         if (keybindHovered) {
            r.rect(buttonX, keybindY, buttonWidth, keybindHeight, 3.0F, (new Color(0, 100, 140, 120)).getRGB());
            r.rectOutline(buttonX, keybindY, buttonWidth, keybindHeight, 3.0F, (new Color(0, 200, 255, 180)).getRGB(), 1.0F);
         } else {
            r.rect(buttonX, keybindY, buttonWidth, keybindHeight, 3.0F, (new Color(0, 40, 60, 100)).getRGB());
            r.rectOutline(buttonX, keybindY, buttonWidth, keybindHeight, 3.0F, (new Color(0, 150, 200, 120)).getRGB(), 1.0F);
         }

         String keyText = KeyNameUtil.getKeyName(this.currentModule.getKey());
         r.text(font, buttonX + buttonWidth / 2.0F, keybindY + keybindHeight / 2.0F + 1.0F, 6.5F, keyText, (new Color(0, 255, 255)).getRGB(), "c");
         float cy = keybindY + keybindHeight + 6.0F;

         SettingElement e;
         for(Iterator var17 = this.elements.iterator(); var17.hasNext(); cy += e.getHeight()) {
            e = (SettingElement)var17.next();
            e.render(r, font, this.x + 4.0F, cy, this.width - 8.0F, 1.0F);
         }

      }
   }

   public void mouseClicked(double mx, double my, int btn) {
      if (this.active) {
         float headerHeight = 24.0F;
         if (mx >= (double)this.x && mx <= (double)(this.x + this.width) && my >= (double)this.y && my <= (double)(this.y + headerHeight) && btn == 0) {
            this.dragging = true;
            this.dragOffsetX = (float)(mx - (double)this.x);
            this.dragOffsetY = (float)(my - (double)this.y);
         } else {
            float keybindY = this.y + headerHeight + 4.0F;
            float keybindHeight = 22.0F;
            float buttonX = this.x + 42.0F;
            float buttonWidth = this.width - 48.0F;
            if (mx >= (double)buttonX && mx <= (double)(buttonX + buttonWidth) && my >= (double)keybindY && my <= (double)(keybindY + keybindHeight)) {
               if (btn == 0) {
                  ClickGuiScreen screen = (ClickGuiScreen)FourEClient.getInstance().getFunctionManager().getModule(ClickGuiScreen.class);
                  if (screen != null) {
                     screen.getBindPopup().open(this.currentModule, buttonX, keybindY + keybindHeight + 2.0F);
                  }
               }

            } else {
               if (mx >= (double)this.x && mx <= (double)(this.x + this.width) && my >= (double)this.y && my <= (double)(this.y + this.height)) {
                  float cy = keybindY + keybindHeight + 6.0F;

                  SettingElement e;
                  for(Iterator var12 = this.elements.iterator(); var12.hasNext(); cy += e.getHeight()) {
                     e = (SettingElement)var12.next();
                     if (e.mouseClicked(mx, my, btn, this.x + 4.0F, cy, this.width - 8.0F)) {
                        return;
                     }
                  }
               } else {
                  this.close();
               }

            }
         }
      }
   }

   public void mouseReleased(double mx, double my, int btn) {
      if (this.active) {
         if (btn == 0) {
            this.dragging = false;
         }

         Iterator var6 = this.elements.iterator();

         while(var6.hasNext()) {
            SettingElement e = (SettingElement)var6.next();
            e.mouseReleased(mx, my, btn);
         }

      }
   }

   public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
      if (this.active) {
         if (this.dragging && btn == 0) {
            this.x = (float)(mx - (double)this.dragOffsetX);
            this.y = (float)(my - (double)this.dragOffsetY);
         }

         float headerHeight = 24.0F;
         float keybindY = this.y + headerHeight + 4.0F;
         float keybindHeight = 22.0F;
         float cy = keybindY + keybindHeight + 6.0F;

         SettingElement e;
         for(Iterator var14 = this.elements.iterator(); var14.hasNext(); cy += e.getHeight()) {
            e = (SettingElement)var14.next();
            e.mouseDragged(mx, my, btn, this.x + 4.0F, cy, this.width - 8.0F);
         }

      }
   }

   public List<SettingElement> getElements() {
      return this.elements;
   }

   public boolean isActive() {
      return this.active;
   }
}
