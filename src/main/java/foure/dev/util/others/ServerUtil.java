package foure.dev.util.others;

import foure.dev.event.impl.game.PacketEvent;
import foure.dev.mixin.accessor.BossBarHudAccessor;
import foure.dev.util.Player.PlayerIntersectionUtil;
import foure.dev.util.math.TimerUtil;
import foure.dev.util.wrapper.Wrapper;
import java.util.Iterator;
import java.util.Objects;
import lombok.Generated;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

public final class ServerUtil implements Wrapper {
   private static final TimerUtil pvpWatch = new TimerUtil();
   public static String server = "Vanilla";
   public static float TPS = 20.0F;
   public static long timestamp;
   public static int anarchy;
   public static boolean pvpEnd;

   public static void tick() {
      anarchy = getAnarchyMode();
      server = getServer();
      pvpEnd = inPvpEnd();
      if (inPvp()) {
         pvpWatch.reset();
      }

   }

   public static void onTick(PacketEvent e) {
      Packet<?> packet = e.getPacket();

      if (packet instanceof WorldTimeUpdateS2CPacket time) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         float rawTPS = maxTPS * (1.0E9F / (float)(nanoTime - timestamp));

         TPS = MathHelper.clamp(rawTPS, 0.0F, maxTPS);
         timestamp = nanoTime;
      }
   }

   private static String getServer() {
      if (!PlayerIntersectionUtil.nullCheck() && mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getBrand() != null) {
         String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
         String brand = mc.getNetworkHandler().getBrand().toLowerCase();
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (brand.contains("§6spooky§ccore")) {
            return "SpookyTime";
         } else if (!serverIp.contains("funtime") && !serverIp.contains("skytime") && !serverIp.contains("space-times") && !serverIp.contains("funsky")) {
            if (!brand.contains("holyworld") && !brand.contains("vk.com/idwok")) {
               if (serverIp.contains("reallyworld")) {
                  return "ReallyWorld";
               } else {
                  return serverIp.contains("LonyGrief") ? "LonyGrie" : "Vanilla";
               }
            } else {
               return "HolyWorld";
            }
         } else {
            return "CopyTime";
         }
      } else {
         return "Vanilla";
      }
   }

   private static int getAnarchyMode() {
      if (mc.player != null && mc.world != null) {
         Scoreboard scoreboard = mc.world.getScoreboard();
         ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         String var2 = server;
         byte var3 = -1;
         switch(var2.hashCode()) {
         case -495240450:
            if (var2.equals("HolyWorld")) {
               var3 = 1;
            }
            break;
         case 1154553036:
            if (var2.equals("FunTime")) {
               var3 = 0;
            }
         }

         switch(var3) {
         case 0:
            if (objective != null) {
               String[] string = objective.getDisplayName().getString().split("-");
               if (string.length > 1) {
                  return Integer.parseInt(string[1]);
               }
            }
            break;
         case 1:
            Iterator var4 = scoreboard.getScoreboardEntries(objective).iterator();

            while(var4.hasNext()) {
               ScoreboardEntry scoreboardEntry = (ScoreboardEntry)var4.next();
               String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
               if (!text.isEmpty()) {
                  String string = StringUtils.substringBetween(text, "#", " -◆-");
                  if (string != null && !string.isEmpty()) {
                     return Integer.parseInt(string.replace(" (1.20)", ""));
                  }
               }
            }
         }

         return -1;
      } else {
         return 0;
      }
   }

   public static boolean isPvp() {
      return !pvpWatch.finished(500.0D);
   }

   public static boolean inPvp() {
      return ((BossBarHudAccessor)mc.inGameHud.getBossBarHud()).getBossBars().values().stream().map((c) -> {
         return c.getName().getString().toLowerCase();
      }).anyMatch((s) -> {
         return s.contains("pvp") || s.contains("пвп") || s.contains("PvP");
      });
   }

   private static boolean inPvpEnd() {
      return ((BossBarHudAccessor)mc.inGameHud.getBossBarHud()).getBossBars().values().stream().map((c) -> {
         return c.getName().getString().toLowerCase();
      }).anyMatch((s) -> {
         return (s.contains("pvp") || s.contains("пвп")) && (s.contains("intermediary") || s.contains("1"));
      });
   }

   public static String getWorldType() {
      return mc.world.getRegistryKey().getValue().getPath();
   }

   public static boolean isCopyTime() {
      return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");
   }

   public static boolean isFunTime() {
      return server.equals("FunTime");
   }

   public static boolean isReallyWorld() {
      return server.equals("ReallyWorld");
   }

   public static boolean isHolyWorld() {
      return server.equals("HolyWorld");
   }

   public static boolean isVanilla() {
      return server.equals("Vanilla");
   }

   @Generated
   private ServerUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   @Generated
   public static int getAnarchy() {
      return anarchy;
   }

   @Generated
   public static boolean isPvpEnd() {
      return pvpEnd;
   }
}
