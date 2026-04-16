package foure.dev.ui.clickgui;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.input.EventMouseScroll;
import foure.dev.event.impl.presss.EventPress;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.api.SettingElement;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.MultiBoxSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.ui.clickgui.component.BindPopup;
import foure.dev.ui.clickgui.component.EmptyScreen;
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
import foure.dev.util.render.text.FontRegistry;
import java.awt.Color;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
   name = "ClickGui",
   category = Category.RENDER,
   desc = "Professional Dashboard Interface"
)
public class ClickGuiDashboard extends Function {
   private boolean isOpen = false;
   private float animProgress = 0.0F;
   private long lastTime = System.currentTimeMillis();
   private static final float SIDEBAR_WIDTH = 140.0F;
   private static final float TOP_BAR_HEIGHT = 40.0F;
   private float windowX = 50.0F;
   private float windowY = 50.0F;
   private boolean dragging = false;
   private float dragOffsetX = 0.0F;
   private float dragOffsetY = 0.0F;
   private float targetWindowX = 50.0F;
   private float targetWindowY = 50.0F;
   private Category selectedCategory;
   private Function selectedModule;
   private final List<SettingElement> activeSettings;
   private float settingsAnim;
   private final BindPopup bindPopup;
   private String searchString;
   private boolean searchFocused;
   private float scrollOffset;
   private boolean bindingModuleKey;

   // icon cache: iconPath -> GL texture id (0 = failed)
   private final Map<String, Integer> iconCache = new HashMap<>();

   public ClickGuiDashboard() {
      this.selectedCategory = Category.COMBAT;
      this.selectedModule = null;
      this.activeSettings = new ArrayList();
      this.settingsAnim = 0.0F;
      this.bindPopup = new BindPopup();
      this.searchString = "";
      this.searchFocused = false;
      this.scrollOffset = 0.0F;
      this.bindingModuleKey = false;
      this.setKey(344);
   }

   public void toggle() {
      this.isOpen = !this.isOpen;
      if (this.isOpen) {
         this.onEnable();
         mc.setScreen(new EmptyScreen());
         this.animProgress = 0.0F;
         this.lastTime = System.currentTimeMillis();
         double screenW = (double)mc.getWindow().getScaledWidth();
         double screenH = (double)mc.getWindow().getScaledHeight();
         float winW = (float)screenW - 100.0F;
         float winH = (float)screenH - 100.0F;
         this.windowX = (float)screenW / 2.0F - winW / 2.0F;
         this.windowY = (float)screenH / 2.0F - winH / 2.0F;
      } else {
         this.onDisable();
         mc.setScreen((Screen)null);
         this.dragging = false;
      }
   }

   @Subscribe
   public void onMouseScroll(EventMouseScroll event) {
      if (this.isOpen) this.handleMouseScroll(event.getDelta());
   }

   @Subscribe
   public void onRender(RenderEvent event) {
      if (this.isOpen) {
         long now = System.currentTimeMillis();
         float delta = (float)(now - this.lastTime) / 1000.0F;
         this.lastTime = now;
         this.windowX += (this.targetWindowX - this.windowX) * Math.min(1.0F, delta * 15.0F);
         this.windowY += (this.targetWindowY - this.windowY) * Math.min(1.0F, delta * 15.0F);
         if (this.animProgress < 1.0F) {
            this.animProgress += delta * 4.0F;
            if (this.animProgress > 1.0F) this.animProgress = 1.0F;
         }
         float alpha = Easings.EASE_OUT_CUBIC.ease(this.animProgress);
         float animScale = 0.9F + 0.1F * alpha;
         double screenW = (double)mc.getWindow().getScaledWidth();
         double screenH = (double)mc.getWindow().getScaledHeight();
         double mouseX = mc.mouse.getX() * screenW / (double)mc.getWindow().getWidth();
         double mouseY = mc.mouse.getY() * screenH / (double)mc.getWindow().getHeight();
         float finalScale = animScale * 0.75F;
         Renderer2D r = event.renderer();
         r.rect(0.0F, 0.0F, (float)screenW, (float)screenH, (new Color(5, 0, 15, (int)(200.0F * alpha))).getRGB());
         float winW = (float)screenW - 100.0F;
         float winH = (float)screenH - 100.0F;
         float pivotX = this.windowX + winW / 2.0F;
         float pivotY = this.windowY + winH / 2.0F;
         r.pushScale(finalScale, pivotX, pivotY);
         r.pushAlpha(alpha);
         this.drawDashboardBackground(r, this.windowX, this.windowY, winW, winH);
         this.drawSidebar(r, this.windowX, this.windowY, winH, mouseX, mouseY, finalScale, pivotX, pivotY);
         float contentX = this.windowX + 140.0F;
         float contentW = winW - 140.0F;
         this.drawModuleGrid(r, contentX, this.windowY, contentW, winH, mouseX, mouseY, finalScale, pivotX, pivotY);
         r.popAlpha();
         r.popScale();
         if (this.selectedModule != null) {
            float settingsW = 250.0F;
            float settingsX = this.windowX + winW + 15.0F;
            r.pushScale(finalScale, pivotX, pivotY);
            r.pushAlpha(alpha);
            this.drawSettingsPanel(r, settingsX, this.windowY, settingsW, winH, mouseX, mouseY, finalScale, pivotX, pivotY);
            r.popAlpha();
            r.popScale();
         }
      }
   }

