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
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;

/**
 * NovxClickGui — One Piece / Prestige style
 * Left sidebar: logo + categories
 * Right content: modules grid OR themes tab
 * Right panel: settings popup
 */
@ModuleInfo(
    name = "ClickGui",
    category = Category.RENDER,
    desc = "Novx Client Interface"
)
public class NovxClickGui extends Function {

    // ── Layout constants ──────────────────────────────────────────────────
    private static final float WIN_W        = 620.0f;
    private static final float WIN_H        = 380.0f;
    private static final float SIDEBAR_W    = 140.0f;
    private static final float HEADER_H     = 36.0f;
    private static final float TAB_BAR_H    = 30.0f;
    private static final float SETTINGS_W   = 200.0f;

    // ── Theme definitions ─────────────────────────────────────────────────
    public static final ThemeEntry[] THEMES = {
        new ThemeEntry("Default",    "Dark neutral",    new Color(30,30,40),   new Color(180,180,190), new Color(120,120,130)),
        new ThemeEntry("Crimson",    "Deep red",        new Color(40,10,10),   new Color(220,50,50),   new Color(150,20,20)),
        new ThemeEntry("Ocean",      "Teal / cyan",     new Color(5,20,35),    new Color(0,180,220),   new Color(0,100,150)),
        new ThemeEntry("Forest",     "Muted green",     new Color(10,25,10),   new Color(60,180,60),   new Color(30,110,30)),
        new ThemeEntry("Sakura",     "Soft pink",       new Color(35,15,25),   new Color(240,120,160), new Color(180,60,100)),
        new ThemeEntry("Gold",       "Warm amber",      new Color(30,20,0),    new Color(220,170,30),  new Color(150,100,10)),
        new ThemeEntry("Midnight",   "Deep blue",       new Color(5,5,25),     new Color(80,80,220),   new Color(40,40,150)),
        new ThemeEntry("Nebula",     "Purple nebula",   new Color(15,5,30),    new Color(180,60,255),  new Color(100,20,180)),
        new ThemeEntry("Matrix",     "Hacker green",    new Color(0,12,0),     new Color(0,220,60),    new Color(0,140,30)),
        new ThemeEntry("NeonTokyo",  "Neon magenta",    new Color(20,0,20),    new Color(255,0,180),   new Color(180,0,120)),
        new ThemeEntry("SciFi",      "Sci-fi blue",     new Color(5,10,25),    new Color(0,200,255),   new Color(0,120,200)),
        new ThemeEntry("Astral",     "Cosmic silver",   new Color(15,15,20),   new Color(200,210,230), new Color(120,130,160)),
    };

    private static int activeThemeIndex = 2; // Ocean default

    // ── State ─────────────────────────────────────────────────────────────
    private boolean isOpen         = false;
    private float   animProgress   = 0.0f;
    private long    lastTime       = System.currentTimeMillis();
    private float   winX, winY;
    private boolean dragging       = false;
    private float   dragOffX, dragOffY;

    // Sidebar
    private Category selectedCategory = null;
    private int      selectedTab      = 0; // 0=Settings 1=Themes 2=Discord
    private float    sidebarScroll    = 0;

    // Modules
    private float    moduleScroll     = 0;
    private Function selectedModule   = null;
    private final List<SettingElement> settingElements = new ArrayList<>();

    // Settings panel
    private boolean  bindingKey       = false;
    private String   searchQuery      = "";
    private boolean  searchFocused    = false;

    // Theme scroll
    private float    themeScroll      = 0;

    // Misc
    private final BindPopup bindPopup = new BindPopup();

