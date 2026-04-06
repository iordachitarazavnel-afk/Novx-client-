package foure.dev.ui.clickgui;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.presss.EventPress;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.ui.clickgui.component.BindPopup;
import foure.dev.ui.clickgui.component.EmptyScreen;
import foure.dev.ui.clickgui.elements.SettingsPopup;
import foure.dev.util.render.animation.Easings;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;

@ModuleInfo(
   name = "ClickGui",
   category = Category.RENDER,
   desc = "Futuristic sci-fi ClickGUI"
)
public class ClickGuiScreen extends Function {
   private boolean isOpen = false;
   private float animProgress = 0.0F;
   private final BindPopup bindPopup = new BindPopup();
   private final SettingsPopup settingsPopup = new SettingsPopup();
   private static final Color BG_DARK = new Color(5, 10, 20, 150);
   private static final Color NEON_CYAN = new Color(0, 200, 255);
   private static final Color NEON_PURPLE = new Color(150, 50, 255);
   private static final Color GLOW_CYAN = new Color(0, 200, 255, 120);
   private final List<ClickGuiScreen.CategoryPanel> categoryPanels = new ArrayList();
   private long startTime = System.currentTimeMillis();

   public ClickGuiScreen() {
      this.setKey(344);
   }

   private void initializePanels() {
      if (this.categoryPanels.isEmpty()) {
         float startX = 50.0F;
         float startY = 50.0F;
         float panelWidth = 120.0F;
         float spacing = 12.0F;
         int index = 0;
         Category[] var6 = Category.values();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Category category = var6[var8];
            if (category != Category.THEME && category != Category.SCRIPT && category != Category.CONFIG) {
               List<Function> modules = FourEClient.getInstance().getFunctionManager().getModules(category);
               if (!modules.isEmpty()) {
                  ClickGuiScreen.CategoryPanel panel = new ClickGuiScreen.CategoryPanel(category, startX + (panelWidth + spacing) * (float)index, startY, panelWidth, modules);
                  this.categoryPanels.add(panel);
                  ++index;
               }
            }
         }

      }
   }

   public void toggle() {
      this.isOpen = !this.isOpen;
      if (this.isOpen) {
         this.onEnable();
         mc.setScreen(new EmptyScreen());
         this.animProgress = 0.0F;
         this.startTime = System.currentTimeMillis();
      } else {
         this.onDisable();
         mc.setScreen((Screen)null);
      }

   }

   @Subscribe
   public void onRender(RenderEvent event) {
      if (this.isOpen) {
         this.initializePanels();
         Renderer2D r = event.renderer();
         FontObject font = FontRegistry.SF_REGULAR;
         double scaledWidth = (double)mc.getWindow().getScaledWidth();
         double scaledHeight = (double)mc.getWindow().getScaledHeight();
         double mouseX = mc.mouse.getX() * scaledWidth / (double)mc.getWindow().getWidth();
         double mouseY = mc.mouse.getY() * scaledHeight / (double)mc.getWindow().getHeight();
         if (this.animProgress < 1.0F) {
            this.animProgress += 0.05F;
            if (this.animProgress > 1.0F) {
               this.animProgress = 1.0F;
            }
         }

         float alpha = Easings.EASE_OUT_CUBIC.ease(this.animProgress);
         r.rect(0.0F, 0.0F, (float)scaledWidth, (float)scaledHeight, 0.0F, (new Color(0, 5, 15, (int)(140.0F * alpha))).getRGB());
         Iterator var13 = this.categoryPanels.iterator();

         while(var13.hasNext()) {
            ClickGuiScreen.CategoryPanel panel = (ClickGuiScreen.CategoryPanel)var13.next();
            panel.render(r, font, alpha, mouseX, mouseY);
         }

         String title = "4e CLIENT";
         float titleY = 20.0F;
         r.text(FontRegistry.INTER_MEDIUM, (float)scaledWidth / 2.0F, titleY, 16.0F, title, (new Color(0, 200, 255, (int)(100.0F * alpha))).getRGB(), "c");
         r.text(FontRegistry.INTER_MEDIUM, (float)scaledWidth / 2.0F, titleY, 16.0F, title, (new Color(0, 200, 255, (int)(60.0F * alpha))).getRGB(), "c");
         r.text(FontRegistry.INTER_MEDIUM, (float)scaledWidth / 2.0F, titleY, 16.0F, title, (new Color(255, 255, 255, (int)(255.0F * alpha))).getRGB(), "c");
         this.settingsPopup.render(r, font, mouseX, mouseY);
      }
   }

   @Subscribe
   public void onPress(EventPress e) {
      if (e.getAction() != 0) {
         int key = e.getKey();
         if (key == 256) {
            this.toggle();
         }

      }
   }

   public void handleMouseClick(double mx, double my, int btn) {
      if (this.isOpen) {
         if (this.settingsPopup.isActive()) {
            this.settingsPopup.mouseClicked(mx, my, btn);
         } else {
            Iterator var6 = this.categoryPanels.iterator();

            while(var6.hasNext()) {
               ClickGuiScreen.CategoryPanel panel = (ClickGuiScreen.CategoryPanel)var6.next();
               panel.mouseClicked(mx, my, btn);
            }

         }
      }
   }

   public void handleMouseDrag(double mx, double my, int btn, double dx, double dy) {
      if (this.isOpen) {
         if (this.settingsPopup.isActive()) {
            this.settingsPopup.mouseDragged(mx, my, btn, dx, dy);
         } else {
            Iterator var10 = this.categoryPanels.iterator();

            while(var10.hasNext()) {
               ClickGuiScreen.CategoryPanel panel = (ClickGuiScreen.CategoryPanel)var10.next();
               panel.mouseDragged(mx, my, btn, dx, dy);
            }

         }
      }
   }

   public void handleMouseRelease(double mx, double my, int btn) {
      if (this.isOpen) {
         if (this.settingsPopup.isActive()) {
            this.settingsPopup.mouseReleased(mx, my, btn);
         } else {
            Iterator var6 = this.categoryPanels.iterator();

            while(var6.hasNext()) {
               ClickGuiScreen.CategoryPanel panel = (ClickGuiScreen.CategoryPanel)var6.next();
               panel.mouseReleased(mx, my, btn);
            }

         }
      }
   }

   public void handleMouseScroll(double amount) {
      if (this.isOpen) {
         Iterator var3 = this.categoryPanels.iterator();

         while(var3.hasNext()) {
            ClickGuiScreen.CategoryPanel panel = (ClickGuiScreen.CategoryPanel)var3.next();
            panel.mouseScrolled(amount);
         }

      }
   }

   @Generated
   public boolean isOpen() {
      return this.isOpen;
   }

   @Generated
   public BindPopup getBindPopup() {
      return this.bindPopup;
   }

   private class CategoryPanel {
      private final Category category;
      private float x;
      private float y;
      private float width;
      private final List<Function> modules;
      private boolean dragging = false;
      private float dragOffsetX;
      private float dragOffsetY;
      private float scrollOffset = 0.0F;
      private float targetScroll = 0.0F;
      private boolean expanded = true;
      private float hoverAnim = 0.0F;

      public CategoryPanel(Category category, float x, float y, float width, List<Function> modules) {
         this.category = category;
         this.x = x;
         this.y = y;
         this.width = width;
         this.modules = modules;
      }

      public void render(Renderer2D r, FontObject font, float alpha, double mouseX, double mouseY) {
         float headerHeight = 24.0F;
         float moduleHeight = 20.0F;
         float totalHeight = headerHeight + (this.expanded ? (float)this.modules.size() * moduleHeight + 10.0F : 0.0F);
         this.scrollOffset += (this.targetScroll - this.scrollOffset) * 0.2F;
         float time = (float)(System.currentTimeMillis() - ClickGuiScreen.this.startTime) / 1000.0F;
         float pulse = (float)(Math.sin((double)(time * 2.0F)) * 0.5D + 0.5D);
         r.shadow(this.x, this.y, this.width, totalHeight, 20.0F, 3.0F, 1.2F, (new Color(0, 200, 255, (int)(60.0F * alpha * (0.7F + pulse * 0.3F)))).getRGB());
         r.rect(this.x, this.y, this.width, totalHeight, 10.0F, (new Color(5, 10, 20, 120)).getRGB());
         r.rect(this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, totalHeight - 2.0F, 9.0F, (new Color(0, 40, 60, 40)).getRGB());
         int borderAlpha = (int)(100.0F * alpha + pulse * 30.0F);
         r.rectOutline(this.x, this.y, this.width, totalHeight, 10.0F, (new Color(0, 200, 255, borderAlpha)).getRGB(), 1.5F);
         r.gradient(this.x + 1.0F, this.y + 1.0F, this.width - 2.0F, headerHeight - 1.0F, 9.0F, (new Color(0, 100, 140, 120)).getRGB(), (new Color(0, 100, 140, 120)).getRGB(), (new Color(0, 60, 90, 120)).getRGB(), (new Color(0, 60, 90, 120)).getRGB());
         r.gradient(this.x + 3.0F, this.y + 3.0F, this.width - 6.0F, 2.0F, 5.0F, (new Color(0, 255, 255, (int)(100.0F * alpha))).getRGB(), (new Color(0, 255, 255, (int)(100.0F * alpha))).getRGB(), (new Color(0, 255, 255, (int)(0.0F * alpha))).getRGB(), (new Color(0, 255, 255, (int)(0.0F * alpha))).getRGB());
         String categoryName = this.category.getName().toUpperCase();
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width / 2.0F, this.y + headerHeight / 2.0F + 2.0F, 8.5F, categoryName, (new Color(0, 200, 255, (int)(120.0F * alpha))).getRGB(), "c");
         r.text(FontRegistry.INTER_MEDIUM, this.x + this.width / 2.0F, this.y + headerHeight / 2.0F + 2.0F, 8.5F, categoryName, (new Color(255, 255, 255, (int)(255.0F * alpha))).getRGB(), "c");
         if (this.expanded) {
            float moduleY = this.y + headerHeight + 5.0F;

            for(Iterator var16 = this.modules.iterator(); var16.hasNext(); moduleY += moduleHeight) {
               Function module = (Function)var16.next();
               if (moduleY + moduleHeight > this.y + totalHeight - 5.0F) {
                  break;
               }

               boolean hovered = mouseX >= (double)(this.x + 5.0F) && mouseX <= (double)(this.x + this.width - 5.0F) && mouseY >= (double)moduleY && mouseY <= (double)(moduleY + moduleHeight);
               if (module.isToggled()) {
                  r.rect(this.x + 4.0F, moduleY, this.width - 8.0F, moduleHeight, 4.0F, (new Color(0, 100, 140, (int)(100.0F * alpha))).getRGB());
                  r.rectOutline(this.x + 4.0F, moduleY, this.width - 8.0F, moduleHeight, 4.0F, (new Color(0, 200, 255, (int)(150.0F * alpha))).getRGB(), 1.0F);
               } else if (hovered) {
                  r.rect(this.x + 4.0F, moduleY, this.width - 8.0F, moduleHeight, 4.0F, (new Color(0, 200, 255, (int)(20.0F * alpha))).getRGB());
               }

               if (module.isToggled()) {
                  float dotX = this.x + 10.0F;
                  float dotY = moduleY + moduleHeight / 2.0F - 3.0F;
                  float dotSize = 6.0F;
                  r.rect(dotX - 2.0F, dotY - 2.0F, dotSize + 4.0F, dotSize + 4.0F, dotSize + 4.0F, (new Color(0, 255, 255, (int)(40.0F * alpha))).getRGB());
                  r.rect(dotX - 1.0F, dotY - 1.0F, dotSize + 2.0F, dotSize + 2.0F, dotSize + 2.0F, (new Color(0, 255, 255, (int)(80.0F * alpha))).getRGB());
                  r.rect(dotX, dotY, dotSize, dotSize, dotSize, (new Color(0, 255, 255, (int)(255.0F * alpha))).getRGB());
               }

               int textColor = module.isToggled() ? (new Color(0, 255, 255, (int)(255.0F * alpha))).getRGB() : (new Color(180, 200, 220, (int)(200.0F * alpha))).getRGB();
               r.text(font, this.x + 18.0F, moduleY + moduleHeight / 2.0F + 1.0F, 6.5F, module.getName(), textColor, "l");
            }

         }
      }

      public void mouseClicked(double mx, double my, int btn) {
         float headerHeight = 24.0F;
         if (mx >= (double)this.x && mx <= (double)(this.x + this.width) && my >= (double)this.y && my <= (double)(this.y + headerHeight)) {
            if (btn == 0) {
               this.dragging = true;
               this.dragOffsetX = (float)(mx - (double)this.x);
               this.dragOffsetY = (float)(my - (double)this.y);
            } else if (btn == 1) {
               this.expanded = !this.expanded;
            }

         } else if (this.expanded) {
            float moduleY = this.y + headerHeight + 5.0F;
            float moduleHeight = 20.0F;

            for(Iterator var9 = this.modules.iterator(); var9.hasNext(); moduleY += moduleHeight) {
               Function module = (Function)var9.next();
               if (mx >= (double)(this.x + 5.0F) && mx <= (double)(this.x + this.width - 5.0F) && my >= (double)moduleY && my <= (double)(moduleY + moduleHeight)) {
                  if (btn == 0) {
                     module.toggle();
                  } else if (btn == 1) {
                     ClickGuiScreen.this.settingsPopup.open(module, this.x + this.width + 5.0F, moduleY);
                  }

                  return;
               }
            }

         }
      }

      public void mouseDragged(double mx, double my, int btn, double dx, double dy) {
         if (this.dragging && btn == 0) {
            this.x = (float)(mx - (double)this.dragOffsetX);
            this.y = (float)(my - (double)this.dragOffsetY);
         }

      }

      public void mouseReleased(double mx, double my, int btn) {
         if (btn == 0) {
            this.dragging = false;
         }

      }

      public void mouseScrolled(double amount) {
         this.targetScroll += (float)amount * 20.0F;
         this.targetScroll = Math.max(-100.0F, Math.min(100.0F, this.targetScroll));
      }
   }
}
