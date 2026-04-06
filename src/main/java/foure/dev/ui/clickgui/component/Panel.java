package foure.dev.ui.clickgui.component;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Panel {
   private final Category category;
   public float currentX;
   public float currentY;
   public float targetX;
   public float targetY;
   private final float width = 120.0F;
   private boolean dragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private boolean expanded = true;
   private float expandAnimation = 1.0F;
   private float targetExpandAnimation = 1.0F;
   private final List<ModuleButton> allButtons = new ArrayList();
   private final List<ModuleButton> generalButtons = new ArrayList();
   private final List<ModuleButton> visualButtons = new ArrayList();
   private boolean showVisuals = false;
   private boolean targetShowVisuals = false;
   private float switchAnimation = 0.0F;
   private float targetSwitchAnimation = 0.0F;
   private final float HEADER_HEIGHT = 24.0F;
   private final float SUB_HEADER_HEIGHT = 18.0F;
   private final float PADDING = 4.0F;

   public Panel(Category category, float startX, float startY, List<Function> modules) {
      this.category = category;
      this.targetX = startX;
      this.targetY = startY;
      this.currentX = startX;
      this.currentY = startY;
      Iterator var5 = modules.iterator();

      while(var5.hasNext()) {
         Function module = (Function)var5.next();
         ModuleButton btn = new ModuleButton(module, 4.0F, 0.0F, 112.0F);
         if (category == Category.RENDER) {
            if (module.isVisual()) {
               this.visualButtons.add(btn);
            } else {
               this.generalButtons.add(btn);
            }
         } else {
            this.allButtons.add(btn);
         }
      }

   }

   private float getCurrentHeight() {
      if (!this.expanded) {
         return 24.0F;
      } else {
         float baseHeight = 28.0F;
         if (this.category == Category.RENDER) {
            baseHeight += 18.0F;
         }

         List activeList;
         if (this.category == Category.RENDER) {
            activeList = this.showVisuals ? this.visualButtons : this.generalButtons;
         } else {
            activeList = this.allButtons;
         }

         float contentHeight = 0.0F;

         ModuleButton btn;
         for(Iterator var4 = activeList.iterator(); var4.hasNext(); contentHeight += btn.getHeight() + 4.0F) {
            btn = (ModuleButton)var4.next();
         }

         return baseHeight + contentHeight * this.expandAnimation;
      }
   }

   private void updateAnimations() {
      if (this.expanded && this.targetExpandAnimation != 1.0F) {
         this.targetExpandAnimation = 1.0F;
      } else if (!this.expanded && this.targetExpandAnimation != 0.0F) {
         this.targetExpandAnimation = 0.0F;
      }

      this.expandAnimation = this.lerp(this.expandAnimation, this.targetExpandAnimation, 0.15F);
      if (this.category == Category.RENDER) {
         if (this.targetShowVisuals && this.targetSwitchAnimation != 1.0F) {
            this.targetSwitchAnimation = 1.0F;
         } else if (!this.targetShowVisuals && this.targetSwitchAnimation != 0.0F) {
            this.targetSwitchAnimation = 0.0F;
         }

         this.switchAnimation = this.lerp(this.switchAnimation, this.targetSwitchAnimation, 0.12F);
         if (Math.abs(this.switchAnimation - this.targetSwitchAnimation) < 0.1F) {
            this.showVisuals = this.targetShowVisuals;
         }
      }

   }

   private void updateButtonPositions(List<ModuleButton> list, float startY) {
      float cy = startY;

      ModuleButton btn;
      for(Iterator var4 = list.iterator(); var4.hasNext(); cy += btn.getHeight() + 4.0F) {
         btn = (ModuleButton)var4.next();
         btn.setLocalY(cy);
      }

   }

   public void render(Renderer2D r, FontObject font, float alpha, float yAnimOffset, double mx, double my) {
      this.updateAnimations();
      if (this.dragging) {
         this.targetX = (float)mx - this.dragOffsetX;
         this.targetY = (float)my - this.dragOffsetY;
         this.currentX = this.targetX;
         this.currentY = this.targetY;
      } else {
         this.currentX = this.lerp(this.currentX, this.targetX, 0.25F);
         this.currentY = this.lerp(this.currentY, this.targetY, 0.25F);
      }

      float height = this.getCurrentHeight();
      float renderY = this.currentY + yAnimOffset;
      int bgAlpha = (int)(200.0F * alpha);
      int headAlpha = (int)(255.0F * alpha);
      int textAlpha = (int)(255.0F * alpha);
      r.rect(this.currentX, renderY, 120.0F, height, 8.0F, (new Color(10, 10, 12, bgAlpha)).getRGB());
      r.rect(this.currentX, renderY, 120.0F, 24.0F, 8.0F, 8.0F, 0.0F, 0.0F, (new Color(15, 15, 18, headAlpha)).getRGB());
      int cyan = (new Color(0, 255, 255, textAlpha)).getRGB();
      r.text(FontRegistry.INTER_MEDIUM, this.currentX + 60.0F, renderY + 12.0F + 3.0F, 10.0F, this.category.getName(), cyan, "c");
      if (this.category == Category.RENDER && this.expanded) {
         this.renderAnimatedSwitcher(r, renderY, alpha);
         this.renderAnimatedContent(r, font, renderY, alpha, mx, my);
      } else if (this.expanded) {
         this.updateButtonPositions(this.allButtons, 28.0F);
         this.renderButtonsWithAnimation(r, font, this.allButtons, renderY, alpha, mx, my);
      }

   }

   private void renderAnimatedSwitcher(Renderer2D r, float renderY, float alpha) {
      float y = renderY + 24.0F;
      float half = 60.0F;
      int activeC = (new Color(100, 160, 255, (int)(255.0F * alpha))).getRGB();
      int inactiveC = (new Color(160, 160, 160, (int)(150.0F * alpha))).getRGB();
      r.rect(this.currentX, y, 120.0F, 18.0F, 0.0F, (new Color(25, 25, 30, (int)(100.0F * alpha))).getRGB());
      float indX = this.currentX + half * this.switchAnimation;
      r.rect(indX, y + 18.0F - 2.0F, half, 2.0F, activeC);
      float mainAlpha = 1.0F - this.switchAnimation;
      float visualAlpha = this.switchAnimation;
      int mainColor = this.interpolateColor(activeC, inactiveC, this.switchAnimation);
      int visualColor = this.interpolateColor(inactiveC, activeC, this.switchAnimation);
      r.text(FontRegistry.SF_REGULAR, this.currentX + half / 2.0F, y + 12.0F, 8.0F, "Main", mainColor, "c");
      r.text(FontRegistry.SF_REGULAR, this.currentX + half + half / 2.0F, y + 12.0F, 8.0F, "Visuals", visualColor, "c");
   }

   private void renderAnimatedContent(Renderer2D r, FontObject font, float renderY, float alpha, double mx, double my) {
      float contentY = renderY + 24.0F + 18.0F + 4.0F;
      float visualAlpha;
      float visualOffset;
      if (this.switchAnimation < 1.0F) {
         this.updateButtonPositions(this.generalButtons, 46.0F);
         visualAlpha = alpha * (1.0F - this.switchAnimation);
         visualOffset = -20.0F * this.switchAnimation;
         this.renderButtonsWithAnimation(r, font, this.generalButtons, renderY + visualOffset, visualAlpha, mx, my);
      }

      if (this.switchAnimation > 0.0F) {
         this.updateButtonPositions(this.visualButtons, 46.0F);
         visualAlpha = alpha * this.switchAnimation;
         visualOffset = 20.0F * (1.0F - this.switchAnimation);
         this.renderButtonsWithAnimation(r, font, this.visualButtons, renderY + visualOffset, visualAlpha, mx, my);
      }

   }

   private void renderButtonsWithAnimation(Renderer2D r, FontObject font, List<ModuleButton> buttons, float renderY, float alpha, double mx, double my) {
      if (!(alpha <= 0.01F)) {
         for(int i = 0; i < buttons.size(); ++i) {
            ModuleButton btn = (ModuleButton)buttons.get(i);
            btn.updateHover(mx, my, this.currentX, renderY);
            float buttonDelay = (float)i * 0.05F;
            float buttonAlpha = 1.0F;
            if (this.category == Category.RENDER) {
               buttonAlpha = Math.min(1.0F, Math.max(0.0F, this.expandAnimation - buttonDelay));
               buttonAlpha = Easings.EASE_OUT_CUBIC.ease(buttonAlpha);
            }

            if (buttonAlpha > 0.01F) {
               float yOffset = this.category == Category.RENDER ? (1.0F - buttonAlpha) * 10.0F : 0.0F;
               btn.render(r, font, this.currentX, renderY - yOffset, alpha * buttonAlpha);
            }
         }

      }
   }

   public void mouseClicked(double mx, double my, int btn) {
      if (this.isHovered(mx, my)) {
         if (my >= (double)this.currentY && my <= (double)(this.currentY + 24.0F)) {
            if (btn == 0) {
               this.dragging = true;
               this.dragOffsetX = (float)mx - this.currentX;
               this.dragOffsetY = (float)my - this.currentY;
            } else if (btn == 1) {
               this.expanded = !this.expanded;
            }

         } else if (this.category == Category.RENDER && this.expanded) {
            float sy = this.currentY + 24.0F;
            if (my >= (double)sy && my <= (double)(sy + 18.0F)) {
               boolean newShowVisuals = mx >= (double)(this.currentX + 60.0F);
               if (newShowVisuals != this.targetShowVisuals) {
                  this.targetShowVisuals = newShowVisuals;
               }

            } else {
               List<ModuleButton> list = this.showVisuals ? this.visualButtons : this.generalButtons;
               Iterator var8 = list.iterator();

               ModuleButton mb;
               do {
                  if (!var8.hasNext()) {
                     return;
                  }

                  mb = (ModuleButton)var8.next();
               } while(!mb.mouseClicked(mx, my, btn, this.currentX, this.currentY));

            }
         } else {
            if (this.expanded) {
               Iterator var6 = this.allButtons.iterator();

               while(var6.hasNext()) {
                  ModuleButton mb = (ModuleButton)var6.next();
                  if (mb.mouseClicked(mx, my, btn, this.currentX, this.currentY)) {
                     return;
                  }
               }
            }

         }
      }
   }

   public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
      if (this.expanded && !this.dragging) {
         List<ModuleButton> list = this.getActiveList();
         Iterator var11 = list.iterator();

         while(var11.hasNext()) {
            ModuleButton mb = (ModuleButton)var11.next();
            mb.mouseDragged(mx, my, btn, this.currentX, this.currentY);
         }
      }

   }

   public void mouseReleased(double mx, double my, int btn) {
      this.dragging = false;
      if (this.expanded) {
         List<ModuleButton> list = this.getActiveList();
         Iterator var7 = list.iterator();

         while(var7.hasNext()) {
            ModuleButton mb = (ModuleButton)var7.next();
            mb.mouseReleased(mx, my, btn);
         }
      }

   }

   public List<ModuleButton> getButtons() {
      return this.getActiveList();
   }

   public Function getHoveredModule(double mx, double my) {
      if (!this.expanded) {
         return null;
      } else {
         Iterator var5 = this.getActiveList().iterator();

         ModuleButton mb;
         do {
            if (!var5.hasNext()) {
               return null;
            }

            mb = (ModuleButton)var5.next();
         } while(!(mx >= (double)(this.currentX + mb.getLocalX())) || !(mx <= (double)(this.currentX + mb.getLocalX() + 120.0F)) || !(my >= (double)(this.currentY + mb.getLocalY())) || !(my <= (double)(this.currentY + mb.getLocalY() + mb.getHeight())));

         return mb.getModule();
      }
   }

   public List<ModuleButton> getActiveList() {
      if (this.category == Category.RENDER) {
         return this.showVisuals ? this.visualButtons : this.generalButtons;
      } else {
         return this.allButtons;
      }
   }

   private boolean isHovered(double mx, double my) {
      return mx >= (double)this.currentX && mx <= (double)(this.currentX + 120.0F) && my >= (double)this.currentY && my <= (double)(this.currentY + this.getCurrentHeight());
   }

   private float lerp(float s, float e, float d) {
      return s + (e - s) * d;
   }

   private int interpolateColor(int color1, int color2, float factor) {
      factor = Math.max(0.0F, Math.min(1.0F, factor));
      int r1 = color1 >> 16 & 255;
      int g1 = color1 >> 8 & 255;
      int b1 = color1 & 255;
      int a1 = color1 >> 24 & 255;
      int r2 = color2 >> 16 & 255;
      int g2 = color2 >> 8 & 255;
      int b2 = color2 & 255;
      int a2 = color2 >> 24 & 255;
      int r = (int)((float)r1 + (float)(r2 - r1) * factor);
      int g = (int)((float)g1 + (float)(g2 - g1) * factor);
      int b = (int)((float)b1 + (float)(b2 - b1) * factor);
      int a = (int)((float)a1 + (float)(a2 - a1) * factor);
      return a << 24 | r << 16 | g << 8 | b;
   }
}
