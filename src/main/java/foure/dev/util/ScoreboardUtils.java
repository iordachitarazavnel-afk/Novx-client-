package foure.dev.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

public class ScoreboardUtils {
   public static String getRawScoreboard() {
      try {
         MinecraftClient mc = MinecraftClient.getInstance();
         if (mc != null && mc.player != null && mc.world != null && mc.world.getScoreboard() != null) {
            Scoreboard scoreboard = mc.world.getScoreboard();
            if (scoreboard == null) {
               return "";
            } else {
               ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
               if (objective == null) {
                  return "";
               } else {
                  StringBuilder result = new StringBuilder();
                  Collection<ScoreboardEntry> entries = scoreboard.getScoreboardEntries(objective);
                  if (entries == null) {
                     return "";
                  } else {
                     Iterator var5 = entries.iterator();

                     while(true) {
                        String name;
                        do {
                           ScoreboardEntry entry;
                           do {
                              if (!var5.hasNext()) {
                                 return result.toString();
                              }

                              entry = (ScoreboardEntry)var5.next();
                           } while(entry == null);

                           name = entry.owner();
                        } while(name == null);

                        try {
                           Team team = scoreboard.getScoreHolderTeam(name);
                           if (team != null) {
                              Text prefix = team.getPrefix();
                              Text suffix = team.getSuffix();
                              if (prefix != null && suffix != null) {
                                 result.append(prefix.getString()).append(name).append(suffix.getString()).append("\n");
                              } else {
                                 result.append(name).append("\n");
                              }
                           } else {
                              result.append(name).append("\n");
                           }
                        } catch (Exception var11) {
                           result.append(name).append("\n");
                        }
                     }
                  }
               }
            }
         } else {
            return "";
         }
      } catch (Exception var12) {
         return "";
      }
   }

   public static String getPing() {
      String scoreboard = getRawScoreboard();
      Pattern pattern = Pattern.compile("\\((\\d+)ms\\)");
      Matcher matcher = pattern.matcher(scoreboard);
      return matcher.find() ? matcher.group(1) : String.valueOf(PingUtils.getCachedPing());
   }

   public static String getMoney() {
      String scoreboard = getRawScoreboard();
      if (scoreboard.isEmpty()) {
         return "intermediary";
      } else {
         Pattern pattern = Pattern.compile("(?:\\$|Money|Balance)\\s*:?\\s*\\$?\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.\\d+)?[KMB]?)", 2);
         Matcher matcher = pattern.matcher(scoreboard);
         return matcher.find() ? matcher.group(1).replace(",", "") : "intermediary";
      }
   }

   public static String getKeyallTimer() {
      String scoreboard = getRawScoreboard();
      Pattern pattern = Pattern.compile("Keyall\\s+([0-9]+[msh]\\s*)+", 2);
      Matcher matcher = pattern.matcher(scoreboard);
      if (matcher.find()) {
         String fullMatch = matcher.group(0);
         return fullMatch.replaceFirst("(?i)Keyall\\s+", "").trim();
      } else {
         return "";
      }
   }

   public static String getRegion(boolean replace) {
      String scoreboard = getRawScoreboard();
      scoreboard = scoreboard.replaceAll("§.", "");
      Pattern pattern = Pattern.compile("([A-Za-z][A-Za-z\\s]+)\\s*\\(");
      Matcher matcher = pattern.matcher(scoreboard);
      if (matcher.find()) {
         String region = matcher.group(1).trim();
         if (!region.isEmpty()) {
            return replace ? "novx Client" : region;
         }
      }

      return "novx Client";
   }
}
