package foure.dev.mixin.render;

import foure.dev.FourEClient;
import foure.dev.module.impl.donut.FakeScoreboard;
import foure.dev.module.impl.donut.HideScoreboard;
import foure.dev.util.ScoreboardUtils;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({InGameHud.class})
public abstract class InGameHudMixin {
   @Shadow
   @Final
   private MinecraftClient client;

   @Shadow
   private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
      throw new AssertionError();
   }

   private Text parseColorCodes(String text) {
      if (text != null && !text.isEmpty()) {
         MutableText result = Text.literal("");
         Pattern pattern = Pattern.compile("&#([0-9A-Fa-f]{6})|([§&])([0-9a-fk-or])");
         Matcher matcher = pattern.matcher(text);

         int lastEnd = 0;
         int currentColor = 16777215;
         boolean bold = false;
         boolean italic = false;
         boolean underline = false;
         boolean strikethrough = false;

         while (matcher.find()) {
            if (matcher.start() > lastEnd) {
               String segment = text.substring(lastEnd, matcher.start());
               MutableText segmentText = Text.literal(segment);

               final int color = currentColor;
               final boolean isBold = bold;
               final boolean isItalic = italic;
               final boolean isUnderline = underline;
               final boolean isStrikethrough = strikethrough;

               segmentText.styled(style -> {
                  style = style.withColor(color);
                  if (isBold) style = style.withBold(true);
                  if (isItalic) style = style.withItalic(true);
                  if (isUnderline) style = style.withUnderline(true);
                  if (isStrikethrough) style = style.withStrikethrough(true);
                  return style;
               });

               result.append(segmentText);
            }

            if (matcher.group(1) != null) {
               String hexColor = matcher.group(1);
               currentColor = Integer.parseInt(hexColor, 16);

               strikethrough = false;
               underline = false;
               italic = false;
               bold = false;

            } else if (matcher.group(2) != null && matcher.group(3) != null) {
               char c = matcher.group(3).charAt(0);
               Formatting formatting = Formatting.byCode(c);

               if (formatting != null) {
                  if (formatting.isColor()) {
                     Integer color = formatting.getColorValue();
                     currentColor = color != null ? color : 16777215;

                     strikethrough = false;
                     underline = false;
                     italic = false;
                     bold = false;
                  } else {
                     switch (c) {
                        case 'l':
                           bold = true;
                           break;
                        case 'm':
                           strikethrough = true;
                           break;
                        case 'n':
                           underline = true;
                           break;
                        case 'o':
                           italic = true;
                           break;
                        case 'r':
                           currentColor = 16777215;
                           strikethrough = false;
                           underline = false;
                           italic = false;
                           bold = false;
                           break;
                     }
                  }
               }
            }

            lastEnd = matcher.end();
         }

         if (lastEnd < text.length()) {
            String segment = text.substring(lastEnd);
            MutableText segmentText = Text.literal(segment);

            final int color = currentColor;
            final boolean isBold = bold;
            final boolean isItalic = italic;
            final boolean isUnderline = underline;
            final boolean isStrikethrough = strikethrough;

            segmentText.styled(style -> {
               style = style.withColor(color);
               if (isBold) style = style.withBold(true);
               if (isItalic) style = style.withItalic(true);
               if (isUnderline) style = style.withUnderline(true);
               if (isStrikethrough) style = style.withStrikethrough(true);
               return style;
            });

            result.append(segmentText);
         }

         return result;
      }

      return Text.literal("");
   }

   private void renderCustomScoreboard(DrawContext context, Text title, String[] lines) {
      MinecraftClient client = MinecraftClient.getInstance();
      TextRenderer textRenderer = client.textRenderer;
      int screenWidth = context.getScaledWindowWidth();
      int screenHeight = context.getScaledWindowHeight();
      int maxWidth = 0;
      String[] var9 = lines;
      int var10 = lines.length;

      int x;
      for(int var11 = 0; var11 < var10; ++var11) {
         String line = var9[var11];
         Text parsedLine = this.parseColorCodes(line + "3");
         x = textRenderer.getWidth(parsedLine);
         if (x > maxWidth) {
            maxWidth = x;
         }
      }

      int titleWidth = textRenderer.getWidth(title);
      if (titleWidth > maxWidth) {
         maxWidth = titleWidth;
      }

      int lineHeight = 9;
      int boardHeight = lines.length * lineHeight;
      x = screenWidth - maxWidth - 1;
      int y = screenHeight / 2 - (boardHeight + lineHeight) / 2 - lineHeight - 10;
      context.fill(x - 2, y, x + maxWidth, y + lineHeight, 1711276032);
      context.drawText(textRenderer, title, x + maxWidth / 2 - titleWidth / 2, y + 1, 16777215, false);
      context.fill(x - 2, y + lineHeight, x + maxWidth, y + lineHeight + boardHeight, 1342177280);
      int currentY = y + lineHeight;
      String[] var17 = lines;
      int var18 = lines.length;

      for(int var19 = 0; var19 < var18; ++var19) {
         String line = var17[var19];
         Text parsedLine = this.parseColorCodes(line);
         context.drawText(textRenderer, parsedLine, x, currentY, 16777215, false);
         currentY += lineHeight;
      }

   }

   @Inject(
           method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V"},
           at = {@At("HEAD")},
           cancellable = true
   )
   private void onRenderScoreboard(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
      if (FourEClient.getInstance().getFunctionManager() != null) {
         if (((HideScoreboard)FourEClient.getInstance().getFunctionManager().getModule(HideScoreboard.class)).isToggled()) {
            ci.cancel();
         }

         FakeScoreboard module = (FakeScoreboard)FourEClient.getInstance().getFunctionManager().getModule(FakeScoreboard.class);
         if (module != null && module.isToggled()) {
            if (!ci.isCancelled()) {
               ci.cancel();
            }

            Text title = this.parseColorCodes("&#007cf9&lD&#0089f9&lo&#0096f9&ln&#00a3f9&lu&#00b0f9&lt&#00bdf9 &#00b0f9&lS&#00b7f9&lM&#00c6f9&lP");
            String[] var10000 = new String[]{"", null, null, null, null, null, null, null, null, null};
            String var10003 = (Boolean)module.realMoney.getValue() ? ScoreboardUtils.getMoney() : (String)module.money.getValue();
            var10000[1] = "&#00FC00&l$ &fMoney &#00FC00" + var10003;
            var10000[2] = "&#A303F9★ &fShards &#A303F9" + (String)module.shards.getValue();
            var10000[3] = "&#FC0000\ud83d\udde1 &fKills &#FC0000" + (String)module.kills.getValue();
            var10000[4] = "&#F97603☠ &fDeaths &#F97603" + (String)module.deaths.getValue();
            var10003 = (Boolean)module.realKey.getValue() ? ScoreboardUtils.getKeyallTimer() : (String)module.keyAll.getValue();
            var10000[5] = "&#00A4FC⌛ &fKeyall &#00A4FC" + var10003;
            var10000[6] = "&#FCE300⌚ &fPlaytime &#FCE300" + (String)module.playtime.getValue();
            var10000[7] = "&#00A4FC\ud83e\ude93 &fTeam &#00A4FC" + (String)module.team.getValue();
            var10000[8] = "";
            var10003 = ScoreboardUtils.getRegion((Boolean)module.hideRegion.getValue());
            var10000[9] = "&7" + var10003 + " &7(&#00A4FC" + ScoreboardUtils.getPing() + "ms&7)";
            String[] lines = var10000;
            var10000 = new String[]{"", null, null, null, null, null, null, null, null};
            var10003 = (Boolean)module.realMoney.getValue() ? ScoreboardUtils.getMoney() : (String)module.money.getValue();
            var10000[1] = "&#00FC00&l$ &fMoney &#00FC00" + var10003;
            var10000[2] = "&#A303F9★ &fShards &#A303F9" + (String)module.shards.getValue();
            var10000[3] = "&#FC0000\ud83d\udde1 &fKills &#FC0000" + (String)module.kills.getValue();
            var10000[4] = "&#F97603☠ &fDeaths &#F97603" + (String)module.deaths.getValue();
            var10003 = (Boolean)module.realKey.getValue() ? ScoreboardUtils.getKeyallTimer() : (String)module.keyAll.getValue();
            var10000[5] = "&#00A4FC⌛ &fKeyall &#00A4FC" + var10003;
            var10000[6] = "&#FCE300⌚ &fPlaytime &#FCE300" + (String)module.playtime.getValue();
            var10000[7] = "";
            var10003 = ScoreboardUtils.getRegion((Boolean)module.hideRegion.getValue());
            var10000[8] = "&7" + var10003 + " &7(&#00A4FC" + ScoreboardUtils.getPing() + "ms&7)";
            String[] noTeam = var10000;
            if (((String)module.team.getValue()).equals("")) {
               this.renderCustomScoreboard(context, title, noTeam);
            } else {
               this.renderCustomScoreboard(context, title, lines);
            }

         }
      }
   }
}
