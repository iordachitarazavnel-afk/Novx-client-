package foure.dev.util.spotify;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.util.Identifier;

public class SpotifyManager {
   private static final SpotifyManager INSTANCE = new SpotifyManager();
   private String currentTrackName = "Not Playing";
   private String currentArtistName = "Open Spotify";
   private boolean isSpotifyRunning = false;
   private Identifier albumArtTexture = null;
   private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

   private SpotifyManager() {
      this.scheduler.scheduleAtFixedRate(this::updateFromWindowTitle, 0L, 2L, TimeUnit.SECONDS);
   }

   public static SpotifyManager getInstance() {
      return INSTANCE;
   }

   private void updateFromWindowTitle() {
      try {
         String[] foundTitle = new String[]{null};
         String[] foundClass = new String[]{null};
         User32.INSTANCE.EnumWindows((hwnd, data) -> {
            char[] windowText = new char[512];
            User32.INSTANCE.GetWindowText(hwnd, windowText, 512);
            String title = Native.toString(windowText);
            if (title.isEmpty()) {
               return true;
            } else {
               char[] className = new char[512];
               User32.INSTANCE.GetClassName(hwnd, className, 512);
               String clz = Native.toString(className);
               if (clz.contains("Chrome_WidgetWin")) {
                  System.out.println("[SpotifyManager] Found Chrome window: class=" + clz + ", title=" + title);
               }

               boolean isSpotifyClass = clz.contains("Chrome_WidgetWin") || clz.contains("Spotify");
               boolean hasSpotifyInTitle = title.toLowerCase().contains("spotify");
               boolean looksLikeSong = title.contains(" - ") && !title.contains("Google Chrome") && !title.contains("Discord") && !title.contains("VS Code") && !title.contains("Visual Studio") && !title.contains("Microsoft Edge");
               if ((!isSpotifyClass || !hasSpotifyInTitle) && (!isSpotifyClass || !looksLikeSong)) {
                  return true;
               } else {
                  System.out.println("[SpotifyManager] MATCHED Spotify window: class=" + clz + ", title=" + title);
                  foundTitle[0] = title;
                  foundClass[0] = clz;
                  return false;
               }
            }
         }, (Pointer)null);
         if (foundTitle[0] != null) {
            String title = foundTitle[0];
            this.isSpotifyRunning = true;
            System.out.println("[SpotifyManager] Processing title: " + title);
            if (!title.equals("Spotify") && !title.equals("Spotify Free") && !title.equals("Spotify Premium") && !title.equals("Spotify - ")) {
               if (title.contains(" - ")) {
                  String cleanTitle = title;
                  if (title.toLowerCase().startsWith("spotify - ")) {
                     cleanTitle = title.substring("spotify - ".length());
                  } else if (title.toLowerCase().startsWith("spotify: ")) {
                     cleanTitle = title.substring("spotify: ".length());
                  }

                  String[] parts = cleanTitle.split(" - ", 2);
                  if (parts.length >= 2) {
                     this.currentArtistName = parts[0].trim();
                     this.currentTrackName = parts[1].trim();
                     System.out.println("[SpotifyManager] Parsed: Artist=" + this.currentArtistName + ", Track=" + this.currentTrackName);
                  } else {
                     this.currentArtistName = "Unknown";
                     this.currentTrackName = cleanTitle;
                  }
               } else {
                  this.currentTrackName = title.replace("Spotify", "").replace("spotify", "").trim();
                  if (this.currentTrackName.isEmpty()) {
                     this.currentTrackName = "Paused";
                  }

                  this.currentArtistName = "Spotify";
               }
            } else {
               this.currentTrackName = "Paused";
               this.currentArtistName = "No track playing";
            }
         } else {
            this.isSpotifyRunning = false;
            this.currentTrackName = "Not Playing";
            this.currentArtistName = "Open Spotify";
            System.out.println("[SpotifyManager] No Spotify window found");
         }
      } catch (Exception var6) {
         var6.printStackTrace();
      }

   }

   public void updateTexture() {
   }

   public String getTrackName() {
      return this.currentTrackName;
   }

   public String getArtistName() {
      return this.currentArtistName;
   }

   public int getPosition() {
      return 0;
   }

   public int getDuration() {
      return 1;
   }

   public Identifier getAlbumArt() {
      return null;
   }

   public boolean hasAlbumArt() {
      return false;
   }
}
