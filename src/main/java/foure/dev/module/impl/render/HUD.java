package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.input.KeyNameUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "HUD", category = Category.RENDER, desc = "Displays client information")
public class HUD extends HudModule {

    private final BooleanSetting showWatermark = new BooleanSetting("Watermark",      this, true);
    private final BooleanSetting showInfoBar   = new BooleanSetting("Info Bar",        this, true);
    private final BooleanSetting showHotkeys   = new BooleanSetting("Hotkeys",         this, true);
    private final BooleanSetting showPotions   = new BooleanSetting("Potions",         this, true);
    private final BooleanSetting showArrayList = new BooleanSetting("ArrayList",       this, true);
    private final BooleanSetting showCoords    = new BooleanSetting("Coords",          this, true);
    private final BooleanSetting showBps       = new BooleanSetting("BPS",             this, true);
    private final BooleanSetting showTicks     = new BooleanSetting("Ticks",           this, true);

    // BPS tracking
    private double lastX = 0, lastZ = 0;
    private long   lastBpsTime = 0;
    private float  currentBps  = 0f;

    // ArrayList cache
    private List<Function> cachedModules = new ArrayList<>();
    private long lastListUpdate = 0L;

    public HUD() {
        this.addSettings(new Setting[]{
            showWatermark, showInfoBar, showHotkeys,
            showPotions, showArrayList, showCoords, showBps, showTicks
        });
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (mc.player == null || mc.world == null) return;
        Renderer2D r = event.renderer();
        updateBps();

        float sw = mc.getWindow().getScaledWidth();
        float sh = mc.getWindow().getScaledHeight();

        if ((Boolean) showWatermark.getValue()) renderWatermark(r, sw);
        if ((Boolean) showInfoBar.getValue())   renderInfoBar(r, sw);
        if ((Boolean) showHotkeys.getValue())   renderHotkeys(r);
        if ((Boolean) showPotions.getValue())   renderPotions(r, sw);
        if ((Boolean) showArrayList.getValue()) renderArrayList(r, sw);
    }

    // ── 1. WATERMARK — center top ─────────────────────────────────────────
    private void renderWatermark(Renderer2D r, float sw) {
        String text = "— NOVX —";
        float fontSize = 9f;
        float textW = r.getStringWidth(FontRegistry.INTER_SEMIBOLD, text, fontSize);
        float w = textW + 16f, h = 14f;
        float x = sw / 2f - w / 2f, y = 3f;

        r.rect(x, y, w, h, 3f, new Color(8, 4, 16, 200).getRGB());
        r.rectOutline(x, y, w, h, 3f, new Color(255,255,255,15).getRGB(), 1f);

        Color c = getAnimColor(0);
        r.text(FontRegistry.INTER_SEMIBOLD, x + w / 2f, y + h / 2f + 2.5f, fontSize,
            text, c.getRGB(), "c");
    }