   private void drawDashboardBackground(Renderer2D r, float x, float y, float w, float h) {
      r.rect(x, y, w, h, 15.0F, (new Color(15, 10, 25, 240)).getRGB());
      r.gradient(x, y, w, h, 15.0F, (new Color(30, 20, 50, 50)).getRGB(), (new Color(30, 20, 50, 50)).getRGB(), (new Color(5, 5, 10, 100)).getRGB(), (new Color(5, 5, 10, 100)).getRGB());
      r.rect(x, y + 40.0F, w, 1.0F, (new Color(100, 100, 200, 30)).getRGB());
      r.rectOutline(x, y, w, h, 15.0F, (new Color(80, 60, 180, 80)).getRGB(), 1.0F);
      r.rect(x + 140.0F, y + 40.0F, 1.0F, h - 40.0F, (new Color(100, 100, 200, 30)).getRGB());
      float searchW = 160.0F, searchH = 22.0F;
      float searchX = x + w - searchW - 20.0F;
      float searchY = y + (40.0F - searchH) / 2.0F;
      r.rect(searchX, searchY, searchW, searchH, 6.0F,
         this.searchFocused ? (new Color(50, 40, 80, 200)).getRGB() : (new Color(30, 20, 50, 150)).getRGB());
      r.rectOutline(searchX, searchY, searchW, searchH, 6.0F,
         this.searchFocused ? (new Color(150, 100, 255, 180)).getRGB() : (new Color(100, 80, 180, 50)).getRGB(), 1.0F);
      String disp = this.searchString.isEmpty() && !this.searchFocused ? "Search..."
         : this.searchString + (this.searchFocused && System.currentTimeMillis() % 1000L > 500L ? "_" : "");
      int textC = this.searchString.isEmpty() && !this.searchFocused ? (new Color(150, 150, 170)).getRGB() : -1;
      r.text(FontRegistry.INTER_MEDIUM, searchX + 8.0F, searchY + searchH / 2.0F + 2.0F, 8.0F, disp, textC, "l");
   }

