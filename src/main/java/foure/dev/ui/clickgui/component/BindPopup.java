package foure.dev.ui.clickgui.component;

import foure.dev.module.api.Function;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.input.KeyNameUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;

public class BindPopup {
   private float x;
   private float y;
   private float width = 100.0F;
   private float height = 76.0F;
   private Function currentModule;
   private BooleanSetting currentSetting;
   private BindSetting currentBindSetting;
   private boolean isActive;
   private boolean listening;
   private float animProgress = 0.0F;
   private boolean dragging;
   private float dragOffsetX;
   private float dragOffsetY;
   private final float PADDING = 6.0F;
   private final float BUTTON_HEIGHT = 18.0F;
   private final float SPACING = 4.0F;
   private final float HEADER_HEIGHT = 20.0F;

   public void open(Function module, float x, float y) {
      this.currentModule = module;
      this.currentSetting = null;
      this.currentBindSetting = null;
      this.x = x;
      this.y = y;
      this.initOpen();
   }

   public void open(BooleanSetting setting, float x, float y) {
      this.currentSetting = setting;
      this.currentModule = null;
      this.currentBindSetting = null;
      this.x = x;
      this.y = y;
      this.initOpen();
   }

   public void open(BindSetting setting, float x, float y) {
      this.currentBindSetting = setting;
      this.currentModule = null;
      this.currentSetting = null;
      this.x = x;
      this.y = y;
      this.initOpen();
   }

   private void initOpen() {
      this.isActive = true;
      this.listening = false;
      this.animProgress = 0.0F;
   }

   public void close() {
      this.isActive = false;
      this.listening = false;
      this.currentModule = null;
      this.currentSetting = null;
      this.currentBindSetting = null;
   }

   public void render(Renderer2D r, FontObject font, double mouseX, double mouseY) {
      if (this.isActive) {
         String name = this.currentModule != null ? this.currentModule.getName() : (this.currentSetting != null ? this.currentSetting.getName() : (this.currentBindSetting != null ? this.currentBindSetting.getName() : ""));
         int currentKey = this.currentModule != null ? this.currentModule.getKey() : (this.currentSetting != null ? this.currentSetting.getKey() : (this.currentBindSetting != null ? (Integer)this.currentBindSetting.getValue() : -1));
         String modeName = this.currentModule != null ? this.currentModule.getBindMode().name() : "Toggle";
         if (this.animProgress < 1.0F) {
            this.animProgress += 0.12F;
            if (this.animProgress > 1.0F) {
               this.animProgress = 1.0F;
            }
         }

         float alpha = this.animProgress;
         int alphaInt = (int)(255.0F * alpha);
         int bgColor = (new Color(10, 10, 12, (int)(240.0F * alpha))).getRGB();
         int accentColor = (new Color(0, 200, 255, alphaInt)).getRGB();
         int textColor = (new Color(255, 255, 255, alphaInt)).getRGB();
         int labelColor = (new Color(160, 160, 170, alphaInt)).getRGB();
         int hoverColor = (new Color(0, 200, 255, (int)(40.0F * alpha))).getRGB();
         r.rect(this.x, this.y, this.width, this.height, 8.0F, bgColor);
         r.rectOutline(this.x, this.y, this.width, this.height, 8.0F, (new Color(20, 20, 25)).getRGB(), 1.0F);
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width / 2.0F, this.y + 6.0F + 6.0F, 8.0F, name, textColor, "c");
         float bindY = this.y + 6.0F + 20.0F;
         boolean hoverBind = this.isHovered(mouseX, mouseY, this.x + 6.0F, bindY, this.width - 12.0F, 18.0F);
         if (hoverBind || this.listening) {
            r.rect(this.x + 6.0F, bindY, this.width - 12.0F, 18.0F, 4.0F, this.listening ? (new Color(0, 200, 255, (int)(60.0F * alpha))).getRGB() : hoverColor);
         }

         if (this.listening) {
            r.rectOutline(this.x + 6.0F, bindY, this.width - 12.0F, 18.0F, 4.0F, accentColor, 1.0F);
         }

         String keyName = KeyNameUtil.getKeyName(currentKey);
         String bindText = this.listening ? "..." : (keyName != null && !keyName.isEmpty() ? keyName : "None");
         r.text(FontRegistry.SF_REGULAR, this.x + 6.0F + 4.0F, bindY + 9.0F + 2.0F, 7.0F, "Key", labelColor, "l");
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width - 6.0F - 4.0F, bindY + 9.0F + 2.0F, 7.0F, bindText, this.listening ? accentColor : textColor, "r");
         float modeY = bindY + 18.0F + 4.0F;
         if (this.currentModule != null) {
            boolean hoverMode = this.isHovered(mouseX, mouseY, this.x + 6.0F, modeY, this.width - 12.0F, 18.0F);
            if (hoverMode) {
               r.rect(this.x + 6.0F, modeY, this.width - 12.0F, 18.0F, 4.0F, hoverColor);
            }

            char var10000 = modeName.charAt(0);
            modeName = var10000 + modeName.substring(1).toLowerCase();
            r.text(FontRegistry.SF_REGULAR, this.x + 6.0F + 4.0F, modeY + 9.0F + 2.0F, 7.0F, "Mode", labelColor, "l");
            r.text(FontRegistry.INTER_MEDIUM, this.x + this.width - 6.0F - 4.0F, modeY + 9.0F + 2.0F, 7.0F, modeName, accentColor, "r");
         } else {
            r.text(FontRegistry.SF_REGULAR, this.x + this.width / 2.0F, modeY + 9.0F + 2.0F, 7.0F, "Setting Bind", labelColor, "c");
         }

      }
   }

   public void mouseClicked(double mx, double my, int btn) {
      if (this.isActive) {
         if (!this.isHovered(mx, my, this.x, this.y, this.width, this.height)) {
            this.close();
         } else {
            float bindY = this.y + 6.0F + 20.0F;
            float modeY = bindY + 18.0F + 4.0F;
            float btnWidth = this.width - 12.0F;
            if (this.isHovered(mx, my, this.x, this.y, this.width, 26.0F) && btn == 0) {
               this.dragging = true;
               this.dragOffsetX = (float)mx - this.x;
               this.dragOffsetY = (float)my - this.y;
            } else if (this.isHovered(mx, my, this.x + 6.0F, bindY, btnWidth, 18.0F) && btn == 0) {
               this.listening = !this.listening;
            } else {
               if (this.currentModule != null && this.isHovered(mx, my, this.x + 6.0F, modeY, btnWidth, 18.0F) && btn == 0) {
                  Function.BindMode current = this.currentModule.getBindMode();
                  this.currentModule.setBindMode(current == Function.BindMode.TOGGLE ? Function.BindMode.HOLD : Function.BindMode.TOGGLE);
               }

            }
         }
      }
   }

   public void keyTyped(int key) {
      if (this.isActive && this.listening) {
         int bind = key != 256 && key != 261 ? key : -1;
         if (this.currentModule != null) {
            this.currentModule.setKey(bind);
         } else if (this.currentSetting != null) {
            this.currentSetting.setKey(bind);
         } else if (this.currentBindSetting != null) {
            this.currentBindSetting.setValue(bind);
         }

         this.listening = false;
      }

   }

   public void mouseReleased(double mx, double my, int btn) {
      this.dragging = false;
   }

   public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
      if (this.dragging) {
         this.x = (float)mx - this.dragOffsetX;
         this.y = (float)my - this.dragOffsetY;
      }

   }

   private boolean isHovered(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isListening() {
      return this.listening;
   }
}