    // ── 2. INFO BAR — top left: FPS · TIME · PING · TICKS · BPS + coords ─
    private void renderInfoBar(Renderer2D r, float sw) {
        int fps  = mc.getCurrentFps();
        int ping = getPing();

        String fpsStr   = fps + " FPS";
        String timeStr  = getTimeStr();
        String pingStr  = ping + " PING";
        String ticksStr = (Boolean) showTicks.getValue() ? "20,0 TICKS" : null;
        String bpsStr   = (Boolean) showBps.getValue()
            ? String.format("%.1f BPS", currentBps).replace('.', ',') : null;

        float fontSize = 7.5f, h = 11f, pad = 5f;
        float x = 3f, y = 3f;

        // Measure width
        float totalW = pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, fpsStr,  fontSize) + pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, timeStr, fontSize) + pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, pingStr, fontSize) + pad;
        if (ticksStr != null) totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, ticksStr, fontSize) + pad;
        if (bpsStr   != null) totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, bpsStr,   fontSize) + pad;

        r.rect(x, y, totalW, h, 2f, new Color(8, 4, 16, 200).getRGB());

        float cx = x + pad / 2f;
        cx = drawInfoSeg(r, fpsStr,  cx, y, h, fontSize, colorFps(fps),   false);
        cx = drawInfoSeg(r, timeStr, cx, y, h, fontSize, 0xFFCCCCDD,       true);
        cx = drawInfoSeg(r, pingStr, cx, y, h, fontSize, colorPing(ping),  true);
        if (ticksStr != null) cx = drawInfoSeg(r, ticksStr, cx, y, h, fontSize, 0xFFCCCCDD, true);
        if (bpsStr   != null)      drawInfoSeg(r, bpsStr,   cx, y, h, fontSize, 0xFFCCCCDD, true);

        // Coords line below
        if ((Boolean) showCoords.getValue() && mc.player != null) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String coord = "-" + Math.abs(px) + "  " + py + "  " + Math.abs(pz) + "350";
            // full coords
            coord = "X " + px + "  Y " + py + "  Z " + pz;
            float cw = r.getStringWidth(FontRegistry.INTER_MEDIUM, coord, fontSize) + 10f;
            float cy = y + h + 2f;
            r.rect(x, cy, cw, h, 2f, new Color(8, 4, 16, 180).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + 4f, cy + h / 2f + 2.5f, fontSize,
                coord, new Color(160,160,180).getRGB(), "l");
        }
    }

    private float drawInfoSeg(Renderer2D r, String text, float x, float bgY, float bgH,
                               float fs, int color, boolean sep) {
        if (sep) {
            r.rect(x, bgY + 2f, 1f, bgH - 4f, 0f, new Color(255,255,255,18).getRGB());
            x += 4f;
        }
        r.text(FontRegistry.INTER_MEDIUM, x, bgY + bgH / 2f + 2.5f, fs, text, color, "l");
        return x + r.getStringWidth(FontRegistry.INTER_MEDIUM, text, fs) + 5f;
    }

    // ── 3. HOTKEYS — left side panel ─────────────────────────────────────
    private void renderHotkeys(Renderer2D r) {
        List<Function> bound = FourEClient.getInstance().getFunctionManager().getModules()
            .stream().filter(m -> m.getKey() > 0).collect(Collectors.toList());

        float x = 130f, y = 58f;
        float fontSize = 8f, itemH = 12f, gap = 2f, pad = 6f;

        // Header
        float headerW = 90f;
        for (Function m : bound) {
            String line = m.getName() + "  " + KeyNameUtil.getKeyName(m.getKey());
            float w2 = r.getStringWidth(FontRegistry.INTER_MEDIUM, line, fontSize) + pad * 2;
            if (w2 > headerW) headerW = w2;
        }
        r.rect(x, y, headerW, 12f, 3f, new Color(8,4,16,200).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD, x + headerW/2f, y + 8f, 7.5f,
            "HOTKEYS", new Color(180,180,200).getRGB(), "c");

        float cy = y + 14f;
        if (bound.isEmpty()) {
            r.rect(x, cy, headerW, itemH, 2f, new Color(8,4,16,160).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + pad, cy + itemH/2f + 2.5f, fontSize,
                "None bound", new Color(80,80,95).getRGB(), "l");
            return;
        }
        for (int i = 0; i < bound.size(); i++) {
            Function m = bound.get(i);
            boolean active = m.isToggled();
            r.rect(x, cy, headerW, itemH, 2f, new Color(8,4,16,active?180:140).getRGB());
            int nameColor = active ? 0xFFDDDDEB : 0xFF787887;
            r.text(FontRegistry.INTER_MEDIUM, x + pad, cy + itemH/2f + 2.5f, fontSize,
                m.getName(), nameColor, "l");
            r.text(FontRegistry.INTER_MEDIUM, x + headerW - pad, cy + itemH/2f + 2.5f, fontSize,
                KeyNameUtil.getKeyName(m.getKey()), getAnimColor(i).getRGB(), "r");
            cy += itemH + gap;
        }
    }

    // ── 4. POTIONS — top right ────────────────────────────────────────────
    private void renderPotions(Renderer2D r, float sw) {
        if (mc.player == null) return;
        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        List<StatusEffectInstance> list = new ArrayList<>(effects);
        if (list.isEmpty()) return;

        float fontSize = 8f, itemH = 13f, gap = 1f;
        float maxW = 80f;
        for (StatusEffectInstance eff : list) {
            String name = eff.getEffectType().value().getName().getString();
            float w2 = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize) + 50f;
            if (w2 > maxW) maxW = w2;
        }

        // Position: center-right of screen (like Krypton ~550,35)
        float x = sw / 2f + 20f, y = 35f;

        // Header
        r.rect(x, y, maxW, 12f, 3f, new Color(8,4,16,200).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD, x + maxW/2f, y + 8f, 7.5f,
            "ACTIVE POTIONS", new Color(180,180,200).getRGB(), "c");

        float cy = y + 14f;
        for (StatusEffectInstance eff : list) {
            String name     = eff.getEffectType().value().getName().getString();
            String duration = formatDuration(eff.getDuration());
            int amp          = eff.getAmplifier() + 1;
            boolean good     = eff.getEffectType().value().isBeneficial();
            Color effColor   = good ? new Color(80,200,120) : new Color(200,80,80);
            String label     = name + (amp > 1 ? " " + toRoman(amp) : "");

            r.rect(x, cy, maxW, itemH, 2f, new Color(8,4,16,170).getRGB());
            r.rect(x + 4f, cy + itemH/2f - 3f, 6f, 6f, 3f, effColor.getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + 14f, cy + itemH/2f + 2.5f, fontSize,
                label, new Color(220,220,235).getRGB(), "l");
            r.text(FontRegistry.INTER_MEDIUM, x + maxW - 5f, cy + itemH/2f + 2.5f, fontSize,
                duration, effColor.brighter().getRGB(), "r");
            cy += itemH + gap;
        }
    }

    // ── 5. ARRAYLIST — right side, white text + animated colored bar ──────
    private void renderArrayList(Renderer2D r, float sw) {
        long now = System.currentTimeMillis();
        if (now - lastListUpdate > 200L) {
            cachedModules = FourEClient.getInstance().getFunctionManager().getModules()
                .stream()
                .filter(Function::isState)
                .filter(m -> !(m instanceof HudModule) && !m.getName().equals("HUD"))
                .sorted(Comparator.comparingDouble(m ->
                    -(double) r.getStringWidth(FontRegistry.INTER_MEDIUM, m.getName(), 9f)))
                .collect(Collectors.toList());
            lastListUpdate = now;
        }
        if (cachedModules.isEmpty()) return;

        float fontSize = 9f, itemH = 13f, gap = 1f, y = 5f;

        for (int i = 0; i < cachedModules.size(); i++) {
            Function mod = cachedModules.get(i);
            String name  = mod.getName();
            float textW  = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize);
            float w      = textW + 10f;
            float x      = sw - w - 4f;

            Color accent  = getAnimColor(i);
            Color accent2 = accent.darker();

            // Background
            r.rect(x, y, w, itemH, 2f, new Color(8, 4, 16, 180).getRGB());

            // Animated bar right edge
            float barW = 2f;
            r.gradient(x + w - barW, y, barW, itemH, 0f,
                accent.getRGB(), accent.getRGB(), accent2.getRGB(), accent2.getRGB());
            r.shadow(x + w - barW, y, barW, itemH, 6f, 2f, 1f, accent.getRGB());

            // White text
            r.text(FontRegistry.INTER_MEDIUM, x + 4.5f, y + itemH/2f + 3f, fontSize,
                name, new Color(0,0,0,120).getRGB(), "l");
            r.text(FontRegistry.INTER_MEDIUM, x + 4f, y + itemH/2f + 2.5f, fontSize,
                name, new Color(230, 230, 240).getRGB(), "l");

            y += itemH + gap;
        }
    }

    // ── Util ──────────────────────────────────────────────────────────────
    public static Color getAnimColor(int index) {
        float t = ((float)(System.currentTimeMillis() % 4000L) / 4000f + index * 0.06f) % 1f;
        if (t < 0.5f) {
            float b = t * 2f;
            return new Color(
                Math.min(255, (int)(30  + b * 225)),
                Math.min(255, (int)(80  - b * 80)),
                Math.min(255, (int)(255 - b * 255)), 255);
        } else {
            float b = (t - 0.5f) * 2f;
            return new Color(
                Math.min(255, (int)(255 - b * 225)),
                Math.min(255, (int)(0   + b * 80)),
                Math.min(255, (int)(0   + b * 255)), 255);
        }
    }

    private void updateBps() {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBpsTime >= 200L) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            currentBps = (float)(Math.sqrt(dx*dx + dz*dz) / ((now - lastBpsTime) / 1000.0));
            lastX = mc.player.getX(); lastZ = mc.player.getZ(); lastBpsTime = now;
        }
    }

    private int getPing() {
        if (mc.player == null || mc.player.networkHandler == null) return 0;
        PlayerListEntry e = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
        return e != null ? e.getLatency() : 0;
    }

    private String getTimeStr() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
    }

    private int colorFps(int fps) {
        if (fps > 100) return new Color(80,220,100).getRGB();
        if (fps > 50)  return new Color(180,220,80).getRGB();
        if (fps > 25)  return new Color(220,200,50).getRGB();
        return new Color(220,60,60).getRGB();
    }

    private int colorPing(int ping) {
        if (ping < 60)  return new Color(80,220,100).getRGB();
        if (ping < 120) return new Color(220,200,50).getRGB();
        return new Color(220,60,60).getRGB();
    }

    private String formatDuration(int ticks) {
        if (ticks > 32767) return "∞";
        int s = ticks / 20;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private String toRoman(int n) {
        return switch (n) { case 2->"II"; case 3->"III"; case 4->"IV"; case 5->"V"; default->String.valueOf(n); };
    }

    public static void notify(String msg) {
        foure.dev.ui.notification.NotificationManager.add("Info", msg,
            foure.dev.ui.notification.NotificationType.INFO);
    }
}