   private void drawSidebar(Renderer2D r, float x, float y, float h, double rawMx, double rawMy, float scale, float px, float py) {
      double mx = (rawMx - (double)px) / (double)scale + (double)px;
      double my = (rawMy - (double)py) / (double)scale + (double)py;
      float curY = y + 60.0F;
      r.text(FontRegistry.INTER_SEMIBOLD, x + 70.0F, y + 25.0F, 14.0F, "4e CLIENT", (new Color(200, 180, 255)).getRGB(), "c");
      for (Category cat : Category.values()) {
         if (cat != Category.THEME && cat != Category.SCRIPT && cat != Category.CONFIG) {
            boolean selected = cat == this.selectedCategory;
            boolean hovered = mx >= (double)x && mx <= (double)(x + 140.0F) && my >= (double)curY && my <= (double)(curY + 30.0F);
            if (selected) {
               r.gradient(x, curY, 140.0F, 30.0F, (new Color(100, 50, 255, 60)).getRGB(), (new Color(20, 10, 40, 0)).getRGB(), (new Color(20, 10, 40, 0)).getRGB(), (new Color(100, 50, 255, 60)).getRGB());
               r.rect(x, curY, 3.0F, 30.0F, 0.0F, (new Color(150, 100, 255)).getRGB());
            } else if (hovered) {
               r.rect(x + 10.0F, curY, 120.0F, 30.0F, 6.0F, (new Color(255, 255, 255, 10)).getRGB());
            }
            r.text(FontRegistry.INTER_MEDIUM, x + 70.0F, curY + 15.0F + 3.0F, 9.0F, cat.getName(),
               selected ? (new Color(255, 255, 255)).getRGB() : (new Color(160, 150, 180)).getRGB(), "c");
            curY += 40.0F;
         }
      }
      float profileH = 50.0F;
      float profileY = y + h - profileH - 10.0F;
      float profileX = x + 10.0F;
      float profileW = 120.0F;
      r.rect(profileX, profileY, profileW, profileH, 10.0F, (new Color(20, 15, 30, 150)).getRGB());
      r.rectOutline(profileX, profileY, profileW, profileH, 10.0F, (new Color(100, 80, 200, 50)).getRGB(), 1.0F);
      float headSize = 32.0F;
      float headX = profileX + 10.0F;
      float headY = profileY + (profileH - headSize) / 2.0F;
      r.gradient(headX, headY, headSize, headSize, 6.0F, (new Color(100, 80, 200)).getRGB(), (new Color(60, 40, 160)).getRGB(), (new Color(60, 40, 160)).getRGB(), (new Color(100, 80, 200)).getRGB());
      r.rectOutline(headX, headY, headSize, headSize, 6.0F, (new Color(255, 255, 255, 100)).getRGB(), 1.0F);
      r.text(FontRegistry.INTER_SEMIBOLD, headX + headSize + 10.0F, headY + 6.0F, 9.0F, mc.player.getName().getString(), -1);
      r.text(FontRegistry.INTER_MEDIUM, headX + headSize + 10.0F, headY + 18.0F, 8.0F, "User", (new Color(160, 160, 180)).getRGB());
   }

