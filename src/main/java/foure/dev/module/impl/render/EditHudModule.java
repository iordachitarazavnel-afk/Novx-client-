package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.ui.clickgui.component.EmptyScreen;
import foure.dev.util.render.core.Renderer2D;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;

@ModuleInfo(
   name = "EditHud",
   category = Category.RENDER,
   desc = "Move HUD elements around"
)
public class EditHudModule extends Function {
   private boolean isOpen = false;
   private HudModule draggingModule = null;
   private float dragOffsetX = 0.0F;
   private float dragOffsetY = 0.0F;

   public void onEnable() {
      super.onEnable();
      this.isOpen = true;
      if (!(mc.currentScreen instanceof EmptyScreen)) {
         mc.setScreen(new EmptyScreen());
      }

   }

   public void onDisable() {
      super.onDisable();
      this.isOpen = false;
      this.draggingModule = null;
      if (mc.currentScreen instanceof EmptyScreen) {
         mc.setScreen((Screen)null);
      }

   }

   @Subscribe
   public void onRender(RenderEvent event) {
      if (this.isOpen) {
         Renderer2D r = event.renderer();
         float screenW = event.scaledWidth();
         float screenH = event.scaledHeight();
         r.rect(0.0F, 0.0F, screenW, screenH, (new Color(0, 0, 0, 100)).getRGB());
         double mouseX = mc.mouse.getX() * (double)screenW / (double)mc.getWindow().getWidth();
         double mouseY = mc.mouse.getY() * (double)screenH / (double)mc.getWindow().getHeight();
         List<HudModule> modules = this.getHudModules();
         Iterator var10 = modules.iterator();

         while(true) {
            HudModule mod;
            do {
               if (!var10.hasNext()) {
                  return;
               }

               mod = (HudModule)var10.next();
            } while(!mod.isToggled());

            float mx = mod.getX();
            float my = mod.getY();
            float mw = mod.getWidth();
            float mh = mod.getHeight();
            boolean hovered = mouseX >= (double)mx && mouseX <= (double)(mx + mw) && mouseY >= (double)my && mouseY <= (double)(my + mh);
            int color = !hovered && this.draggingModule != mod ? (new Color(255, 255, 255, 80)).getRGB() : (new Color(0, 255, 0, 180)).getRGB();
            r.rectOutline(mx, my, mw, mh, 0.0F, color, 1.5F);
            if (this.draggingModule == mod) {
               if (Math.abs(mx + mw / 2.0F - screenW / 2.0F) < 5.0F) {
                  r.rect(screenW / 2.0F - 0.5F, 0.0F, 1.0F, screenH, (new Color(255, 0, 0, 100)).getRGB());
               }

               if (Math.abs(my + mh / 2.0F - screenH / 2.0F) < 5.0F) {
                  r.rect(0.0F, screenH / 2.0F - 0.5F, screenW, 1.0F, (new Color(255, 0, 0, 100)).getRGB());
               }
            }
         }
      }
   }

   public void handleMouseClick(double mouseX, double mouseY, int button) {
      if (this.isOpen && mc.currentScreen instanceof EmptyScreen) {
         if (button == 0) {
            List<HudModule> modules = this.getHudModules();

            for(int i = modules.size() - 1; i >= 0; --i) {
               HudModule mod = (HudModule)modules.get(i);
               if (mod.isToggled() && mouseX >= (double)mod.getX() && mouseX <= (double)(mod.getX() + mod.getWidth()) && mouseY >= (double)mod.getY() && mouseY <= (double)(mod.getY() + mod.getHeight())) {
                  this.draggingModule = mod;
                  this.dragOffsetX = (float)mouseX - mod.getX();
                  this.dragOffsetY = (float)mouseY - mod.getY();
                  return;
               }
            }
         }

      }
   }

   public void handleMouseRelease(double mouseX, double mouseY, int button) {
      if (this.isOpen && mc.currentScreen instanceof EmptyScreen) {
         if (button == 0) {
            this.draggingModule = null;
         }

      } else {
         this.draggingModule = null;
      }
   }

   public void handleMouseDrag(double mouseX, double mouseY, int button, double dx, double dy) {
      if (this.isOpen && this.draggingModule != null && mc.currentScreen instanceof EmptyScreen) {
         if (button == 0) {
            float newX = (float)mouseX - this.dragOffsetX;
            float newY = (float)mouseY - this.dragOffsetY;
            float screenW = (float)mc.getWindow().getScaledWidth();
            float screenH = (float)mc.getWindow().getScaledHeight();
            float modW = this.draggingModule.getWidth();
            float modH = this.draggingModule.getHeight();
            if (Math.abs(newX + modW / 2.0F - screenW / 2.0F) < 8.0F) {
               newX = screenW / 2.0F - modW / 2.0F;
            }

            if (Math.abs(newY + modH / 2.0F - screenH / 2.0F) < 8.0F) {
               newY = screenH / 2.0F - modH / 2.0F;
            }

            this.draggingModule.setX(newX);
            this.draggingModule.setY(newY);
         }

      }
   }

   private List<HudModule> getHudModules() {
      List<HudModule> list = new ArrayList();
      Iterator var2 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

      while(var2.hasNext()) {
         Function f = (Function)var2.next();
         if (f instanceof HudModule) {
            HudModule hm = (HudModule)f;
            list.add(hm);
         }
      }

      return list;
   }

   @Generated
   public boolean isOpen() {
      return this.isOpen;
   }
}
