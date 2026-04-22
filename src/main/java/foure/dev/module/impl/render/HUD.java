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
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(
    name = "HUD",
    category = Category.RENDER,
    desc = "Displays client information"
)
public class HUD extends Function {

    private final BooleanSetting watermark  = new BooleanSetting("Watermark",  this, true);
    public  final BooleanSetting arraylist  = new BooleanSetting("ArrayList",  this, true);
    public  final BooleanSetting info       = new BooleanSetting("Info",       this, true);
    private final BooleanSetting coords     = new BooleanSetting("Coords",     this, true);
    private final BooleanSetting serverInfo = new BooleanSetting("Server Info",this, true);

    private List<Function> cachedModules = new ArrayList<>();
    private long lastUpdate = 0L;

    public HUD() {
        this.addSettings(new Setting[]{watermark, arraylist, info, coords, serverInfo});
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        Renderer2D r = event.renderer();

        if ((Boolean) watermark.getValue()) renderWatermark(r);
        if ((Boolean) arraylist.getValue()) renderArrayList(r);
        if ((Boolean) info.getValue())      renderInfo(r);
    }

    // ── Watermark (top-left, like AstraWare in screenshot) ────────────────
    private void renderWatermark(Renderer2D r) {
        float x = 4f, y = 4f;
        String clientName = "Novx";
        String version    = "v1";

        float nameW  = r.getStringWidth(FontRegistry.INTER_SEMIBOLD, clientName, 9f);
        float verW   = r.getStringWidth(FontRegistry.INTER_MEDIUM,   version,    7.5f);
        float totalW = nameW + verW + 14f;
        float h      = 16f;

        // Dark bg pill
        r.rect(x, y, totalW, h, 4f, new Color(10, 5, 20, 200).getRGB());

        // Animated accent bar on left edge
        float time  = (float)(System.currentTimeMillis() % 3000L) / 3000f;
        Color c1    = lerpColor(new Color(30, 80, 255), new Color(255, 30, 80), time < 0.5f ? time*2f : (1f-time)*2f);
        Color c2    = c1.darker();
        r.gradient(x, y, 3f, h, 2f, c1.getRGB(), c1.getRGB(), c2.getRGB(), c2.getRGB());
        r.shadow(x, y, 3f, h, 8f, 2f, 1f, c1.getRGB());

        // Text: "Novx" white + "v1" dimmed
        r.text(FontRegistry.INTER_SEMIBOLD, x + 7f,          y + h/2f + 2f, 9f,   clientName, new Color(230, 230, 240).getRGB(), "l");
        r.text(FontRegistry.INTER_MEDIUM,   x + 7f + nameW + 4f, y + h/2f + 2f, 7.5f, version,    new Color(120, 120, 135).getRGB(), "l");
    }

    // ── ArrayList (right side, like Krypton screenshot) ───────────────────
    private void renderArrayList(Renderer2D r) {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 200L || cachedModules.isEmpty()) {
            cachedModules = FourEClient.getInstance().getFunctionManager().getModules()
                .stream()
                .filter(Function::isState)
                .filter(m -> !m.getName().equals("HUD"))
                .sorted(Comparator.comparingDouble(m -> -(double) r.getStringWidth(FontRegistry.INTER_MEDIUM, m.getName(), 9f)))
                .collect(Collectors.toList());
            lastUpdate = now;
        }
        if (cachedModules.isEmpty()) return;

        float screenW  = mc.getWindow().getScaledWidth();
        float itemH    = 14f;
        float gap      = 1f;
        float y        = 5f;

        for (int i = 0; i < cachedModules.size(); i++) {
            Function mod = cachedModules.get(i);
            String name  = mod.getName();
            float fontSize = 9f;
            float textW  = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize);
            float w      = textW + 8f;
            float x      = screenW - w - 4f;

            // Cycling color (same as original HUD — blue→red)
            float t = ((float)(now % 4000L) / 4000f + i * 0.06f) % 1f;
            Color accent;
            if (t < 0.5f) {
                float blend = t * 2f;
                accent = new Color(
                    Math.min(255, (int)(30  + blend * 225)),
                    Math.min(255, (int)(80  - blend * 80)),
                    Math.min(255, (int)(255 - blend * 255)), 255);
            } else {
                float blend = (t - 0.5f) * 2f;
                accent = new Color(
                    Math.min(255, (int)(255 - blend * 225)),
                    Math.min(255, (int)(0   + blend * 80)),
                    Math.min(255, (int)(0   + blend * 255)), 255);
            }
            Color accent2 = accent.darker();

            // Background
            r.rect(x, y, w, itemH, 3f, new Color(10, 5, 20, 175).getRGB());

            // Animated side bar (right edge)
            float barW = 2f;
            r.gradient(x + w - barW, y, barW, itemH, 0f,
                accent.getRGB(), accent.getRGB(), accent2.getRGB(), accent2.getRGB());
            r.shadow(x + w - barW, y, barW, itemH, 7f, 2f, 1f, accent.getRGB());

            // Text shadow + text
            r.text(FontRegistry.INTER_MEDIUM, x + 4.5f, y + itemH/2f + 3f, fontSize, name, new Color(0,0,0,140).getRGB(), "l");
            r.text(FontRegistry.INTER_MEDIUM, x + 4f,   y + itemH/2f + 2.5f, fontSize, name, -1, "l");

            y += itemH + gap;
        }
    }

    // ── Info bar (bottom-left, like screenshot) ───────────────────────────
    private void renderInfo(Renderer2D r) {
        float x   = 5f;
        float sh  = mc.getWindow().getScaledHeight();
        float y   = sh - 22f;
        float lineH = 9f;

        // FPS · Ping · ms
        int fps  = mc.getCurrentFps();
        int ping = mc.player.networkHandler != null
            ? (int)(mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()) != null
                ? mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid()).getLatency() : 0)
            : 0;

        // Color FPS: green>50, yellow>25, red<=25
        Color fpsColor = fps > 50 ? new Color(80,220,100) : fps > 25 ? new Color(220,200,50) : new Color(220,60,60);
        Color pingColor= ping < 80 ? new Color(80,220,100) : ping < 150 ? new Color(220,200,50) : new Color(220,60,60);

        String fpsStr  = fps  + " FPS";
        String pingStr = ping + " ms";
        String sep     = " · ";

        float cx = x;
        r.text(FontRegistry.INTER_MEDIUM, cx, y + lineH, 8f, fpsStr,  fpsColor.getRGB(),  "l"); cx += r.getStringWidth(FontRegistry.INTER_MEDIUM, fpsStr, 8f);
        r.text(FontRegistry.INTER_MEDIUM, cx, y + lineH, 8f, sep,     new Color(80,80,95).getRGB(), "l"); cx += r.getStringWidth(FontRegistry.INTER_MEDIUM, sep, 8f);
        r.text(FontRegistry.INTER_MEDIUM, cx, y + lineH, 8f, pingStr, pingColor.getRGB(), "l");

        // Coords (if enabled)
        if ((Boolean) coords.getValue() && mc.player != null) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String coordStr = "X " + px + " Y " + py + " Z " + pz;
            r.text(FontRegistry.INTER_MEDIUM, x, y, 8f, coordStr, new Color(140,140,160).getRGB(), "l");
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────
    private static Color lerpColor(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed())   * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t),
            255);
    }

    // Static helper other modules can call
    public static void notify(String msg) {
        foure.dev.ui.notification.NotificationManager.add("Info", msg,
            foure.dev.ui.notification.NotificationType.INFO);
    }
}

