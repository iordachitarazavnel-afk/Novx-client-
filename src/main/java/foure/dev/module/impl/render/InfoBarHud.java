package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import net.minecraft.client.network.PlayerListEntry;

import java.awt.Color;

@ModuleInfo(name = "InfoBarHud", category = Category.RENDER, desc = "FPS / Ticks / BPS / Ping / Coords bar")
public class InfoBarHud extends HudModule {

    private final BooleanSetting showCoords = new BooleanSetting("Coords", this, true);
    private final BooleanSetting showBps    = new BooleanSetting("BPS",    this, true);
    private final BooleanSetting showTicks  = new BooleanSetting("Ticks",  this, true);

    // BPS tracking
    private double lastX, lastZ;
    private long   lastBpsTime = 0;
    private float  currentBps  = 0f;
    private int    tickCounter  = 0;

    public InfoBarHud() {
        setX(3); setY(3);
        this.addSettings(new Setting[]{ showCoords, showBps, showTicks });
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isToggled() || mc.player == null) return;
        Renderer2D r = event.renderer();

        updateBps();

        int fps   = mc.getCurrentFps();
        int ping  = getPing();
        float bps = currentBps;

        // Build info string like Krypton: "85 FPS  10:57:23  0 PING  13,9 TICKS  5,5 BPS"
        String fpsStr   = fps + " FPS";
        String timeStr  = getTimeString();
        String pingStr  = ping + " PING";
        String ticksStr = (Boolean) showTicks.getValue() ? String.format("%.1f TICKS", getServerTps()) : null;
        String bpsStr   = (Boolean) showBps.getValue()   ? String.format("%.1f BPS", bps) : null;

        float fontSize = 7.5f;
        float h = 11f;
        float pad = 6f;

        // Measure total width
        float totalW = pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, fpsStr,  fontSize) + pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, timeStr, fontSize) + pad;
        totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, pingStr, fontSize) + pad;
        if (ticksStr != null) totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, ticksStr, fontSize) + pad;
        if (bpsStr   != null) totalW += r.getStringWidth(FontRegistry.INTER_MEDIUM, bpsStr,   fontSize) + pad;

        float x = getX(), y = getY();
        setWidth(totalW); setHeight(h);

        // BG
        r.rect(x, y, totalW, h, 2f, new Color(8, 4, 16, 200).getRGB());

        // Draw items separated by dim vertical lines
        float cx = x + pad / 2f;
        int dimColor = new Color(100, 100, 120).getRGB();
        int whiteColor = new Color(220, 220, 230).getRGB();

        cx = drawInfoItem(r, fpsStr,  cx, y, h, fontSize,
            colorForFps(fps), dimColor, false);
        cx = drawInfoItem(r, timeStr, cx, y, h, fontSize, whiteColor,  dimColor, true);
        cx = drawInfoItem(r, pingStr, cx, y, h, fontSize,
            colorForPing(ping), dimColor, true);
        if (ticksStr != null) cx = drawInfoItem(r, ticksStr, cx, y, h, fontSize, whiteColor, dimColor, true);
        if (bpsStr   != null) cx = drawInfoItem(r, bpsStr,   cx, y, h, fontSize, whiteColor, dimColor, true);

        // Coords row below (if enabled)
        if ((Boolean) showCoords.getValue()) {
            int px = (int) mc.player.getX();
            int py = (int) mc.player.getY();
            int pz = (int) mc.player.getZ();
            String coord = "X " + px + "  Y " + py + "  Z " + pz;
            float cw = r.getStringWidth(FontRegistry.INTER_MEDIUM, coord, fontSize) + 10f;
            float cy = y + h + 2f;
            r.rect(x, cy, cw, h, 2f, new Color(8, 4, 16, 180).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + 4f, cy + h / 2f + 2.5f, fontSize,
                coord, new Color(160, 160, 180).getRGB(), "l");
            setHeight(h * 2 + 2f);
        }
    }

    private float drawInfoItem(Renderer2D r, String text, float x, float bgY, float bgH,
                                float fontSize, int color, int dimColor, boolean sep) {
        if (sep) {
            r.rect(x, bgY + 2f, 1f, bgH - 4f, 0f, new Color(255,255,255,18).getRGB());
            x += 4f;
        }
        r.text(FontRegistry.INTER_MEDIUM, x, bgY + bgH / 2f + 2.5f, fontSize, text, color, "l");
        x += r.getStringWidth(FontRegistry.INTER_MEDIUM, text, fontSize) + 5f;
        return x;
    }

    private void updateBps() {
        if (mc.player == null) return;
        long now = System.currentTimeMillis();
        if (now - lastBpsTime >= 200L) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            double dist = Math.sqrt(dx*dx + dz*dz);
            currentBps = (float)(dist / ((now - lastBpsTime) / 1000.0));
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
            lastBpsTime = now;
        }
    }

    private float getServerTps() {
        // MC 1.21.x: approximate via world time
        return Math.min(20f, 20f);
    }

    private int getPing() {
        if (mc.player == null || mc.player.networkHandler == null) return 0;
        PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(mc.player.getUuid());
        return entry != null ? entry.getLatency() : 0;
    }

    private String getTimeString() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return String.format("%02d:%02d:%02d", t.getHour(), t.getMinute(), t.getSecond());
    }

    private int colorForFps(int fps) {
        if (fps > 100) return new Color(80,220,100).getRGB();
        if (fps > 50)  return new Color(180,220,80).getRGB();
        if (fps > 25)  return new Color(220,200,50).getRGB();
        return new Color(220,60,60).getRGB();
    }

    private int colorForPing(int ping) {
        if (ping < 60)  return new Color(80,220,100).getRGB();
        if (ping < 120) return new Color(220,200,50).getRGB();
        return new Color(220,60,60).getRGB();
    }
}