   private void drawModuleGrid(Renderer2D r, float x, float y, float w, float h, double rawMx, double rawMy, float scale, float px, float py) {
      double mx = (rawMx - (double)px) / (double)scale + (double)px;
      double my = (rawMy - (double)py) / (double)scale + (double)py;
      boolean searching = !this.searchString.isEmpty();
      r.text(FontRegistry.INTER_SEMIBOLD, x + 25.0F, y + 25.0F, 18.0F,
         searching ? "Search Results" : this.selectedCategory.getName(), (new Color(230, 230, 255)).getRGB());
      List<Function> modules;
      if (searching) {
         modules = new ArrayList<>();
         for (Category c : Category.values()) modules.addAll(FourEClient.getInstance().getFunctionManager().getModules(c));
         modules = modules.stream().filter(m -> m.getClass().getName().toLowerCase().contains(this.searchString.toLowerCase())).toList();
      } else {
         modules = FourEClient.getInstance().getFunctionManager().getModules(this.selectedCategory);
      }
      float startX = x + 25.0F, startY = y + 60.0F - this.scrollOffset;
      float padding = 15.0F, modW = 200.0F, modH = 45.0F;
      float curX = startX, curY = startY;
      for (Function mod : modules) {
         if (curX + modW > x + w - 25.0F) { curX = startX; curY += modH + padding; }
         if (!(curY + modH < y + 60.0F) && !(curY > y + h - 10.0F)) {
            boolean toggled = mod.isToggled();
            boolean hovered = mx >= (double)curX && mx <= (double)(curX + modW) && my >= (double)curY && my <= (double)(curY + modH);
            if (toggled) {
               r.gradient(curX, curY, modW, modH, 10.0F, (new Color(110, 60, 240, 200)).getRGB(), (new Color(70, 30, 180, 200)).getRGB(), (new Color(50, 20, 140, 200)).getRGB(), (new Color(90, 50, 200, 200)).getRGB());
               r.gradient(curX+2,curY+2,modW-4,modH-4,8f,(new Color(140,100,255,40)).getRGB(),(new Color(100,60,220,40)).getRGB(),(new Color(80,40,180,40)).getRGB(),(new Color(120,80,240,40)).getRGB());
               r.rectOutline(curX, curY, modW, modH, 10.0F, (new Color(180, 140, 255, 220)).getRGB(), 2.0F);
               r.shadow(curX, curY, modW, modH, 20.0F, 3.0F, 1.5F, (new Color(120, 70, 255, 120)).getRGB());
            } else {
               r.rect(curX, curY, modW, modH, 10.0F, (new Color(30, 25, 45, 25)).getRGB());
               r.gradient(curX+1,curY+1,modW-2,modH-2,9f,(new Color(50,40,70,15)).getRGB(),(new Color(40,30,60,15)).getRGB(),(new Color(30,25,50,20)).getRGB(),(new Color(45,35,65,15)).getRGB());
               if (hovered) {
                  r.rectOutline(curX, curY, modW, modH, 10.0F, (new Color(140, 120, 200, 120)).getRGB(), 1.5F);
                  r.rect(curX, curY, modW, modH, 10.0F, (new Color(80, 60, 140, 20)).getRGB());
                  r.shadow(curX, curY, modW, modH, 12.0F, 1.5F, 1.0F, (new Color(100, 80, 180, 40)).getRGB());
               } else {
                  r.rectOutline(curX, curY, modW, modH, 10.0F, (new Color(80, 70, 120, 40)).getRGB(), 1.0F);
               }
            }
            // ── Icon ──────────────────────────────────────────────────
            String iconPath = mod.getIcon();
            float iconSize = 22.0F;
            float iconX = curX + 8.0F;
            float iconY = curY + (modH - iconSize) / 2.0F;
            boolean hasIcon = false;
            if (!iconPath.isEmpty()) {
               int texId = getOrLoadIcon(iconPath);
               if (texId > 0) {
                  r.drawRgbaTexture(texId, iconX, iconY, iconSize, iconSize);
                  hasIcon = true;
               }
            }
            // ─────────────────────────────────────────────────────────
            int textColor = toggled ? -1 : (new Color(180, 180, 200)).getRGB();
            if (hasIcon) {
               r.text(FontRegistry.INTER_MEDIUM, iconX + iconSize + 5.0F, curY + modH / 2.0F + 2.0F, 8.0F, mod.getName(), textColor, "l");
            } else {
               r.text(FontRegistry.INTER_MEDIUM, curX + modW / 2.0F, curY + modH / 2.0F + 2.0F, 8.0F, mod.getName(), textColor, "c");
            }
         }
         curX += modW + padding;
      }
      // scrollbar
      float scrollbarX = x + w - 15.0F, scrollbarY = y + 60.0F, scrollbarH = h - 70.0F, scrollbarW = 6.0F;
      r.rect(scrollbarX, scrollbarY, scrollbarW, scrollbarH, 3.0F, (new Color(30, 25, 45, 100)).getRGB());
      int rows = (int)Math.ceil((double)modules.size() / 3.0);
      float contentHeight = (float)rows * (modH + padding) - padding;
      float maxScroll = Math.max(0.0F, contentHeight - scrollbarH);
      if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
      if (this.scrollOffset < 0.0F) this.scrollOffset = 0.0F;
      if (contentHeight > scrollbarH) {
         float thumbH = Math.max(20.0F, scrollbarH / contentHeight * scrollbarH);
         float thumbY = scrollbarY + this.scrollOffset / maxScroll * (scrollbarH - thumbH);
         r.rect(scrollbarX, thumbY, scrollbarW, thumbH, 3.0F, (new Color(120, 100, 220, 180)).getRGB());
      }
   }

