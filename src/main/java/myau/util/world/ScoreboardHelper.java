package myau.util.world;

import java.util.ArrayList;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public final class ScoreboardHelper {
   private static final Minecraft minecraft = Minecraft.func_71410_x();

   private ScoreboardHelper() {
   }

   public static ArrayList<String> sidebarLines() {
      if (minecraft.field_71441_e == null) {
         return new ArrayList<>();
      } else {
         Scoreboard scoreboard = minecraft.field_71441_e.func_96441_U();
         ScoreObjective objective = scoreboard.func_96539_a(1);
         return objective == null
            ? new ArrayList<>()
            : scoreboard.func_96534_i(objective)
               .stream()
               .map(score -> formatScoreboardLine(scoreboard, score))
               .collect(Collectors.toCollection(ArrayList::new));
      }
   }

   public static boolean isHypixelScoreboard() {
      ArrayList<String> lines = sidebarLines();
      if (lines.isEmpty()) {
         return false;
      } else {
         String firstLine = lines.get(0);
         return firstLine.equals("§ewww.hypixel.ne\ud83c\udf82§et") || firstLine.equals("§ewww.hypixel.ne§g§et");
      }
   }

   public static boolean hasLobbyPlayerCount() {
      return sidebarLines().stream().anyMatch(line -> line.matches(".*Players: §a\\d+/\\d+.*"));
   }

   private static String formatScoreboardLine(Scoreboard scoreboard, Score score) {
      return ScorePlayerTeam.func_96667_a(scoreboard.func_96509_i(score.func_96653_e()), score.func_96653_e());
   }
}
