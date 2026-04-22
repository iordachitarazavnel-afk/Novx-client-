package foure.dev.ui.clickgui;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.input.EventMouseScroll;
import foure.dev.event.impl.presss.EventPress;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.config.FriendManager;
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
import lombok.Generated;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "ClickGui", category = Category.RENDER, desc = "Novx Client Interface")
public class NovxClickGuiFull extends Function {

    // ── Layout ────────────────────────────────────────────────────────────
    private static final float WIN_W    = 640f;
    private static final float WIN_H    = 400f;
    private static final float SIDE_W   = 150f;
    private static final float HEADER_H = 36f;

    // ── Popup ─────────────────────────────────────────────────────────────
    private static final float POP_W   = 230f;
    private static final float POP_H   = 320f;
    private static final float POP_HDR = 28f;

    // ── Themes ────────────────────────────────────────────────────────────
    public record ThemeEntry(String name, String desc, Color bg, Color accent, Color accentDim) {}
    public static final ThemeEntry[] THEMES = {
        new ThemeEntry("Astral",  "Dark neutral",   new Color(20,20,28),  new Color(180,180,200), new Color(100,100,120)),
        new ThemeEntry("Crimson", "Deep red",        new Color(28,8,8),   new Color(220,50,50),   new Color(140,20,20)),
        new ThemeEntry("Ocean",   "Teal / cyan",     new Color(5,18,32),  new Color(0,180,220),   new Color(0,100,150)),
        new ThemeEntry("Forest",  "Muted green",     new Color(8,22,8),   new Color(60,180,60),   new Color(30,110,30)),
        new ThemeEntry("Sakura",  "Soft pink",       new Color(30,12,22), new Color(240,120,160), new Color(180,60,100)),
        new ThemeEntry("Gold",    "Warm golden",     new Color(28,18,0),  new Color(220,170,30),  new Color(140,90,0)),
        new ThemeEntry("Void",    "Deep purple",     new Color(12,5,25),  new Color(140,60,255),  new Color(80,20,180)),
        new ThemeEntry("Arctic",  "Icy blue",        new Color(8,15,28),  new Color(100,180,255), new Color(50,120,200)),
        new ThemeEntry("Ember",   "Burnt orange",    new Color(28,12,5),  new Color(255,120,40),  new Color(180,70,10)),
        new ThemeEntry("Rose",    "Rose magenta",    new Color(28,8,18),  new Color(255,80,160),  new Color(180,30,100)),
        new ThemeEntry("Mint",    "Fresh mint",      new Color(5,22,15),  new Color(60,220,140),  new Color(20,150,80)),
        new ThemeEntry("Slate",   "Steel blue",      new Color(12,15,22), new Color(120,150,200), new Color(70,90,140)),
    };
    private static int activeTheme = 2;

    public static ThemeEntry theme() { return THEMES[activeTheme]; }
    public static Color accent()     { return theme().accent; }
    public static Color bg()         { return theme().bg; }

    // ── State ─────────────────────────────────────────────────────────────
    private boolean isOpen       = false;
    private float   animProgress = 0f;
    private long    lastTime     = System.currentTimeMillis();
    private float   winX, winY;
    private boolean dragging     = false;
    private float   dragOffX, dragOffY;

    private Category selCategory = null;
    // Tabs: 0=Settings 1=Themes 2=GUISettings 3=Configs 4=Friends
    private int selTab = 0;

    private float   modScroll = 0;
    private String  search    = "";
    private boolean searchF   = false;

    // Popup
    private Function  popupModule   = null;
    private float     popupX, popupY;
    private boolean   popupDragging = false;
    private float     popupDragX, popupDragY;
    private boolean   bindingKey    = false;
    private final List<SettingElement> popupElements = new ArrayList<>();
    private float     popupScroll   = 0;
    private float     popupAnim     = 0f;
    private boolean   popupClosing  = false;

    private float themeScroll = 0;

    // Friends
    private String  friendInput  = "";
    private boolean friendFocus  = false;
    private String  friendSearch = "";
    private boolean friendSrchF  = false;
    private float   friendScroll = 0;

    // Configs
    private String  cfgName      = "";
    private boolean cfgFocus     = false;
    private String  cfgServerIp  = "";
    private boolean cfgIpFocus   = false;
    private float   cfgScroll    = 0;
    private int     cfgTab       = 0; // 0=My 1=Shared
    private List<String> savedConfigs = new ArrayList<>();

    // GUI Settings
    private boolean cfgRainbow    = false;
    private boolean cfgBreathing  = false;
    private boolean cfgCustomFont = true;
    private boolean cfgMSAA       = true;

    private final BindPopup bindPopup = new BindPopup();

    public NovxClickGuiFull() { setKey(344); }

    // ── Toggle ────────────────────────────────────────────────────────────
    public void toggle() {
        isOpen = !isOpen;
        if (isOpen) {
            onEnable();
            mc.setScreen(new EmptyScreen());
            animProgress = 0;
            lastTime = System.currentTimeMillis();
            float sw = mc.getWindow().getScaledWidth();
            float sh = mc.getWindow().getScaledHeight();
            winX = sw / 2f - WIN_W / 2f;
            winY = sh / 2f - WIN_H / 2f;
            if (selCategory == null) {
                for (Category c : Category.values())
                    if (c != Category.THEME && c != Category.SCRIPT && c != Category.CONFIG)
                    { selCategory = c; break; }
            }
            refreshConfigList();
        } else {
            onDisable();
            mc.setScreen((Screen) null);
            dragging = false; popupDragging = false;
            closePopup();
        }
    }