   // ── Icon loader ───────────────────────────────────────────────────────
   private int getOrLoadIcon(String iconPath) {
      if (iconCache.containsKey(iconPath)) return iconCache.get(iconPath);
      try {
         Identifier id = Identifier.of("foure", "icons/" + iconPath + ".png");
         InputStream stream = mc.getResourceManager().open(id);
         NativeImage img = NativeImage.read(stream);
         stream.close();
         // MC 1.21+ constructor: NativeImageBackedTexture(Supplier<String>, NativeImage)
         NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "foure_icon_" + iconPath, img);
         mc.getTextureManager().registerTexture(id, tex);
         // getGlId() was removed in 1.21 — use getId() which returns the GL id via TextureUtil
         int glId = tex.getGlId();
         iconCache.put(iconPath, glId);
         return glId;
      } catch (Exception e) {
         iconCache.put(iconPath, 0);
         return 0;
      }
   }
   // ─────────────────────────────────────────────────────────────────────

   private void drawSettingsPanel(Renderer2D r, float x, float y, float w, float h, double rawMx, double rawMy, float scale, float px, float py) {
      if (this.selectedModule != null) {
         if (this.settingsAnim < 1.0F) { this.settingsAnim += 0.1F; if (this.settingsAnim > 1.0F) this.settingsAnim = 1.0F; }
         float anim = Easings.EASE_OUT_CUBIC.ease(this.settingsAnim);
         double mx = (rawMx - (double)px) / (double)scale + (double)px;
         double my = (rawMy - (double)py) / (double)scale + (double)py;
         float drawX = x + (1.0F - anim) * 50.0F;
         r.pushAlpha(anim);
         r.rect(drawX, y, w, h, (new Color(20, 15, 30, 230)).getRGB());
         r.rectOutline(drawX, y, w, h, 0.0F, (new Color(100, 80, 180, 50)).getRGB(), 1.0F);
         r.text(FontRegistry.INTER_SEMIBOLD, drawX + 20.0F, y + 30.0F, 14.0F, this.selectedModule.getName(), -1);
         r.text(FontRegistry.INTER_MEDIUM, drawX + 20.0F, y + 45.0F, 8.0F, this.selectedModule.getDesc(), (new Color(160, 160, 180)).getRGB());
         float startY = y + 70.0F;
         if (this.activeSettings.isEmpty() && !this.selectedModule.getSettings().isEmpty()) this.rebuildSettings();
         float btnH = 22.0F;
         boolean bindHover = mx >= (double)(drawX + 20.0F) && mx <= (double)(drawX + w - 20.0F) && my >= (double)startY && my <= (double)(startY + btnH);
         r.rect(drawX + 20.0F, startY, w - 40.0F, btnH, 6.0F, bindHover ? (new Color(60, 50, 100)).getRGB() : (new Color(40, 35, 60)).getRGB());
         r.rectOutline(drawX + 20.0F, startY, w - 40.0F, btnH, 6.0F, (new Color(100, 80, 200, 50)).getRGB(), 1.0F);
         r.text(FontRegistry.INTER_MEDIUM, drawX + w / 2.0F, startY + btnH / 2.0F + 2.0F, 7.5F,
            this.bindingModuleKey ? "Listening..." : "Bind: " + KeyNameUtil.getKeyName(this.selectedModule.getKey()), -1, "c");
         float curY = startY + btnH + 15.0F;
         for (SettingElement e : this.activeSettings) { e.render(r, FontRegistry.INTER_MEDIUM, drawX + 15.0F, curY, w - 30.0F, 1.0F); curY += e.getHeight(); }
         r.popAlpha();
      }
   }

   private void rebuildSettings() {
      this.activeSettings.clear();
      for (Setting<?> s : this.selectedModule.getSettings()) {
         if (s instanceof BooleanSetting bs)       this.activeSettings.add(new BooleanElement(bs));
         else if (s instanceof NumberSetting ns)   this.activeSettings.add(new NumberElement(ns));
         else if (s instanceof MultiBoxSetting ms) this.activeSettings.add(new MultiBoxElement(ms));
         else if (s instanceof ModeSetting ms)     this.activeSettings.add(new ModeElement(ms));
         else if (s instanceof ColorSetting cs)    this.activeSettings.add(new ColorElement(cs));
         else if (s instanceof StringSetting ss)   this.activeSettings.add(new TextElement(ss));
         else if (s instanceof BindSetting bs)     this.activeSettings.add(new BindElement(bs));
      }
   }

   @Subscribe
   public void onPress(EventPress e) {
      if (e.getAction() != 0) {
         int key = e.getKey();
         for (SettingElement el : this.activeSettings) {
            if (el instanceof TextElement te && te.isListening()) { te.handleKeyPress(key); return; }
         }
         if (this.bindingModuleKey && this.selectedModule != null) {
            this.selectedModule.setKey(key != 256 && key != 261 ? key : 0);
            this.bindingModuleKey = false; return;
         }
         for (SettingElement el : this.activeSettings) {
            if (el instanceof BindElement be && be.getSetting() instanceof BindSetting bs && bs.isListening()) {
               bs.setValue(key != 256 && key != 261 ? key : -1); bs.setListening(false); return;
            }
         }
         if (this.searchFocused) {
            if (key == 256 || key == 257) this.searchFocused = false;
            else if (key == 259) { if (!this.searchString.isEmpty()) this.searchString = this.searchString.substring(0, this.searchString.length() - 1); }
            else { char c = this.keyToChar(key); if (c != 0) this.searchString += c; }
         } else if (key == 256) this.toggle();
      }
   }

   public void handleMouseClick(double rawMx, double rawMy, int btn) {
      if (!this.isOpen) return;
      if (this.bindPopup.isActive()) { this.bindPopup.mouseClicked(rawMx, rawMy, btn); return; }
      double screenW = (double)mc.getWindow().getScaledWidth();
      double screenH = (double)mc.getWindow().getScaledHeight();
      float alpha = Easings.EASE_OUT_CUBIC.ease(this.animProgress);
      float finalScale = (0.9F + 0.1F * alpha) * 0.75F;
      float winW = (float)screenW - 100.0F, winH = (float)screenH - 100.0F;
      float pivotX = this.windowX + winW / 2.0F, pivotY = this.windowY + winH / 2.0F;
      double mx = (rawMx - (double)pivotX) / (double)finalScale + (double)pivotX;
      double my = (rawMy - (double)pivotY) / (double)finalScale + (double)pivotY;
      float searchX = this.windowX + winW - 180.0F, searchY = this.windowY + 9.0F;
      if (mx >= (double)searchX && mx <= (double)(searchX + 160.0F) && my >= (double)searchY && my <= (double)(searchY + 22.0F) && btn == 0) {
         this.searchFocused = !this.searchFocused; return;
      }
      if (btn == 0 && this.searchFocused) this.searchFocused = false;
      if (mx >= (double)this.windowX && mx <= (double)(this.windowX + winW) && my >= (double)this.windowY && my <= (double)(this.windowY + 40.0F) && btn == 0) {
         this.dragging = true; this.dragOffsetX = (float)mx - this.windowX; this.dragOffsetY = (float)my - this.windowY; return;
      }
      float contentX = this.windowX + 140.0F;
      if (mx >= (double)this.windowX && mx <= (double)(this.windowX + 140.0F) && my >= (double)this.windowY && my <= (double)(this.windowY + winH)) {
         float curY = this.windowY + 60.0F;
         for (Category cat : Category.values()) {
            if (cat != Category.THEME && cat != Category.SCRIPT && cat != Category.CONFIG) {
               if (my >= (double)curY && my <= (double)(curY + 30.0F)) { this.selectedCategory = cat; this.selectedModule = null; this.activeSettings.clear(); return; }
               curY += 40.0F;
            }
         }
         return;
      }
      if (this.selectedModule != null) {
         float settingsW = 250.0F, settingsX = this.windowX + winW + 15.0F;
         if (mx >= (double)settingsX && mx <= (double)(settingsX + settingsW) && my >= (double)this.windowY && my <= (double)(this.windowY + winH)) {
            float bindY = this.windowY + 70.0F;
            if (mx >= (double)(settingsX + 20.0F) && mx <= (double)(settingsX + settingsW - 20.0F) && my >= (double)bindY && my <= (double)(bindY + 22.0F)) { this.bindingModuleKey = !this.bindingModuleKey; return; }
            float curY = bindY + 37.0F;
            for (SettingElement el : this.activeSettings) { if (el.mouseClicked(mx, my, btn, settingsX + 15.0F, curY, settingsW - 30.0F)) return; curY += el.getHeight(); }
            return;
         }
      }
      float settingsX = winW - 140.0F;
      if (mx >= (double)contentX && mx <= (double)(contentX + settingsX) && my >= (double)this.windowY && my <= (double)(this.windowY + winH)) {
         List<Function> modules = FourEClient.getInstance().getFunctionManager().getModules(this.selectedCategory);
         float startX = contentX + 25.0F, curY = this.windowY + 60.0F - this.scrollOffset, padding = 15.0F, modW = 200.0F, modH = 45.0F, curX = startX;
         for (Function mod : modules) {
            if (curX + modW > contentX + settingsX - 25.0F) { curX = startX; curY += modH + padding; }
            if (my < (double)(curY + modH) && my > (double)curY && mx < (double)(curX + modW) && mx > (double)curX) {
               if (btn == 0) mod.toggle();
               else if (btn == 1) { if (this.selectedModule == mod) { this.selectedModule = null; this.activeSettings.clear(); } else { this.selectedModule = mod; this.settingsAnim = 0.0F; this.rebuildSettings(); } }
               return;
            }
            curX += modW + padding;
         }
      }
   }

   public void handleMouseRelease(double rawMx, double rawMy, int btn) {
      if (!this.isOpen) return;
      if (this.bindPopup.isActive()) this.bindPopup.mouseReleased(rawMx, rawMy, btn);
      this.dragging = false;
      double screenW = (double)mc.getWindow().getScaledWidth(), screenH = (double)mc.getWindow().getScaledHeight();
      float winW = (float)screenW - 100.0F, winH = (float)screenH - 100.0F;
      float alpha = Easings.EASE_OUT_CUBIC.ease(this.animProgress);
      float finalScale = (0.9F + 0.1F * alpha) * 0.75F;
      float pivotX = this.windowX + winW / 2.0F, pivotY = this.windowY + winH / 2.0F;
      double mx = (rawMx - (double)pivotX) / (double)finalScale + (double)pivotX;
      double my = (rawMy - (double)pivotY) / (double)finalScale + (double)pivotY;
      for (SettingElement e : this.activeSettings) e.mouseReleased(mx, my, btn);
   }

   public void handleMouseDrag(double rawMx, double rawMy, int btn, double dx, double dy) {
      if (!this.isOpen) return;
      double screenW = (double)mc.getWindow().getScaledWidth(), screenH = (double)mc.getWindow().getScaledHeight();
      float winW = (float)screenW - 100.0F, winH = (float)screenH - 100.0F;
      float alpha = Easings.EASE_OUT_CUBIC.ease(this.animProgress);
      float finalScale = (0.9F + 0.1F * alpha) * 0.75F;
      float pivotX = this.windowX + winW / 2.0F, pivotY = this.windowY + winH / 2.0F;
      double mx = (rawMx - (double)pivotX) / (double)finalScale + (double)pivotX;
      double my = (rawMy - (double)pivotY) / (double)finalScale + (double)pivotY;
      if (this.dragging) { this.targetWindowX += (float)dx; this.targetWindowY += (float)dy; }
      else if (this.selectedModule != null) {
         float settingsX = this.windowX + winW + 15.0F, curY = this.windowY + 70.0F + 37.0F;
         for (SettingElement e : this.activeSettings) { e.mouseDragged(mx, my, btn, settingsX + 15.0F, curY, 220.0F); curY += e.getHeight(); }
      }
   }

   private char keyToChar(int key) {
      boolean shift = GLFW.glfwGetKey(mc.getWindow().getHandle(), 340) == 1 || GLFW.glfwGetKey(mc.getWindow().getHandle(), 344) == 1;
      if (key >= 65 && key <= 90) return (char)(key + (shift ? 0 : 32));
      if (key >= 48 && key <= 57) {
         if (shift) { if(key==49)return'!';if(key==50)return'@';if(key==51)return'#';if(key==52)return'$';if(key==53)return'%';if(key==54)return'^';if(key==55)return'&';if(key==56)return'*';if(key==57)return'(';if(key==48)return')'; }
         return (char)key;
      }
      if (key == 32) return ' '; if (key == 45) return (char)(shift ? '_' : '-');
      if (key == 46) return (char)(shift ? '>' : '.'); if (key == 44) return (char)(shift ? '<' : ',');
      return '\u0000';
   }

   public void handleMouseScroll(double amount) {
      if (this.isOpen) { this.scrollOffset -= (float)amount * 20.0F; if (this.scrollOffset < 0.0F) this.scrollOffset = 0.0F; }
   }

   @Generated public boolean isOpen() { return this.isOpen; }
   @Generated public BindPopup getBindPopup() { return this.bindPopup; }
}