    public NovxClickGui() {
        this.setKey(344);
    }

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
            if (selectedCategory == null) {
                for (Category c : Category.values()) {
                    if (c != Category.THEME && c != Category.SCRIPT && c != Category.CONFIG) {
                        selectedCategory = c; break;
                    }
                }
            }
        } else {
            onDisable();
            mc.setScreen((Screen) null);
            dragging = false;
        }
    }

    // ── Active theme helpers ──────────────────────────────────────────────
    public static ThemeEntry activeTheme() { return THEMES[activeThemeIndex]; }
    public static Color accent()    { return activeTheme().accent; }
    public static Color accentDim() { return activeTheme().accentDim; }
    public static Color bg()        { return activeTheme().bg; }

    // ── Render ────────────────────────────────────────────────────────────
    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isOpen) return;
        long now = System.currentTimeMillis();
        float dt = (now - lastTime) / 1000f;
        lastTime = now;
        animProgress = Math.min(1f, animProgress + dt * 5f);
        float alpha = Easings.EASE_OUT_CUBIC.ease(animProgress);

        Renderer2D r = event.renderer();
        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        // Overlay
        r.rect(0, 0, sw, sh, new Color(0, 0, 0, (int)(140 * alpha)).getRGB());

        float scale = 0.92f + 0.08f * alpha;
        float px = winX + WIN_W / 2f, py = winY + WIN_H / 2f;
        r.pushScale(scale, px, py);
        r.pushAlpha(alpha);

        drawWindow(r, sw, sh);

        r.popAlpha();
        r.popScale();
    }

    private void drawWindow(Renderer2D r, float sw, float sh) {
        Color bg     = bg();
        Color acc    = accent();
        Color accDim = accentDim();
        int bgC   = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 245).getRGB();
        int sideC = new Color(Math.min(255, bg.getRed()+8), Math.min(255, bg.getGreen()+8), Math.min(255, bg.getBlue()+12), 255).getRGB();

        // Window background
        r.rect(winX, winY, WIN_W, WIN_H, 10f, bgC);
        r.rectOutline(winX, winY, WIN_W, WIN_H, 10f, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 60).getRGB(), 1f);

        // ── Sidebar ───────────────────────────────────────────────────────
        r.rect(winX, winY, SIDEBAR_W, WIN_H, new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 255).getRGB());
        // Sidebar right border
        r.rect(winX + SIDEBAR_W - 1, winY, 1, WIN_H, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 30).getRGB());

        // Logo area
        r.rect(winX, winY, SIDEBAR_W, HEADER_H, new Color(Math.max(0,bg.getRed()-5), Math.max(0,bg.getGreen()-5), Math.max(0,bg.getBlue()-5), 255).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD, winX + SIDEBAR_W / 2f, winY + HEADER_H / 2f + 2f, 11f,
            "Novx v1", acc.getRGB(), "c");

        // Categories
        float catY = winY + HEADER_H + 8f;
        for (Category cat : Category.values()) {
            if (cat == Category.THEME || cat == Category.SCRIPT || cat == Category.CONFIG) continue;
            boolean sel = cat == selectedCategory && selectedTab == 0;
            boolean hover = isInCat(cat, catY);

            if (sel) {
                r.rect(winX + 2, catY - 1, SIDEBAR_W - 4, 26f, 6f,
                    new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 30).getRGB());
                r.rect(winX + 2, catY + 6f, 3f, 14f, 1.5f, acc.getRGB());
            } else if (hover) {
                r.rect(winX + 2, catY - 1, SIDEBAR_W - 4, 26f, 6f,
                    new Color(255, 255, 255, 8).getRGB());
            }

            int txtC = sel ? acc.getRGB() : new Color(160, 160, 175).getRGB();
            // bullet
            r.rect(winX + 14, catY + 10f, 4f, 4f, 2f,
                sel ? acc.getRGB() : new Color(80, 80, 90).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, winX + 24f, catY + 13f, 8.5f,
                cat.getName(), txtC, "l");
            catY += 28f;
        }

        // Bottom sidebar items: Configs, Friends
        float botY = winY + WIN_H - 60f;
        drawSidebarBottom(r, botY, acc);

        // ── Content area ──────────────────────────────────────────────────
        float contentX = winX + SIDEBAR_W;
        float contentW = WIN_W - SIDEBAR_W - (selectedModule != null ? SETTINGS_W : 0);

        // Header bar with tabs
        drawHeaderBar(r, contentX, contentW, acc, bg);

        // Content
        float contentY = winY + TAB_BAR_H;
        float contentH = WIN_H - TAB_BAR_H;

        if (selectedTab == 0) {
            drawModules(r, contentX, contentY, contentW, contentH, acc, bg);
        } else if (selectedTab == 1) {
            drawThemes(r, contentX, contentY, contentW, contentH, acc, bg);
        } else if (selectedTab == 2) {
            drawDiscord(r, contentX, contentY, contentW, contentH, acc);
        }

        // Settings panel
        if (selectedModule != null && selectedTab == 0) {
            drawSettings(r, winX + WIN_W - SETTINGS_W, winY, SETTINGS_W, WIN_H, acc, bg);
        }
    }

    private void drawSidebarBottom(Renderer2D r, float y, Color acc) {
        // Configs
        boolean cfgSel = selectedTab == 0 && selectedCategory == null;
        r.text(FontRegistry.INTER_MEDIUM, winX + 14f, y + 10f, 8f,
            "# Configs", new Color(100,100,110).getRGB(), "l");
        // Friends
        r.text(FontRegistry.INTER_MEDIUM, winX + 14f, y + 32f, 8f,
            "# Friends", new Color(100,100,110).getRGB(), "l");
    }

    private void drawHeaderBar(Renderer2D r, float x, float w, Color acc, Color bg) {
        float y = winY;
        int hdrC = new Color(Math.max(0,bg.getRed()-3), Math.max(0,bg.getGreen()-3), Math.max(0,bg.getBlue()-3), 255).getRGB();
        r.rect(x, y, w, TAB_BAR_H, hdrC);
        r.rect(x, y + TAB_BAR_H - 1, w, 1, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 25).getRGB());

        // Tabs: Settings | Themes | Discord
        String[] tabs = {"Settings", "Themes", "Discord"};
        float tabW = 80f;
        float tabX = x + 12f;
        for (int i = 0; i < tabs.length; i++) {
            boolean sel = selectedTab == i;
            int tC = sel ? acc.getRGB() : new Color(130, 130, 145).getRGB();
            r.text(FontRegistry.INTER_MEDIUM, tabX + tabW * i + tabW / 2f, y + TAB_BAR_H / 2f + 2f,
                8f, tabs[i], tC, "c");
            if (sel) {
                r.rect(tabX + tabW * i + 8f, y + TAB_BAR_H - 2.5f, tabW - 16f, 2.5f, 1.5f, acc.getRGB());
            }
        }

        // Close button
        float cx = winX + WIN_W - 20f;
        float cy = y + TAB_BAR_H / 2f - 6f;
        r.text(FontRegistry.INTER_MEDIUM, cx, cy + 8f, 9f, "×",
            new Color(160,60,60,200).getRGB(), "c");
    }

    private void drawModules(Renderer2D r, float x, float y, float w, float h, Color acc, Color bg) {
        if (selectedCategory == null) return;
        List<Function> modules;
        if (!searchQuery.isEmpty()) {
            modules = new ArrayList<>();
            for (Category c : Category.values())
                FourEClient.getInstance().getFunctionManager().getModules(c).stream()
                    .filter(m -> m.getName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .forEach(modules::add);
        } else {
            modules = FourEClient.getInstance().getFunctionManager().getModules(selectedCategory);
        }

        // Search bar
        float sbX = x + 10f, sbY = y + 8f, sbW = w - 20f, sbH = 18f;
        r.rect(sbX, sbY, sbW, sbH, 5f,
            new Color(Math.min(255, bg.getRed()+15), Math.min(255, bg.getGreen()+15), Math.min(255, bg.getBlue()+20), 180).getRGB());
        r.rectOutline(sbX, sbY, sbW, sbH, 5f,
            searchFocused ? new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 120).getRGB()
                          : new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 30).getRGB(), 1f);
        String disp = searchQuery.isEmpty() && !searchFocused ? "Search modules..."
            : searchQuery + (searchFocused && System.currentTimeMillis() % 900 > 450 ? "|" : "");
        int sC = searchQuery.isEmpty() ? new Color(80,80,90).getRGB() : -1;
        r.text(FontRegistry.INTER_MEDIUM, sbX + 8f, sbY + sbH / 2f + 2f, 7.5f, disp, sC, "l");

        // Module list
        float listY = y + 34f;
        float listH = h - 36f;
        float mH = 26f, gap = 4f;
        float curY = listY - moduleScroll;

        r.pushClipRect((int)x, (int)listY, (int)w, (int)listH);
        for (Function mod : modules) {
            if (curY + mH < listY) { curY += mH + gap; continue; }
            if (curY > listY + listH) break;

            boolean tog = mod.isToggled();
            boolean sel = mod == selectedModule;
            float mx2 = x + 8f, mw2 = w - 16f;

            if (tog) {
                r.rect(mx2, curY, mw2, mH, 5f,
                    new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 35).getRGB());
                r.rectOutline(mx2, curY, mw2, mH, 5f,
                    new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 120).getRGB(), 1f);
            } else if (sel) {
                r.rect(mx2, curY, mw2, mH, 5f,
                    new Color(255, 255, 255, 10).getRGB());
                r.rectOutline(mx2, curY, mw2, mH, 5f,
                    new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 50).getRGB(), 1f);
            } else {
                r.rect(mx2, curY, mw2, mH, 5f,
                    new Color(255, 255, 255, 6).getRGB());
            }

            // Toggle dot
            float dotX = mx2 + 10f, dotY = curY + mH / 2f - 3f;
            r.rect(dotX, dotY, 6f, 6f, 3f,
                tog ? acc.getRGB() : new Color(60, 60, 70).getRGB());
            if (tog) r.shadow(dotX, dotY, 6f, 6f, 8f, 2f, 1f, acc.getRGB());

            int tC = tog ? acc.getRGB() : new Color(190, 190, 200).getRGB();
            r.text(FontRegistry.INTER_MEDIUM, mx2 + 22f, curY + mH / 2f + 2f, 8f, mod.getName(), tC, "l");

            // Settings arrow if selected
            if (sel) {
                r.text(FontRegistry.INTER_MEDIUM, mx2 + mw2 - 14f, curY + mH / 2f + 2f, 8f, "›",
                    new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 180).getRGB(), "c");
            }

            curY += mH + gap;
        }
        r.popClipRect();

        // Scrollbar
        float totalH = modules.size() * (mH + gap);
        if (totalH > listH) {
            float maxS = totalH - listH;
            moduleScroll = Math.max(0, Math.min(moduleScroll, maxS));
            float thumbH = Math.max(20f, listH / totalH * listH);
            float thumbY = listY + (moduleScroll / maxS) * (listH - thumbH);
            r.rect(x + w - 6f, listY, 3f, listH, 1.5f, new Color(255,255,255,15).getRGB());
            r.rect(x + w - 6f, thumbY, 3f, thumbH, 1.5f, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 100).getRGB());
        }
    }

    private void drawThemes(Renderer2D r, float x, float y, float w, float h, Color acc, Color bg) {
        // Active theme label
        r.text(FontRegistry.INTER_MEDIUM, x + 14f, y + 14f, 8f,
            "Active: " + activeTheme().name,
            new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 200).getRGB(), "l");

        float itemH = 52f, gap = 6f;
        float listY = y + 30f, listH = h - 32f;
        float curY = listY - themeScroll;

        r.pushClipRect((int)x, (int)listY, (int)(w - 8), (int)listH);
        for (int i = 0; i < THEMES.length; i++) {
            ThemeEntry t = THEMES[i];
            if (curY + itemH < listY) { curY += itemH + gap; continue; }
            if (curY > listY + listH) break;

            boolean isActive = i == activeThemeIndex;
            float ix = x + 10f, iw = w - 20f;

            // Card background
            r.rect(ix, curY, iw, itemH, 7f,
                isActive
                    ? new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 20).getRGB()
                    : new Color(255,255,255, 6).getRGB());
            r.rectOutline(ix, curY, iw, itemH, 7f,
                isActive
                    ? new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 100).getRGB()
                    : new Color(255,255,255,18).getRGB(), 1f);

            // Color swatches
            r.rect(ix + 10f, curY + 16f, 14f, 14f, 3f, t.bg.getRGB());
            r.rect(ix + 28f, curY + 16f, 14f, 14f, 3f, t.accent.getRGB());
            r.rect(ix + 46f, curY + 16f, 14f, 14f, 3f,
                new Color(230,230,235).getRGB());

            // Name & description
            r.text(FontRegistry.INTER_SEMIBOLD, ix + 68f, curY + 18f, 8.5f,
                t.name, new Color(230,230,235).getRGB(), "l");
            r.text(FontRegistry.INTER_MEDIUM, ix + 68f, curY + 30f, 7.5f,
                t.description, new Color(130,130,145).getRGB(), "l");

            if (isActive) {
                r.text(FontRegistry.INTER_MEDIUM, ix + iw - 60f, curY + itemH / 2f + 2f, 7.5f,
                    "✓ Active", new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 220).getRGB(), "l");
            } else {
                // Apply button
                float btnX = ix + iw - 55f, btnY = curY + itemH / 2f - 9f;
                r.rect(btnX, btnY, 48f, 18f, 5f,
                    new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 40).getRGB());
                r.rectOutline(btnX, btnY, 48f, 18f, 5f,
                    new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 100).getRGB(), 1f);
                r.text(FontRegistry.INTER_MEDIUM, btnX + 24f, btnY + 11f, 7.5f,
                    "Apply", new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 230).getRGB(), "c");
            }

            curY += itemH + gap;
        }
        r.popClipRect();

        // Scrollbar
        float totalH = THEMES.length * (itemH + gap);
        if (totalH > listH) {
            float maxS = totalH - listH;
            themeScroll = Math.max(0, Math.min(themeScroll, maxS));
            float thumbH = Math.max(20f, listH / totalH * listH);
            float thumbY = listY + (themeScroll / maxS) * (listH - thumbH);
            r.rect(x + w - 6f, listY, 3f, listH, 1.5f, new Color(255,255,255,15).getRGB());
            r.rect(x + w - 6f, thumbY, 3f, thumbH, 1.5f, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 100).getRGB());
        }
    }

    private void drawDiscord(Renderer2D r, float x, float y, float w, float h, Color acc) {
        r.text(FontRegistry.INTER_SEMIBOLD, x + w / 2f, y + h / 2f - 10f, 10f,
            "Discord", acc.getRGB(), "c");
        r.text(FontRegistry.INTER_MEDIUM, x + w / 2f, y + h / 2f + 8f, 8f,
            "discord.gg/novx", new Color(130,130,145).getRGB(), "c");
    }

    private void drawSettings(Renderer2D r, float x, float y, float w, float h, Color acc, Color bg) {
        if (selectedModule == null) return;
        int panelC = new Color(Math.max(0,bg.getRed()-5), Math.max(0,bg.getGreen()-5), Math.max(0,bg.getBlue()-8), 252).getRGB();
        r.rect(x, y, w, h, 0f, panelC);
        r.rect(x, y, 1f, h, new Color(acc.getRed(), acc.getGreen(), acc.getBlue(), 25).getRGB());

        // Module name
        r.text(FontRegistry.INTER_SEMIBOLD, x + w / 2f, y + 20f, 9.5f,
            selectedModule.getName(), acc.getRGB(), "c");
        r.text(FontRegistry.INTER_MEDIUM, x + w / 2f, y + 32f, 7f,
            selectedModule.getDesc(), new Color(100,100,115).getRGB(), "c");

        // Bind button
        float bY = y + 46f;
        r.rect(x + 12f, bY, w - 24f, 20f, 5f, new Color(255,255,255,8).getRGB());
        r.rectOutline(x + 12f, bY, w - 24f, 20f, 5f, new Color(255,255,255,20).getRGB(), 1f);
        String bindTxt = bindingKey ? "Listening..." : "Bind: " + KeyNameUtil.getKeyName(selectedModule.getKey());
        r.text(FontRegistry.INTER_MEDIUM, x + w / 2f, bY + 12f, 7.5f, bindTxt, -1, "c");

        // Settings
        float sY = bY + 28f;
        if (settingElements.isEmpty() && !selectedModule.getSettings().isEmpty()) rebuildSettings();
        for (SettingElement el : settingElements) {
            el.render(r, FontRegistry.INTER_MEDIUM, x + 10f, sY, w - 20f, 1f);
            sY += el.getHeight();
        }
    }

    // ── Mouse / Key handling ──────────────────────────────────────────────
    public void handleMouseClick(double rawMx, double rawMy, int btn) {
        if (!isOpen) return;
        if (bindPopup.isActive()) { bindPopup.mouseClicked(rawMx, rawMy, btn); return; }
        double mx = rawMx, my = rawMy;

        // Close button
        float cx = winX + WIN_W - 20f, cy = winY + TAB_BAR_H / 2f - 6f;
        if (mx >= cx - 8 && mx <= cx + 8 && my >= cy && my <= cy + 14 && btn == 0) {
            toggle(); return;
        }

        // Dragging header
        if (mx >= winX + SIDEBAR_W && mx <= winX + WIN_W && my >= winY && my <= winY + TAB_BAR_H && btn == 0) {
            // Check tabs first
            float tabX = winX + SIDEBAR_W + 12f;
            for (int i = 0; i < 3; i++) {
                if (mx >= tabX + 80f * i && mx <= tabX + 80f * (i + 1)) {
                    selectedTab = i;
                    if (i != 0) { selectedModule = null; settingElements.clear(); }
                    return;
                }
            }
            dragging = true;
            dragOffX = (float)(mx - winX);
            dragOffY = (float)(my - winY);
            return;
        }

        // Sidebar categories
        if (mx >= winX && mx <= winX + SIDEBAR_W) {
            float catY = winY + HEADER_H + 8f;
            for (Category cat : Category.values()) {
                if (cat == Category.THEME || cat == Category.SCRIPT || cat == Category.CONFIG) continue;
                if (my >= catY && my <= catY + 26f) {
                    selectedCategory = cat;
                    selectedTab = 0;
                    selectedModule = null;
                    settingElements.clear();
                    moduleScroll = 0;
                    return;
                }
                catY += 28f;
            }
            return;
        }

        float contentX = winX + SIDEBAR_W;
        float contentW = WIN_W - SIDEBAR_W - (selectedModule != null ? SETTINGS_W : 0);

        // Theme apply buttons
        if (selectedTab == 1) {
            float listY = winY + TAB_BAR_H + 30f;
            float itemH = 52f, gap = 6f;
            float curY = listY - themeScroll;
            for (int i = 0; i < THEMES.length; i++) {
                ThemeEntry t = THEMES[i];
                float ix = contentX + 10f, iw = contentW - 20f;
                if (i != activeThemeIndex) {
                    float btnX = ix + iw - 55f, btnY = curY + itemH / 2f - 9f;
                    if (mx >= btnX && mx <= btnX + 48f && my >= btnY && my <= btnY + 18f) {
                        activeThemeIndex = i;
                        return;
                    }
                }
                curY += itemH + gap;
            }
            return;
        }

        // Module clicks
        if (selectedTab == 0 && selectedCategory != null) {
            List<Function> modules = FourEClient.getInstance().getFunctionManager().getModules(selectedCategory);
            float listY = winY + TAB_BAR_H + 34f;
            float mH = 26f, gap = 4f;
            float curY = listY - moduleScroll;
            for (Function mod : modules) {
                float mx2 = contentX + 8f, mw2 = contentW - 16f;
                if (my >= curY && my <= curY + mH && mx >= mx2 && mx <= mx2 + mw2) {
                    if (btn == 0) mod.toggle();
                    else if (btn == 1) {
                        if (selectedModule == mod) { selectedModule = null; settingElements.clear(); }
                        else { selectedModule = mod; settingElements.clear(); rebuildSettings(); }
                    }
                    return;
                }
                curY += mH + gap;
            }

            // Settings panel clicks
            if (selectedModule != null) {
                float spX = winX + WIN_W - SETTINGS_W;
                float bY = winY + TAB_BAR_H + 46f;
                if (mx >= spX + 12 && mx <= spX + SETTINGS_W - 12 && my >= bY && my <= bY + 20 && btn == 0) {
                    bindingKey = !bindingKey; return;
                }
                float sY = bY + 28f;
                for (SettingElement el : settingElements) {
                    if (el.mouseClicked(mx, my, btn, spX + 10f, sY, SETTINGS_W - 20f)) return;
                    sY += el.getHeight();
                }
            }

            // Search bar
            float sbX = contentX + 10f, sbY = winY + TAB_BAR_H + 8f;
            if (mx >= sbX && mx <= sbX + contentW - 20f && my >= sbY && my <= sbY + 18f) {
                searchFocused = !searchFocused; return;
            }
        }
    }

    public void handleMouseRelease(double mx, double my, int btn) {
        if (!isOpen) return;
        dragging = false;
        if (bindPopup.isActive()) bindPopup.mouseReleased(mx, my, btn);
        for (SettingElement el : settingElements) el.mouseReleased(mx, my, btn);
    }

    public void handleMouseDrag(double mx, double my, int btn, double dx, double dy) {
        if (!isOpen) return;
        if (dragging && btn == 0) {
            winX = (float)(mx - dragOffX);
            winY = (float)(my - dragOffY);
        } else if (selectedModule != null) {
            float spX = winX + WIN_W - SETTINGS_W;
            float sY = winY + TAB_BAR_H + 74f;
            for (SettingElement el : settingElements) {
                el.mouseDragged(mx, my, btn, spX + 10f, sY, SETTINGS_W - 20f);
                sY += el.getHeight();
            }
        }
    }

    public void handleMouseScroll(double amount) {
        if (!isOpen) return;
        if (selectedTab == 0) moduleScroll -= (float) amount * 18f;
        else if (selectedTab == 1) themeScroll -= (float) amount * 18f;
    }

    @Subscribe
    public void onMouseScroll(EventMouseScroll event) {
        if (isOpen) handleMouseScroll(event.getDelta());
    }

    @Subscribe
    public void onPress(EventPress e) {
        if (e.getAction() == 0) return;
        int key = e.getKey();
        for (SettingElement el : settingElements) {
            if (el instanceof TextElement te && te.isListening()) { te.handleKeyPress(key); return; }
        }
        if (bindingKey && selectedModule != null) {
            selectedModule.setKey(key != 256 && key != 261 ? key : 0);
            bindingKey = false; return;
        }
        for (SettingElement el : settingElements) {
            if (el instanceof BindElement be && be.getSetting() instanceof BindSetting bs && bs.isListening()) {
                bs.setValue(key != 256 && key != 261 ? key : -1);
                bs.setListening(false); return;
            }
        }
        if (searchFocused) {
            if (key == 256 || key == 257) searchFocused = false;
            else if (key == 259) { if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length()-1); }
            else { char c = keyToChar(key); if (c != 0) searchQuery += c; }
        } else if (key == 256) toggle();
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private boolean isInCat(Category cat, float catY) {
        if (!isOpen) return false;
        double mx = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double my = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        return mx >= winX && mx <= winX + SIDEBAR_W && my >= catY && my <= catY + 26f;
    }

    private void rebuildSettings() {
        settingElements.clear();
        if (selectedModule == null) return;
        for (Setting<?> s : selectedModule.getSettings()) {
            if (s instanceof BooleanSetting bs)       settingElements.add(new BooleanElement(bs));
            else if (s instanceof NumberSetting ns)   settingElements.add(new NumberElement(ns));
            else if (s instanceof MultiBoxSetting ms) settingElements.add(new MultiBoxElement(ms));
            else if (s instanceof ModeSetting ms)     settingElements.add(new ModeElement(ms));
            else if (s instanceof ColorSetting cs)    settingElements.add(new ColorElement(cs));
            else if (s instanceof StringSetting ss)   settingElements.add(new TextElement(ss));
            else if (s instanceof BindSetting bs)     settingElements.add(new BindElement(bs));
        }
    }

    private char keyToChar(int key) {
        boolean shift = GLFW.glfwGetKey(mc.getWindow().getHandle(), 340) == 1
                     || GLFW.glfwGetKey(mc.getWindow().getHandle(), 344) == 1;
        if (key >= 65 && key <= 90) return (char)(key + (shift ? 0 : 32));
        if (key >= 48 && key <= 57) {
            if (shift) { if(key==49)return'!';if(key==50)return'@';if(key==51)return'#';
                         if(key==52)return'$';if(key==53)return'%';if(key==54)return'^';
                         if(key==55)return'&';if(key==56)return'*';if(key==57)return'(';if(key==48)return')'; }
            return (char) key;
        }
        if (key == 32) return ' ';
        if (key == 45) return shift ? '_' : '-';
        if (key == 46) return shift ? '>' : '.';
        return 0;
    }

    @Generated public boolean isOpen() { return isOpen; }
    @Generated public BindPopup getBindPopup() { return bindPopup; }

    // ── Theme entry ───────────────────────────────────────────────────────
    public record ThemeEntry(String name, String description, Color bg, Color accent, Color accentDim) {}
}