    // ── Config list helpers ───────────────────────────────────────────────
    private void refreshConfigList() {
        savedConfigs.clear();
        File dir = new File(MinecraftClient.getInstance().runDirectory, "configs");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".json"));
            if (files != null) {
                for (File f : files)
                    savedConfigs.add(f.getName().replace(".json", ""));
            }
        }
    }

    // ── Popup helpers ─────────────────────────────────────────────────────
    private void openPopup(Function mod, double mx, double my) {
        popupModule   = mod;
        popupElements.clear();
        rebuildPopup();
        popupScroll   = 0;
        popupAnim     = 0f;
        popupClosing  = false;
        bindingKey    = false;
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        float tryX = winX + WIN_W + 8f;
        if (tryX + POP_W > sw - 4f) tryX = winX - POP_W - 8f;
        if (tryX < 4f) tryX = winX + WIN_W - POP_W - 8f;
        tryX = Math.max(4f, Math.min(tryX, sw - POP_W - 4f));
        float tryY = Math.max(4f, Math.min((float) my - 30f, sh - POP_H - 4f));
        popupX = tryX; popupY = tryY;
    }

    private void closePopup() {
        popupModule = null; popupElements.clear();
        popupAnim = 0f; popupClosing = false; bindingKey = false;
    }

    private float popupTotalH() {
        float h = 0;
        for (SettingElement el : popupElements) h += el.getHeight();
        return h;
    }

    // ── Main render ───────────────────────────────────────────────────────
    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isOpen) return;
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f; lastTime = now;
        animProgress = Math.min(1f, animProgress + dt * 5f);
        float alpha = Easings.EASE_OUT_CUBIC.ease(animProgress);

        Renderer2D r = event.renderer();
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();
        r.rect(0, 0, sw, sh, new Color(0,0,0,(int)(130*alpha)).getRGB());

        r.pushScale(0.93f + 0.07f * alpha, winX + WIN_W/2f, winY + WIN_H/2f);
        r.pushAlpha(alpha);
        drawWindow(r);

        if (popupModule != null) {
            popupAnim = popupClosing
                ? Math.max(0f, popupAnim - dt * 8f)
                : Math.min(1f, popupAnim + dt * 8f);
            if (popupClosing && popupAnim <= 0f) closePopup();
            else if (popupModule != null) drawPopup(r, popupAnim);
        }

        r.popAlpha();
        r.popScale();
    }

    // ── Window ────────────────────────────────────────────────────────────
    private void drawWindow(Renderer2D r) {
        Color bg = bg(), acc = accent();
        int bgC   = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 248).getRGB();
        int sideC = new Color(Math.max(0,bg.getRed()-6),Math.max(0,bg.getGreen()-6),Math.max(0,bg.getBlue()-10),255).getRGB();

        r.rect(winX, winY, WIN_W, WIN_H, 10f, bgC);
        r.rectOutline(winX, winY, WIN_W, WIN_H, 10f, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),50).getRGB(), 1f);
        r.rect(winX, winY, SIDE_W, WIN_H, new Color(bg.getRed(),bg.getGreen(),bg.getBlue(),255).getRGB());
        r.rect(winX+SIDE_W-1, winY, 1, WIN_H, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),25).getRGB());
        r.rect(winX, winY, SIDE_W, HEADER_H, sideC);
        r.text(FontRegistry.INTER_SEMIBOLD, winX+SIDE_W/2f, winY+HEADER_H/2f+2f, 10f, "Novx v1", acc.getRGB(), "c");

        // Categories
        float cy = winY + HEADER_H + 6f;
        for (Category cat : Category.values()) {
            if (cat==Category.THEME||cat==Category.SCRIPT||cat==Category.CONFIG) continue;
            boolean sel = cat==selCategory && selTab==0;
            if (sel) {
                r.rect(winX+2, cy-1, SIDE_W-4, 25f, 5f, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),28).getRGB());
                r.rect(winX+2, cy+5f, 3f, 13f, 1.5f, acc.getRGB());
            }
            int tc = sel ? acc.getRGB() : new Color(150,150,165).getRGB();
            r.rect(winX+13, cy+9f, 4f, 4f, 2f, sel ? acc.getRGB() : new Color(70,70,80).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, winX+22f, cy+12f, 8f, cat.getName(), tc, "l");
            cy += 27f;
        }

        // Bottom sidebar nav
        float bY = winY + WIN_H - 160f;
        String[] sideNav   = {"G GUI Settings","T Themes","F Configs","P Friends"};
        int[]    sideTabIdx = {2, 1, 3, 4};
        for (int i = 0; i < sideNav.length; i++) {
            boolean sel = selTab == sideTabIdx[i];
            int tc = sel ? acc.getRGB() : new Color(110,110,125).getRGB();
            if (sel) r.rect(winX+2, bY-1, SIDE_W-4, 22f, 4f, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),22).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, winX+14f, bY+10f, 8f, sideNav[i], tc, "l");
            bY += 26f;
        }

        // Header
        float hdrX = winX+SIDE_W, hdrW = WIN_W-SIDE_W;
        r.rect(hdrX, winY, hdrW, HEADER_H, new Color(Math.max(0,bg.getRed()-4),Math.max(0,bg.getGreen()-4),Math.max(0,bg.getBlue()-6),255).getRGB());
        r.rect(hdrX, winY+HEADER_H-1, hdrW, 1, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),20).getRGB());
        String hdrTitle = selTab==0?(selCategory!=null?selCategory.getName():"Modules"):selTab==1?"Themes":selTab==2?"GUI Settings":selTab==3?"Config Manager":"Friend Manager";
        r.text(FontRegistry.INTER_SEMIBOLD, hdrX+hdrW/2f, winY+HEADER_H/2f+2f, 9f, hdrTitle, acc.getRGB(), "c");
        r.text(FontRegistry.INTER_MEDIUM, winX+WIN_W-16f, winY+HEADER_H/2f+2f, 10f, "×", new Color(180,60,60,200).getRGB(), "c");

        // Search bar
        if (selTab==0) {
            float sbX=hdrX+8f, sbY=winY+HEADER_H+6f, sbW=hdrW-16f, sbH=18f;
            r.rect(sbX,sbY,sbW,sbH,5f,new Color(255,255,255,10).getRGB());
            r.rectOutline(sbX,sbY,sbW,sbH,5f,searchF?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB():new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),25).getRGB(),1f);
            String disp=search.isEmpty()&&!searchF?"Search modules...":search+(searchF&&System.currentTimeMillis()%900>450?"|":"");
            r.text(FontRegistry.INTER_MEDIUM,sbX+8f,sbY+sbH/2f+2f,7.5f,disp,search.isEmpty()?new Color(70,70,85).getRGB():-1,"l");
        }

        float cX=winX+SIDE_W, cY=winY+HEADER_H, cW=WIN_W-SIDE_W, cH=WIN_H-HEADER_H;
        switch (selTab) {
            case 0 -> drawModules(r, cX, cY+30f, cW, cH-30f);
            case 1 -> drawThemes(r, cX, cY, cW, cH);
            case 2 -> drawGuiSettings(r, cX, cY, cW, cH);
            case 3 -> drawConfigs(r, cX, cY, cW, cH);
            case 4 -> drawFriends(r, cX, cY, cW, cH);
        }
    }

    // ── Modules ───────────────────────────────────────────────────────────
    private void drawModules(Renderer2D r, float x, float y, float w, float h) {
        if (selCategory==null) return;
        Color acc = accent();
        List<Function> mods;
        if (!search.isEmpty()) {
            mods = new ArrayList<>();
            for (Category c : Category.values())
                FourEClient.getInstance().getFunctionManager().getModules(c).stream()
                    .filter(m->m.getName().toLowerCase().contains(search.toLowerCase())).forEach(mods::add);
        } else mods = FourEClient.getInstance().getFunctionManager().getModules(selCategory);

        float mH=26f, gap=3f, curY=y+2f-modScroll, totalH=mods.size()*(mH+gap);
        r.pushClipRect((int)x,(int)y,(int)w,(int)h);
        for (Function mod : mods) {
            if (curY+mH<y||curY>y+h){curY+=mH+gap;continue;}
            boolean tog=mod.isToggled(), sel=mod==popupModule;
            float mx2=x+6f, mw=w-12f;
            if (tog){r.rect(mx2,curY,mw,mH,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),30).getRGB());r.rectOutline(mx2,curY,mw,mH,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB(),1f);}
            else{r.rect(mx2,curY,mw,mH,5f,new Color(255,255,255,sel?12:6).getRGB());if(sel)r.rectOutline(mx2,curY,mw,mH,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),40).getRGB(),1f);}
            r.rect(mx2+8f,curY+mH/2f-3f,6f,6f,3f,tog?acc.getRGB():new Color(55,55,65).getRGB());
            if(tog)r.shadow(mx2+8f,curY+mH/2f-3f,6f,6f,8f,2f,1f,acc.getRGB());
            r.text(FontRegistry.INTER_MEDIUM,mx2+20f,curY+mH/2f+2f,8f,mod.getName(),tog?acc.getRGB():new Color(185,185,200).getRGB(),"l");
            r.text(FontRegistry.INTER_MEDIUM,mx2+mw-12f,curY+mH/2f+2f,8f,"›",new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB(),"c");
            curY+=mH+gap;
        }
        r.popClipRect();
        if (totalH>h){float maxS=totalH-h;modScroll=Math.max(0,Math.min(modScroll,maxS));float tH=Math.max(20f,h/totalH*h);float tY=y+(modScroll/maxS)*(h-tH);r.rect(x+w-5f,y,3f,h,1.5f,new Color(255,255,255,12).getRGB());r.rect(x+w-5f,tY,3f,tH,1.5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),90).getRGB());}
    }

    // ── Themes ────────────────────────────────────────────────────────────
    private void drawThemes(Renderer2D r, float x, float y, float w, float h) {
        Color acc=accent();
        r.text(FontRegistry.INTER_MEDIUM,x+12f,y+20f,8f,"Active: "+theme().name,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),200).getRGB(),"l");
        float lY=y+34f,lH=h-36f,iH=54f,gap=6f,curY=lY-themeScroll;
        r.pushClipRect((int)x,(int)lY,(int)(w-8),(int)lH);
        for (int i=0;i<THEMES.length;i++){
            ThemeEntry t=THEMES[i];if(curY+iH<lY){curY+=iH+gap;continue;}if(curY>lY+lH)break;
            boolean active=i==activeTheme;float ix=x+8f,iw=w-16f;
            r.rect(ix,curY,iw,iH,7f,active?new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),22).getRGB():new Color(255,255,255,7).getRGB());
            r.rectOutline(ix,curY,iw,iH,7f,active?new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),110).getRGB():new Color(255,255,255,18).getRGB(),1f);
            r.rect(ix,curY,iw,3f,7f,new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),active?200:80).getRGB());
            r.rect(ix+10f,curY+16f,14f,14f,3f,t.bg.getRGB());r.rectOutline(ix+10f,curY+16f,14f,14f,3f,new Color(255,255,255,30).getRGB(),1f);
            r.rect(ix+28f,curY+16f,14f,14f,3f,t.accent.getRGB());r.rect(ix+46f,curY+16f,14f,14f,3f,new Color(220,220,230).getRGB());
            r.text(FontRegistry.INTER_SEMIBOLD,ix+68f,curY+18f,8.5f,t.name,new Color(225,225,235).getRGB(),"l");
            r.text(FontRegistry.INTER_MEDIUM,ix+68f,curY+30f,7.5f,t.desc,new Color(120,120,135).getRGB(),"l");
            if(active){r.text(FontRegistry.INTER_MEDIUM,ix+iw-55f,curY+iH/2f+2f,7.5f,"✓ Active",new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),220).getRGB(),"l");}
            else{float bX=ix+iw-52f,bY2=curY+iH/2f-9f;r.rect(bX,bY2,46f,18f,5f,new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),38).getRGB());r.rectOutline(bX,bY2,46f,18f,5f,new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),90).getRGB(),1f);r.text(FontRegistry.INTER_MEDIUM,bX+23f,bY2+11f,7.5f,"Apply",new Color(t.accent.getRed(),t.accent.getGreen(),t.accent.getBlue(),220).getRGB(),"c");}
            curY+=iH+gap;
        }
        r.popClipRect();
        float totalH=THEMES.length*(iH+gap);
        if(totalH>lH){float maxS=totalH-lH;themeScroll=Math.max(0,Math.min(themeScroll,maxS));float tH2=Math.max(20f,lH/totalH*lH);float tY=lY+(themeScroll/maxS)*(lH-tH2);r.rect(x+w-5f,lY,3f,lH,1.5f,new Color(255,255,255,12).getRGB());r.rect(x+w-5f,tY,3f,tH2,1.5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),90).getRGB());}
    }

    // ── GUI Settings ──────────────────────────────────────────────────────
    private void drawGuiSettings(Renderer2D r, float x, float y, float w, float h) {
        Color acc=accent(); float cY=y+14f;
        r.text(FontRegistry.INTER_MEDIUM,x+14f,cY+8f,7.5f,"Visual",new Color(100,100,115).getRGB(),"l"); cY+=22f;
        cY=drawToggleRow(r,x,cY,w,"Rainbow Effect","Cycle accent hues over time",cfgRainbow,acc);
        cY=drawToggleRow(r,x,cY,w,"Breathing","Pulse accent color alpha",cfgBreathing,acc);
        r.rect(x+8f,cY+4f,w-16f,1f,new Color(255,255,255,10).getRGB()); cY+=16f;
        cY=drawToggleRow(r,x,cY,w,"Custom Font","Use client font renderer",cfgCustomFont,acc);
        cY=drawToggleRow(r,x,cY,w,"MSAA","Anti-aliasing for renders",cfgMSAA,acc);
        r.rect(x+8f,cY+4f,w-16f,1f,new Color(255,255,255,10).getRGB()); cY+=18f;
        r.text(FontRegistry.INTER_MEDIUM,x+14f,cY+8f,7.5f,"Discord",new Color(100,100,115).getRGB(),"l"); cY+=22f;
        drawToggleRow(r,x,cY,w,"Discord Rich Presence","Show game activity on Discord",false,acc);
    }

    private float drawToggleRow(Renderer2D r, float x, float y, float w, String name, String desc, boolean val, Color acc) {
        float rH=40f;
        r.rect(x+8f,y,w-16f,rH,5f,new Color(255,255,255,val?8:5).getRGB());
        if(val)r.rectOutline(x+8f,y,w-16f,rH,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),50).getRGB(),1f);
        r.text(FontRegistry.INTER_MEDIUM,x+18f,y+13f,8.5f,name,val?acc.getRGB():new Color(200,200,215).getRGB(),"l");
        r.text(FontRegistry.INTER_MEDIUM,x+18f,y+26f,7f,desc,new Color(100,100,115).getRGB(),"l");
        float tX=x+w-50f,tY=y+rH/2f-7f;
        r.rect(tX,tY,30f,14f,7f,val?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),180).getRGB():new Color(50,50,60,200).getRGB());
        r.rect(val?tX+18f:tX+2f,tY+2f,10f,10f,5f,new Color(255,255,255,val?240:140).getRGB());
        return y+rH+4f;
    }

    // ── Configs (full: save, load, delete, list from disk) ────────────────
    private void drawConfigs(Renderer2D r, float x, float y, float w, float h) {
        Color acc=accent();
        // Tab bar
        float tabY=y+10f, tabW=(w-20f)/2f;
        for (int i=0;i<2;i++){
            boolean sel=cfgTab==i; float tX=x+10f+i*tabW;
            r.rect(tX,tabY,tabW,22f,5f,sel?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),40).getRGB():new Color(255,255,255,8).getRGB());
            if(sel)r.rect(tX+4f,tabY+20f,tabW-8f,2f,1f,acc.getRGB());
            r.text(FontRegistry.INTER_MEDIUM,tX+tabW/2f,tabY+13f,8f,i==0?"My Configs":"Shared",sel?acc.getRGB():new Color(130,130,145).getRGB(),"c");
        }

        if (cfgTab==0){
            // Config name input
            float inY=tabY+30f;
            r.rect(x+10f,inY,w-20f,20f,5f,new Color(255,255,255,10).getRGB());
            r.rectOutline(x+10f,inY,w-20f,20f,5f,cfgFocus?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB():new Color(255,255,255,20).getRGB(),1f);
            String cDisp=cfgName.isEmpty()&&!cfgFocus?"Config name...":cfgName+(cfgFocus&&System.currentTimeMillis()%900>450?"|":"");
            r.text(FontRegistry.INTER_MEDIUM,x+18f,inY+12f,7.5f,cDisp,cfgName.isEmpty()?new Color(70,70,85).getRGB():-1,"l");

            // Server IP input
            float ipY=inY+26f;
            r.rect(x+10f,ipY,w-20f,20f,5f,new Color(255,255,255,10).getRGB());
            r.rectOutline(x+10f,ipY,w-20f,20f,5f,cfgIpFocus?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB():new Color(255,255,255,20).getRGB(),1f);
            String ipDisp=cfgServerIp.isEmpty()&&!cfgIpFocus?"Server IP (optional)...":cfgServerIp+(cfgIpFocus&&System.currentTimeMillis()%900>450?"|":"");
            r.text(FontRegistry.INTER_MEDIUM,x+18f,ipY+12f,7.5f,ipDisp,cfgServerIp.isEmpty()?new Color(70,70,85).getRGB():-1,"l");

            // Save button
            float sY=ipY+26f;
            r.rect(x+10f,sY,w-20f,22f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),40).getRGB());
            r.rectOutline(x+10f,sY,w-20f,22f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),80).getRGB(),1f);
            r.text(FontRegistry.INTER_MEDIUM,x+w/2f,sY+13f,8f,"Save Config",acc.getRGB(),"c");

            // Config list
            float lY=sY+30f, lH=h-(lY-y)-10f;
            r.pushClipRect((int)(x+8f),(int)lY,(int)(w-16f),(int)lH);
            if (savedConfigs.isEmpty()){
                r.text(FontRegistry.INTER_MEDIUM,x+w/2f,lY+lH/2f,8f,"No configs yet",new Color(70,70,85).getRGB(),"c");
            } else {
                float curY=lY-cfgScroll;
                for (String cfg : savedConfigs){
                    if (curY+24f<lY||curY>lY+lH){curY+=28f;continue;}
                    r.rect(x+10f,curY,w-20f,24f,5f,new Color(255,255,255,8).getRGB());
                    r.rectOutline(x+10f,curY,w-20f,24f,5f,new Color(255,255,255,15).getRGB(),1f);
                    r.text(FontRegistry.INTER_MEDIUM,x+18f,curY+14f,8f,cfg,new Color(200,200,215).getRGB(),"l");
                    // Load button
                    float loadX=x+w-70f;
                    r.rect(loadX,curY+4f,28f,16f,4f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),50).getRGB());
                    r.text(FontRegistry.INTER_MEDIUM,loadX+14f,curY+14f,7f,"Load",acc.getRGB(),"c");
                    // Delete button
                    float delX=x+w-36f;
                    r.rect(delX,curY+4f,22f,16f,4f,new Color(140,30,30,100).getRGB());
                    r.text(FontRegistry.INTER_MEDIUM,delX+11f,curY+14f,7f,"×",new Color(220,80,80).getRGB(),"c");
                    curY+=28f;
                }
            }
            r.popClipRect();
            // Scrollbar
            float totalCH=savedConfigs.size()*28f;
            float lY2=sY+30f, lH2=h-(lY2-y)-10f;
            if(totalCH>lH2){float maxS=totalCH-lH2;cfgScroll=Math.max(0,Math.min(cfgScroll,maxS));float tH=Math.max(16f,lH2/totalCH*lH2);float tY=lY2+(cfgScroll/maxS)*(lH2-tH);r.rect(x+w-7f,lY2,3f,lH2,1.5f,new Color(255,255,255,12).getRGB());r.rect(x+w-7f,tY,3f,tH,1.5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),90).getRGB());}
        } else {
            float sY=tabY+30f;
            r.rect(x+10f,sY,w-20f,22f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),30).getRGB());
            r.rectOutline(x+10f,sY,w-20f,22f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),60).getRGB(),1f);
            r.text(FontRegistry.INTER_MEDIUM,x+w/2f,sY+13f,8f,"Import from Clipboard",acc.getRGB(),"c");
            r.text(FontRegistry.INTER_MEDIUM,x+w/2f,y+h/2f+20f,8f,"No shared configs yet",new Color(70,70,85).getRGB(),"c");
        }
    }

    // ── Friends ───────────────────────────────────────────────────────────
    private void drawFriends(Renderer2D r, float x, float y, float w, float h) {
        Color acc=accent(); List<String> friends=FriendManager.getInstance().getFriends();
        r.text(FontRegistry.INTER_SEMIBOLD,x+12f,y+20f,9f,"Friend Manager",new Color(220,220,235).getRGB(),"l");
        r.text(FontRegistry.INTER_MEDIUM,x+w-16f,y+20f,7.5f,friends.size()+" friend"+(friends.size()!=1?"s":""),new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),180).getRGB(),"r");
        float inY=y+32f,btnW=40f,addX=x+w-10f-btnW;
        r.rect(x+10f,inY,w-20f-btnW-4f,20f,5f,new Color(255,255,255,10).getRGB());
        r.rectOutline(x+10f,inY,w-20f-btnW-4f,20f,5f,friendFocus?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB():new Color(255,255,255,20).getRGB(),1f);
        String iDisp=friendInput.isEmpty()&&!friendFocus?"Player name...":friendInput+(friendFocus&&System.currentTimeMillis()%900>450?"|":"");
        r.text(FontRegistry.INTER_MEDIUM,x+18f,inY+12f,7.5f,iDisp,friendInput.isEmpty()?new Color(70,70,85).getRGB():-1,"l");
        r.rect(addX,inY,btnW,20f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),50).getRGB());
        r.rectOutline(addX,inY,btnW,20f,5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB(),1f);
        r.text(FontRegistry.INTER_MEDIUM,addX+btnW/2f,inY+12f,7.5f,"Add",acc.getRGB(),"c");
        float srY=inY+26f;
        r.rect(x+10f,srY,w-20f,18f,5f,new Color(255,255,255,8).getRGB());
        r.rectOutline(x+10f,srY,w-20f,18f,5f,friendSrchF?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),80).getRGB():new Color(255,255,255,15).getRGB(),1f);
        String srDisp=friendSearch.isEmpty()&&!friendSrchF?"Search friends...":friendSearch+(friendSrchF&&System.currentTimeMillis()%900>450?"|":"");
        r.text(FontRegistry.INTER_MEDIUM,x+16f,srY+11f,7f,srDisp,new Color(80,80,95).getRGB(),"l");
        float lY=srY+24f,lH=h-(lY-y)-40f;
        List<String> filtered=friends.stream().filter(f->friendSearch.isEmpty()||f.toLowerCase().contains(friendSearch.toLowerCase())).collect(Collectors.toList());
        r.pushClipRect((int)(x+8f),(int)lY,(int)(w-16f),(int)lH);
        if(filtered.isEmpty()){r.text(FontRegistry.INTER_MEDIUM,x+w/2f,lY+lH/2f,8f,friends.isEmpty()?"No friends added yet":"No results",new Color(70,70,85).getRGB(),"c");}
        else{float curY=lY-friendScroll;for(String fr:filtered){if(curY+22f<lY||curY>lY+lH){curY+=26f;continue;}r.rect(x+10f,curY,w-20f,22f,5f,new Color(0,180,200,12).getRGB());r.rectOutline(x+10f,curY,w-20f,22f,5f,new Color(0,180,200,30).getRGB(),1f);r.rect(x+14f,curY+8f,6f,6f,3f,new Color(0,200,220).getRGB());r.text(FontRegistry.INTER_MEDIUM,x+26f,curY+13f,8f,fr,new Color(0,220,240).getRGB(),"l");r.text(FontRegistry.INTER_MEDIUM,x+w-18f,curY+13f,8f,"×",new Color(180,50,50,200).getRGB(),"c");curY+=26f;}}
        r.popClipRect();
        float clrY=y+h-28f;
        r.rect(x+w-70f,clrY,62f,20f,5f,new Color(140,30,30,100).getRGB());r.rectOutline(x+w-70f,clrY,62f,20f,5f,new Color(180,50,50,120).getRGB(),1f);
        r.text(FontRegistry.INTER_MEDIUM,x+w-39f,clrY+12f,7.5f,"Clear All",new Color(220,80,80).getRGB(),"c");
    }

    // ── Settings popup (animat) ───────────────────────────────────────────
    private void drawPopup(Renderer2D r, float anim) {
        if (popupModule==null) return;
        float eased=Easings.EASE_OUT_CUBIC.ease(anim);
        Color acc=accent(), bg=bg();
        int bgC=new Color(Math.max(0,bg.getRed()+5),Math.max(0,bg.getGreen()+5),Math.max(0,bg.getBlue()+8),252).getRGB();
        r.pushScale(0.88f+0.12f*eased, popupX+POP_W/2f, popupY+POP_H/2f);
        r.pushAlpha(eased);
        r.shadow(popupX,popupY,POP_W,POP_H,18f,3f,1.5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),60).getRGB());
        r.rect(popupX,popupY,POP_W,POP_H,8f,bgC);
        r.rectOutline(popupX,popupY,POP_W,POP_H,8f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),70).getRGB(),1f);
        r.rect(popupX,popupY,POP_W,POP_HDR,8f,new Color(Math.max(0,bg.getRed()-4),Math.max(0,bg.getGreen()-4),Math.max(0,bg.getBlue()-6),255).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD,popupX+12f,popupY+POP_HDR/2f+2f,8.5f,popupModule.getName(),acc.getRGB(),"l");
        r.text(FontRegistry.INTER_MEDIUM,popupX+POP_W-14f,popupY+POP_HDR/2f+2f,9f,"×",new Color(160,50,50,200).getRGB(),"c");
        r.rect(popupX,popupY+POP_HDR-1f,POP_W,1f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),20).getRGB());
        r.text(FontRegistry.INTER_MEDIUM,popupX+12f,popupY+POP_HDR+10f,7f,popupModule.getDesc(),new Color(90,90,105).getRGB(),"l");
        float bY=popupY+POP_HDR+22f;
        r.rect(popupX+10f,bY,POP_W-20f,20f,5f,new Color(255,255,255,8).getRGB());
        r.rectOutline(popupX+10f,bY,POP_W-20f,20f,5f,bindingKey?new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),100).getRGB():new Color(255,255,255,18).getRGB(),1f);
        r.text(FontRegistry.INTER_MEDIUM,popupX+POP_W/2f,bY+12f,7.5f,bindingKey?"Listening... (ESC=remove)":"Bind: "+KeyNameUtil.getKeyName(popupModule.getKey()),-1,"c");
        float sY=bY+26f, sH=POP_H-(sY-popupY)-6f;
        if(popupElements.isEmpty()&&!popupModule.getSettings().isEmpty())rebuildPopup();
        r.pushClipRect((int)(popupX+2f),(int)sY,(int)(POP_W-4f),(int)sH);
        float curY=sY-popupScroll;
        for(SettingElement el:popupElements){el.render(r,FontRegistry.INTER_MEDIUM,popupX+8f,curY,POP_W-16f,1f);curY+=el.getHeight();}
        r.popClipRect();
        float totalSettH=popupTotalH();
        if(totalSettH>sH){float maxS=totalSettH-sH;popupScroll=Math.max(0,Math.min(popupScroll,maxS));float tH=Math.max(16f,sH/totalSettH*sH);float tY=sY+(popupScroll/maxS)*(sH-tH);r.rect(popupX+POP_W-5f,sY,3f,sH,1.5f,new Color(255,255,255,12).getRGB());r.rect(popupX+POP_W-5f,tY,3f,tH,1.5f,new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),90).getRGB());}
        r.popAlpha(); r.popScale();
    }

    private void rebuildPopup() {
        popupElements.clear(); if(popupModule==null)return;
        for(Setting<?> s:popupModule.getSettings()){
            if(s instanceof BooleanSetting bs)popupElements.add(new BooleanElement(bs));
            else if(s instanceof NumberSetting ns)popupElements.add(new NumberElement(ns));
            else if(s instanceof MultiBoxSetting ms)popupElements.add(new MultiBoxElement(ms));
            else if(s instanceof ModeSetting ms)popupElements.add(new ModeElement(ms));
            else if(s instanceof ColorSetting cs)popupElements.add(new ColorElement(cs));
            else if(s instanceof StringSetting ss)popupElements.add(new TextElement(ss));
            else if(s instanceof BindSetting bs)popupElements.add(new BindElement(bs));
        }
    }

    // ── Mouse ─────────────────────────────────────────────────────────────
    public void handleMouseClick(double rawMx, double rawMy, int btn) {
        if (!isOpen) return;
        double mx=rawMx, my=rawMy;
        if (popupModule!=null){
            if(mx>=popupX+POP_W-20f&&mx<=popupX+POP_W&&my>=popupY&&my<=popupY+POP_HDR&&btn==0){closePopup();return;}
            if(mx>=popupX&&mx<=popupX+POP_W&&my>=popupY&&my<=popupY+POP_HDR&&btn==0){popupDragging=true;popupDragX=(float)(mx-popupX);popupDragY=(float)(my-popupY);return;}
            float bY=popupY+POP_HDR+22f;
            if(mx>=popupX+10f&&mx<=popupX+POP_W-10f&&my>=bY&&my<=bY+20f&&btn==0){bindingKey=!bindingKey;return;}
            float sY=bY+26f,curY=sY-popupScroll;
            for(SettingElement el:popupElements){if(el.mouseClicked(mx,my,btn,popupX+8f,curY,POP_W-16f))return;curY+=el.getHeight();}
        }
        if(mx>=winX+WIN_W-22f&&mx<=winX+WIN_W&&my>=winY&&my<=winY+HEADER_H&&btn==0){toggle();return;}
        float cX=winX+SIDE_W;
        if(mx>=cX&&mx<=winX+WIN_W&&my>=winY&&my<=winY+HEADER_H&&btn==0&&popupModule==null){dragging=true;dragOffX=(float)(mx-winX);dragOffY=(float)(my-winY);return;}
        if(mx>=winX&&mx<=winX+SIDE_W){
            float bNavY=winY+WIN_H-160f; int[]tabIdxMap={2,1,3,4};
            for(int i=0;i<4;i++){if(my>=bNavY&&my<=bNavY+22f){selTab=tabIdxMap[i];closePopup();return;}bNavY+=26f;}
            float cy2=winY+HEADER_H+6f;
            for(Category cat:Category.values()){if(cat==Category.THEME||cat==Category.SCRIPT||cat==Category.CONFIG)continue;if(my>=cy2&&my<=cy2+25f){selCategory=cat;selTab=0;modScroll=0;return;}cy2+=27f;}
            return;
        }
        float contentX=winX+SIDE_W, cW=WIN_W-SIDE_W;
        if(selTab==0){
            float sbY=winY+HEADER_H+6f;
            if(mx>=contentX+8f&&mx<=contentX+cW-8f&&my>=sbY&&my<=sbY+18f){searchF=!searchF;return;}
            if(searchF)searchF=false;
            if(selCategory!=null){
                List<Function>mods=FourEClient.getInstance().getFunctionManager().getModules(selCategory);
                float mH=26f,gap=3f,curY=winY+HEADER_H+36f-modScroll;
                for(Function mod:mods){float mx2=contentX+6f,mw2=cW-12f;if(my>=curY&&my<=curY+mH&&mx>=mx2&&mx<=mx2+mw2){if(btn==0)mod.toggle();else if(btn==1){if(popupModule==mod)closePopup();else openPopup(mod,mx,my);}return;}curY+=mH+gap;}
            }
        } else if(selTab==1){
            float lY=winY+HEADER_H+34f,iH=54f,gap=6f,curY=lY-themeScroll;
            for(int i=0;i<THEMES.length;i++){ThemeEntry t=THEMES[i];float ix=contentX+8f,iw=cW-16f;if(i!=activeTheme){float bX=ix+iw-52f,bY2=curY+iH/2f-9f;if(mx>=bX&&mx<=bX+46f&&my>=bY2&&my<=bY2+18f){activeTheme=i;return;}}curY+=iH+gap;}
        } else if(selTab==2){
            float tY=winY+HEADER_H+36f;
            tY=handleToggleClick(mx,my,contentX,tY,cW,btn);tY=handleToggleClick(mx,my,contentX,tY,cW,btn);tY+=10f;
            if(isInToggle(mx,my,contentX,tY,cW)){cfgCustomFont=!cfgCustomFont;return;}tY+=44f;
            if(isInToggle(mx,my,contentX,tY,cW)){cfgMSAA=!cfgMSAA;return;}
        } else if(selTab==3){
            handleConfigClick(mx,my,contentX,cW,btn);
        } else if(selTab==4){
            handleFriendsClick(mx,my,contentX,cW,btn);
        }
    }

    private float handleToggleClick(double mx, double my, float x, float y, float w, int btn){return y+44f;}
    private boolean isInToggle(double mx, double my, float x, float y, float w){return mx>=x+w-52f&&mx<=x+w-12f&&my>=y+13f&&my<=y+27f;}

    private void handleConfigClick(double mx, double my, float x, float w, int btn) {
        float tabY=winY+HEADER_H+10f, tabW=(w-20f)/2f;
        if(my>=tabY&&my<=tabY+22f){
            if(mx>=x+10f&&mx<=x+10f+tabW){cfgTab=0;return;}
            if(mx>=x+10f+tabW&&mx<=x+10f+tabW*2){cfgTab=1;return;}
        }
        if(cfgTab==0){
            float inY=tabY+30f;
            if(mx>=x+10f&&mx<=x+w-10f&&my>=inY&&my<=inY+20f){cfgFocus=true;cfgIpFocus=false;return;}
            float ipY=inY+26f;
            if(mx>=x+10f&&mx<=x+w-10f&&my>=ipY&&my<=ipY+20f){cfgIpFocus=true;cfgFocus=false;return;}
            float sY=ipY+26f;
            if(mx>=x+10f&&mx<=x+w-10f&&my>=sY&&my<=sY+22f&&!cfgName.isEmpty()){
                FourEClient.getInstance().getConfigManager().save(cfgName);
                cfgName="";cfgFocus=false;refreshConfigList();return;
            }
            // Config list clicks
            float lY=sY+30f, curY=lY-cfgScroll;
            for(String cfg:new ArrayList<>(savedConfigs)){
                if(my>=curY&&my<=curY+24f){
                    float loadX=x+w-70f, delX=x+w-36f;
                    if(mx>=loadX&&mx<=loadX+28f){FourEClient.getInstance().getConfigManager().load(cfg);return;}
                    if(mx>=delX&&mx<=delX+22f){
                        File f=new File(MinecraftClient.getInstance().runDirectory,"configs/"+cfg+".json");
                        f.delete(); refreshConfigList(); return;
                    }
                }
                curY+=28f;
            }
        }
    }

    private void handleFriendsClick(double mx, double my, float x, float w, int btn) {
        float inY=winY+HEADER_H+32f,btnW=40f,addX=x+w-10f-btnW;
        if(mx>=x+10f&&mx<=addX-4f&&my>=inY&&my<=inY+20f){friendFocus=!friendFocus;return;}
        if(mx>=addX&&mx<=addX+btnW&&my>=inY&&my<=inY+20f&&!friendInput.isEmpty()){FriendManager.getInstance().addFriend(friendInput.trim());friendInput="";friendFocus=false;return;}
        float srY=inY+26f;
        if(mx>=x+10f&&mx<=x+w-10f&&my>=srY&&my<=srY+18f){friendSrchF=!friendSrchF;return;}
        float lY=srY+24f,curY=lY-friendScroll;
        List<String>friends=FriendManager.getInstance().getFriends();
        for(String fr:friends){if(my>=curY&&my<=curY+22f&&mx>=x+w-26f&&mx<=x+w-10f){FriendManager.getInstance().removeFriend(fr);return;}curY+=26f;}
        float clrY=winY+WIN_H-28f;
        if(mx>=x+w-70f&&mx<=x+w-8f&&my>=clrY&&my<=clrY+20f){FriendManager.getInstance().getFriends().clear();return;}
    }

    public void handleMouseRelease(double mx, double my, int btn) {
        if(!isOpen)return; dragging=false;popupDragging=false;
        for(SettingElement el:popupElements)el.mouseReleased(mx,my,btn);
    }

    public void handleMouseDrag(double mx, double my, int btn, double dx, double dy) {
        if(!isOpen)return;
        if(popupDragging&&btn==0){popupX=(float)(mx-popupDragX);popupY=(float)(my-popupDragY);return;}
        if(dragging&&btn==0){winX=(float)(mx-dragOffX);winY=(float)(my-dragOffY);return;}
        if(popupModule!=null){float bY=popupY+POP_HDR+48f,curY=bY-popupScroll;for(SettingElement el:popupElements){el.mouseDragged(mx,my,btn,popupX+8f,curY,POP_W-16f);curY+=el.getHeight();}}
    }

    public void handleMouseScroll(double amount) {
        if(!isOpen)return;
        if(popupModule!=null){float sY=popupY+POP_HDR+48f,sH=POP_H-(sY-popupY)-6f;if(popupTotalH()>sH){popupScroll-=(float)amount*16f;return;}}
        if(selTab==0)modScroll-=(float)amount*18f;
        else if(selTab==1)themeScroll-=(float)amount*18f;
        else if(selTab==3)cfgScroll-=(float)amount*18f;
        else if(selTab==4)friendScroll-=(float)amount*18f;
    }

    @Subscribe public void onMouseScroll(EventMouseScroll event){if(isOpen)handleMouseScroll(event.getDelta());}

    @Subscribe
    public void onPress(EventPress e) {
        if(e.getAction()==0)return;
        int key=e.getKey();
        for(SettingElement el:popupElements){if(el instanceof TextElement te&&te.isListening()){te.handleKeyPress(key);return;}}
        if(bindingKey&&popupModule!=null){
            popupModule.setKey(key==256||key==261?-1:key); bindingKey=false; return;
        }
        for(SettingElement el:popupElements){if(el instanceof BindElement be&&be.getSetting()instanceof BindSetting bs&&bs.isListening()){bs.setValue(key!=256&&key!=261?key:-1);bs.setListening(false);return;}}
        if(searchF){handleTextInput(key,"search");return;}
        if(cfgFocus){handleTextInput(key,"config");return;}
        if(cfgIpFocus){handleTextInput(key,"cfgip");return;}
        if(friendFocus){handleTextInput(key,"friend");return;}
        if(friendSrchF){handleTextInput(key,"friendsearch");return;}
        if(key==256)toggle();
    }

    private void handleTextInput(int key, String target) {
        if(key==256||key==257){searchF=cfgFocus=cfgIpFocus=friendFocus=friendSrchF=false;return;}
        if(key==259){
            switch(target){
                case "search"->{if(!search.isEmpty())search=search.substring(0,search.length()-1);}
                case "config"->{if(!cfgName.isEmpty())cfgName=cfgName.substring(0,cfgName.length()-1);}
                case "cfgip"->{if(!cfgServerIp.isEmpty())cfgServerIp=cfgServerIp.substring(0,cfgServerIp.length()-1);}
                case "friend"->{if(!friendInput.isEmpty())friendInput=friendInput.substring(0,friendInput.length()-1);}
                case "friendsearch"->{if(!friendSearch.isEmpty())friendSearch=friendSearch.substring(0,friendSearch.length()-1);}
            }
            return;
        }
        char c=keyToChar(key); if(c==0)return;
        switch(target){
            case "search"->search+=c; case "config"->cfgName+=c; case "cfgip"->cfgServerIp+=c;
            case "friend"->friendInput+=c; case "friendsearch"->friendSearch+=c;
        }
    }

    private char keyToChar(int key) {
        boolean shift=GLFW.glfwGetKey(mc.getWindow().getHandle(),340)==1||GLFW.glfwGetKey(mc.getWindow().getHandle(),344)==1;
        if(key>=65&&key<=90)return(char)(key+(shift?0:32));
        if(key>=48&&key<=57){if(shift){if(key==49)return'!';if(key==50)return'@';if(key==51)return'#';if(key==52)return'$';if(key==53)return'%';if(key==54)return'^';if(key==55)return'&';if(key==56)return'*';if(key==57)return'(';if(key==48)return')';}return(char)key;}
        if(key==32)return' ';if(key==45)return shift?'_':'-';if(key==46)return shift?'>':'.';if(key==95)return'_';
        return 0;
    }

    @Generated public boolean isOpen(){return isOpen;}
    @Generated public BindPopup getBindPopup(){return bindPopup;}
}
