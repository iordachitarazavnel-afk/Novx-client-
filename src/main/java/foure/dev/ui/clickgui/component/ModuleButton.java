package foure.dev.ui.clickgui.component;

import foure.dev.FourEClient;
import foure.dev.module.api.Function;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.ui.clickgui.ClickGuiScreen;
import foure.dev.ui.clickgui.elements.BindElement;
import foure.dev.ui.clickgui.elements.BooleanElement;
import foure.dev.ui.clickgui.elements.ColorElement;
import foure.dev.ui.clickgui.elements.ModeElement;
import foure.dev.ui.clickgui.elements.MultiBoxElement;
import foure.dev.ui.clickgui.elements.NumberElement;
import foure.dev.ui.clickgui.elements.TextElement;
import foure.dev.util.input.KeyNameUtil;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;

public class ModuleButton {
   private final Function module;
   private final float localX;
   private float localY;
   private final float width;
   private final float baseHeight = 18.0F;
   private boolean expanded = false;
   private float expandAnimation = 0.0F;
   private float hoverAnimation = 0.0F;
   private boolean isHovered = false;
   private final List<SettingElement> settingElements = new ArrayList();

   public ModuleButton(Function module, float localX, float localY, float width) {
      this.module = module;
      this.localX = localX;
      this.localY = localY;
      this.width = width;
      Iterator var5 = module.getSettings().iterator();

      while(var5.hasNext()) {
         Setting<?> setting = (Setting)var5.next();
         if (setting instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting)setting;
            this.settingElements.add(new BooleanElement(bs));
         } else if (setting instanceof NumberSetting) {
            NumberSetting ns = (NumberSetting)setting;
            this.settingElements.add(new NumberElement(ns));
         } else if (setting instanceof MultiBoxSetting) {
            MultiBoxSetting ms = (MultiBoxSetting)setting;
            this.settingElements.add(new MultiBoxElement(ms));
         } else if (setting instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting)setting;
            this.settingElements.add(new ModeElement(ms));
         } else if (setting instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting)setting;
            this.settingElements.add(new ColorElement(cs));
         } else if (setting instanceof StringSetting) {
            StringSetting ss = (StringSetting)setting;
            this.settingElements.add(new TextElement(ss));
         } else if (setting instanceof BindSetting) {
            BindSetting bs = (BindSetting)setting;
            this.settingElements.add(new BindElement(bs));
         }
      }

   }

   private void updateAnimations() {
      float targetExpand = this.expanded ? 1.0F : 0.0F;
      this.expandAnimation = this.lerp(this.expandAnimation, targetExpand, 0.15F);
      float targetHover = this.isHovered ? 1.0F : 0.0F;
      this.hoverAnimation = this.lerp(this.hoverAnimation, targetHover, 0.2F);
   }

   public void render(Renderer2D renderer, FontObject font, float panelX, float panelY, float alpha) {
      this.updateAnimations();
      float x = panelX + this.localX;
      float y = panelY + this.localY;
      float hoverFactor = this.hoverAnimation * 0.3F;
      int baseAlpha = (int)((float)(this.module.isToggled() ? 255 : 40) * alpha);
      int textAlpha = (int)(255.0F * alpha);
      Color baseColor = this.module.isToggled() ? new Color(0, 200 + (int)(30.0F * hoverFactor), 240 + (int)(15.0F * hoverFactor), baseAlpha) : new Color(20 + (int)(10.0F * hoverFactor), 20 + (int)(10.0F * hoverFactor), 22 + (int)(10.0F * hoverFactor), baseAlpha);
      renderer.rect(x, y, this.width, 18.0F, 3.0F, baseColor.getRGB());
      int textColor = this.module.isToggled() ? (new Color(10, 10, 10, textAlpha)).getRGB() : (new Color(255, 255, 255, textAlpha)).getRGB();
      renderer.text(font, x + 4.0F, y + 9.0F + 2.5F, 8.0F, this.module.getName(), textColor, "l");
      String keyText = "[" + KeyNameUtil.getKeyName(this.module.getKey()) + "]";
      renderer.text(font, x + this.width - 4.0F, y + 9.0F + 2.0F, 7.0F, keyText, (new Color(180, 180, 180, textAlpha)).getRGB(), "r");
      if (this.expandAnimation > 0.01F && !this.settingElements.isEmpty()) {
         this.renderAnimatedSettings(renderer, font, x, y, alpha);
      }

   }

   private void renderAnimatedSettings(Renderer2D renderer, FontObject font, float x, float y, float alpha) {
      float settingsY = y + 18.0F + 1.0F;
      float totalHeight = 0.0F;

      SettingElement element;
      for(Iterator var8 = this.settingElements.iterator(); var8.hasNext(); totalHeight += element.getHeight()) {
         element = (SettingElement)var8.next();
      }

      float animatedHeight = totalHeight * this.expandAnimation;
      if (animatedHeight > 1.0F) {
         int settingsBgAlpha = (int)(50.0F * alpha * this.expandAnimation);
         renderer.rect(x, settingsY, this.width, animatedHeight, 2.0F, (new Color(20, 20, 25, settingsBgAlpha)).getRGB());
      }

      float currentY = settingsY;

      for(int i = 0; i < this.settingElements.size(); ++i) {
         element = (SettingElement)this.settingElements.get(i);
         float elementDelay = (float)i * 0.08F;
         float elementAnimation = Math.max(0.0F, Math.min(1.0F, (this.expandAnimation - elementDelay) / (1.0F - elementDelay)));
         elementAnimation = Easings.EASE_OUT_CUBIC.ease(elementAnimation);
         if (elementAnimation > 0.01F) {
            float yOffset = (1.0F - elementAnimation) * 8.0F;
            float elementAlpha = alpha * elementAnimation;
            element.render(renderer, font, x + 2.0F, currentY - yOffset, this.width - 4.0F, elementAlpha);
         }

         currentY += element.getHeight() * this.expandAnimation;
      }

   }

   public List<SettingElement> getSettingElements() {
      return this.settingElements;
   }

   public boolean isExpanded() {
      return this.expanded;
   }

   public boolean mouseClicked(double mx, double my, int button, float panelX, float panelY) {
      float x = panelX + this.localX;
      float y = panelY + this.localY;
      if (this.isHovered(mx, my, x, y, this.width, 18.0F)) {
         boolean rightSideClick = mx > (double)(x + this.width - 30.0F);
         if (rightSideClick && button == 0) {
            this.openBindPopup(x + this.width + 5.0F, y);
            return true;
         }

         if (button == 0) {
            this.module.toggle();
            return true;
         }

         if (button == 1 && !this.settingElements.isEmpty()) {
            this.expanded = !this.expanded;
            return true;
         }
      }

      if (this.expanded && this.expandAnimation > 0.5F) {
         float sy = y + 18.0F;

         SettingElement element;
         for(Iterator var11 = this.settingElements.iterator(); var11.hasNext(); sy += element.getHeight()) {
            element = (SettingElement)var11.next();
            if (element.mouseClicked(mx, my, button, x + 2.0F, sy, this.width - 4.0F)) {
               return true;
            }
         }
      }

      return false;
   }

   private void openBindPopup(float px, float py) {
      ClickGuiScreen screen = (ClickGuiScreen)FourEClient.getInstance().getFunctionManager().getModule(ClickGuiScreen.class);
      if (screen != null) {
         screen.getBindPopup().open(this.module, px, py);
      }

   }

   public void mouseDragged(double mx, double my, int button, float px, float py) {
      if (this.expanded && !(this.expandAnimation < 0.5F)) {
         float sy = py + this.localY + 18.0F;

         SettingElement e;
         for(Iterator var9 = this.settingElements.iterator(); var9.hasNext(); sy += e.getHeight()) {
            e = (SettingElement)var9.next();
            e.mouseDragged(mx, my, button, px + this.localX + 2.0F, sy, this.width - 4.0F);
         }

      }
   }

   public void mouseReleased(double mx, double my, int button) {
      if (this.expanded) {
         Iterator var6 = this.settingElements.iterator();

         while(var6.hasNext()) {
            SettingElement e = (SettingElement)var6.next();
            e.mouseReleased(mx, my, button);
         }
      }

   }

   public void updateHover(double mx, double my, float panelX, float panelY) {
      float x = panelX + this.localX;
      float y = panelY + this.localY;
      this.isHovered = this.isHovered(mx, my, x, y, this.width, 18.0F);
   }

   public float getHeight() {
      if (!this.expanded && this.expandAnimation < 0.01F) {
         return 18.0F;
      } else {
         float settingsHeight = 0.0F;

         SettingElement e;
         for(Iterator var2 = this.settingElements.iterator(); var2.hasNext(); settingsHeight += e.getHeight()) {
            e = (SettingElement)var2.next();
         }

         return 18.0F + settingsHeight * this.expandAnimation;
      }
   }

   private boolean isHovered(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * delta;
   }

   @Generated
   public Function getModule() {
      return this.module;
   }

   @Generated
   public float getLocalX() {
      return this.localX;
   }

   @Generated
   public void setLocalY(float localY) {
      this.localY = localY;
   }

   @Generated
   public float getLocalY() {
      return this.localY;
   }
}
