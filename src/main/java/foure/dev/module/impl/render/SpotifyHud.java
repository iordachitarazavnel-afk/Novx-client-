package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.Track;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;

import java.awt.Color;

@ModuleInfo(name = "SpotifyHud", category = Category.RENDER, desc = "Spotify now playing overlay")
public class SpotifyHud extends HudModule {

    // ── Spotify state ─────────────────────────────────────────────────────
    private SpotifyAPI spotifyAPI;
    private Track   currentTrack    = null;
    private int     currentPosition = 0;
    private boolean isPlaying       = false;
    private boolean connected       = false;
    private boolean initialized     = false;
    private long    lastUpdate      = 0L;

    private final Object lock = new Object();

    // ── Layout ────────────────────────────────────────────────────────────
    private static final float W      = 200f;
    private static final float ART_S  = 40f;
    private static final float PAD    = 6f;
    private static final float H      = ART_S + PAD * 2 + 14f;

    public SpotifyHud() {
        // Default position: center-top like screenshot
        setX(200); setY(35);
        setWidth(W); setHeight(H);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!initialized) {
            initialized = true;
            new Thread(this::initSpotify, "SpotifyHud-Init").start();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (spotifyAPI != null) {
            try { spotifyAPI.stop(); } catch (Exception ignored) {}
        }
    }

    private void initSpotify() {
        try {
            spotifyAPI = SpotifyAPIFactory.create();
            spotifyAPI.registerListener(new SpotifyListener() {
                public void onConnect()                     { synchronized(lock) { connected = true; } }
                public void onTrackChanged(Track t)        { synchronized(lock) { currentTrack = t; lastUpdate = System.currentTimeMillis(); } }
                public void onPositionChanged(int p)       { synchronized(lock) { currentPosition = p; lastUpdate = System.currentTimeMillis(); } }
                public void onPlayBackChanged(boolean p)   { synchronized(lock) { isPlaying = p; } }
                public void onSync()                       {}
                public void onDisconnect(Exception e)      { synchronized(lock) { connected = false; } }
            });
            spotifyAPI.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isToggled() || mc.player == null) return;
        Renderer2D r = event.renderer();

        String trackName, artistName;
        int duration, position;

        synchronized (lock) {
            if (currentTrack != null) {
                trackName  = currentTrack.getName();
                artistName = currentTrack.getArtist();
                duration   = currentTrack.getLength() / 1000;
                position   = currentPosition / 1000;
                if (isPlaying)
                    position = Math.min(position + (int)((System.currentTimeMillis() - lastUpdate) / 1000L), duration);
            } else {
                trackName  = connected ? "Not Playing" : "Open Spotify";
                artistName = "";
                duration   = 0;
                position   = 0;
            }
        }

        float x = getX(), y = getY();
        setWidth(W); setHeight(H);

        // ── Background ────────────────────────────────────────────────────
        r.rect(x, y, W, H, 6f, new Color(10, 8, 18, 220).getRGB());
        r.rectOutline(x, y, W, H, 6f, new Color(255,255,255,12).getRGB(), 1f);

        // ── Accent top bar ────────────────────────────────────────────────
        Color acc = HUD.getAnimColor(0);
        r.gradient(x, y, W, 2f, 3f,
            acc.getRGB(), acc.getRGB(), acc.darker().getRGB(), acc.darker().getRGB());

        // ── Time top-center (like screenshot "11:35") ─────────────────────
        String timeStr = getTimeStr();
        r.text(FontRegistry.INTER_MEDIUM, x + W/2f, y + 7f, 7f,
            timeStr, new Color(180,180,200).getRGB(), "c");

        // ── Album art placeholder (left) ──────────────────────────────────
        float artX = x + PAD, artY = y + 12f;
        r.rect(artX, artY, ART_S, ART_S, 4f, new Color(30,20,45,220).getRGB());
        r.rectOutline(artX, artY, ART_S, ART_S, 4f, new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),60).getRGB(), 1f);
        // Music note icon
        r.text(FontRegistry.INTER_SEMIBOLD, artX + ART_S/2f, artY + ART_S/2f + 3f, 14f,
            "♫", new Color(acc.getRed(),acc.getGreen(),acc.getBlue(),200).getRGB(), "c");

        // ── Track info (right of art) ─────────────────────────────────────
        float tx = artX + ART_S + PAD;
        float availW = W - tx - PAD;

        // Track name (white, bold)
        String truncName = trunc(trackName, availW, FontRegistry.INTER_SEMIBOLD, 8.5f, r);
        r.text(FontRegistry.INTER_SEMIBOLD, tx, artY + 7f, 8.5f,
            truncName, new Color(230,230,240).getRGB(), "l");

        // Artist (grey)
        String truncArtist = trunc(artistName, availW, FontRegistry.INTER_MEDIUM, 7.5f, r);
        r.text(FontRegistry.INTER_MEDIUM, tx, artY + 18f, 7.5f,
            truncArtist, new Color(140,140,160).getRGB(), "l");

        // ── Progress bar ──────────────────────────────────────────────────
        if (duration > 0) {
            float bx = tx, by = artY + 30f, bw = availW, bh = 3f;

            // Track bg
            r.rect(bx, by, bw, bh, 1.5f, new Color(40,35,55,200).getRGB());

            // Progress fill (green like screenshot)
            float pct = Math.min(1f, (float) position / duration);
            if (pct > 0)
                r.rect(bx, by, bw * pct, bh, 1.5f, new Color(30,200,80).getRGB());

            // Times: "1:03" left, "2:11" right
            String posStr = fmt(position);
            String durStr = fmt(duration);
            r.text(FontRegistry.INTER_MEDIUM, bx,           by + 8f, 7f, posStr, new Color(100,200,100).getRGB(), "l");
            r.text(FontRegistry.INTER_MEDIUM, bx + bw,      by + 8f, 7f, durStr, new Color(120,120,140).getRGB(), "r");
        } else {
            // Not playing state
            r.text(FontRegistry.INTER_MEDIUM, tx, artY + 30f, 7f,
                connected ? "Paused" : "Not connected",
                new Color(80,80,100).getRGB(), "l");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private String trunc(String s, float maxW, foure.dev.util.render.text.FontObject fo, float size, Renderer2D r) {
        if (s == null || s.isEmpty()) return "";
        while (s.length() > 2 && r.getStringWidth(fo, s, size) > maxW) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private String fmt(int secs) {
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    private String getTimeStr() {
        java.time.LocalTime t = java.time.LocalTime.now();
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }

    public static void notify(String msg) {
        foure.dev.ui.notification.NotificationManager.add("Spotify", msg,
            foure.dev.ui.notification.NotificationType.INFO);
    }
}
