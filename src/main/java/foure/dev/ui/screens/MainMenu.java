package foure.dev.ui.screens;

import java.awt.Color;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class MainMenu extends Screen {
   public static MainMenu INSTANCE = new MainMenu();
   protected final MinecraftClient mc = MinecraftClient.getInstance();
   private int width;
   private int height;
   private final MainMenu.Decelerate mainFadeAnimation = (new MainMenu.Decelerate()).setMs(500L);
   private boolean lockScreenVisible = true;
   private int hoveredIndex = -1;
   private boolean wasMouseDown = false;
   private boolean wasSpaceDown = false;
   private boolean wasEscDown = false;

   public MainMenu() {
      super(Text.of("MainMenu"));
      this.mainFadeAnimation.setDirection(true);
   }

   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      this.width = context.getScaledWindowWidth();
      this.height = context.getScaledWindowHeight();
      int darkPurple = (new Color(20, 0, 50, 255)).getRGB();
      context.fill(0, 0, this.width, this.height, darkPurple);
      double mainAlpha = this.mainFadeAnimation.getOutput();
      int mainAlphaInt = (int)(255.0D * mainAlpha);
      LocalTime currentTime = LocalTime.now();
      String timeString = String.format("%02d:%02d", currentTime.getHour(), currentTime.getMinute());
      LocalDateTime now = LocalDateTime.now();
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM");
      String dateString = now.format(dateFormatter);
      if (this.lockScreenVisible) {
         context.drawCenteredTextWithShadow(this.mc.textRenderer, timeString, this.width / 2, this.height / 2 - 40, -1);
         context.drawCenteredTextWithShadow(this.mc.textRenderer, dateString, this.width / 2, this.height / 2 + 10, 13421772);
         context.drawCenteredTextWithShadow(this.mc.textRenderer, "Press SPACE to unlock", this.width / 2, this.height - 30, 11184810);
      } else {
         if (mainAlpha > 0.009999999776482582D) {
            context.getMatrices().translate((float)this.width / 2.0F, (float)this.height / 2.0F - 200.0F);
            context.getMatrices().scale(8.0F, 8.0F);
            context.drawCenteredTextWithShadow(this.mc.textRenderer, "4E", 0, 0, this.applyAlpha(10494192, mainAlphaInt));
            context.getMatrices().scale(0.125F, 0.125F);
            context.getMatrices().translate(-((float)this.width / 2.0F), -((float)this.height / 2.0F - 200.0F));
            context.getMatrices().translate((float)this.width / 2.0F, (float)this.height / 2.0F - 100.0F);
            context.getMatrices().translate((float)this.width / 2.0F, (float)this.height / 2.0F - 100.0F);
            context.getMatrices().scale(4.0F, 4.0F);
            context.drawCenteredTextWithShadow(this.mc.textRenderer, timeString, 0, 0, this.applyAlpha(16777215, mainAlphaInt));
            context.getMatrices().scale(0.25F, 0.25F);
            context.getMatrices().translate(-((float)this.width / 2.0F), -((float)this.height / 2.0F - 100.0F));
            context.drawCenteredTextWithShadow(this.mc.textRenderer, dateString, this.width / 2, this.height / 2 - 10, this.applyAlpha(13421772, mainAlphaInt));
            float btnY = (float)this.height / 2.0F + 40.0F;
            float btnW = 50.0F;
            float btnH = 50.0F;
            float btn1X = (float)this.width / 2.0F - 135.0F;
            float btn2X = (float)this.width / 2.0F - 75.0F;
            float btn3X = (float)this.width / 2.0F - 15.0F;
            float btn4X = (float)this.width / 2.0F + 45.0F;
            this.hoveredIndex = -1;
            if (this.isIn((double)mouseX, (double)mouseY, btn1X, btnY, btnW, btnH)) {
               this.hoveredIndex = 0;
            } else if (this.isIn((double)mouseX, (double)mouseY, btn2X, btnY, btnW, btnH)) {
               this.hoveredIndex = 1;
            } else if (this.isIn((double)mouseX, (double)mouseY, btn3X, btnY, btnW, btnH)) {
               this.hoveredIndex = 2;
            } else if (this.isIn((double)mouseX, (double)mouseY, btn4X, btnY, btnW, btnH)) {
               this.hoveredIndex = 3;
            }

            this.drawButton(context, btn1X, btnY, btnW, btnH, "Single", this.hoveredIndex == 0, mainAlphaInt);
            this.drawButton(context, btn2X, btnY, btnW, btnH, "Multi", this.hoveredIndex == 1, mainAlphaInt);
            this.drawButton(context, btn3X, btnY, btnW, btnH, "Alt", this.hoveredIndex == 2, mainAlphaInt);
            this.drawButton(context, btn4X, btnY, btnW, btnH, "Opts", this.hoveredIndex == 3, mainAlphaInt);
            float exitX = (float)this.width / 2.0F - 75.0F;
            float exitY = (float)this.height - 30.0F;
            float exitW = 150.0F;
            float exitH = 20.0F;
            boolean exitHover = this.isIn((double)mouseX, (double)mouseY, exitX, exitY, exitW, exitH);
            context.fill((int)exitX, (int)exitY, (int)(exitX + exitW), (int)(exitY + exitH), exitHover ? -2130771968 : 1090453504);
            context.drawCenteredTextWithShadow(this.mc.textRenderer, "Exit Game", this.width / 2, (int)exitY + 6, -1);
            this.handleInputManual();
         }

      }
   }

   private void handleInputManual() {
      long window = this.mc.getWindow().getHandle();
      boolean mouseDown = GLFW.glfwGetMouseButton(window, 0) == 1;
      if (mouseDown && !this.wasMouseDown) {
         this.onMouseClickStub();
      }

      this.wasMouseDown = mouseDown;
      boolean spaceDown = GLFW.glfwGetKey(window, 32) == 1;
      if (spaceDown && !this.wasSpaceDown && this.lockScreenVisible) {
         this.lockScreenVisible = false;
      }

      this.wasSpaceDown = spaceDown;
      boolean escDown = GLFW.glfwGetKey(window, 256) == 1;
      if (escDown && !this.wasEscDown && !this.lockScreenVisible) {
         this.lockScreenVisible = true;
      }

      this.wasEscDown = escDown;
   }

   private void onMouseClickStub() {
      double mx = this.mc.mouse.getX() * (double)this.width / (double)this.mc.getWindow().getWidth();
      double my = this.mc.mouse.getY() * (double)this.height / (double)this.mc.getWindow().getHeight();
      if (!this.lockScreenVisible) {
         float btnY = (float)this.height / 2.0F + 40.0F;
         float btnW = 50.0F;
         float btnH = 50.0F;
         float btn1X = (float)this.width / 2.0F - 135.0F;
         float btn2X = (float)this.width / 2.0F - 75.0F;
         float btn3X = (float)this.width / 2.0F - 15.0F;
         float btn4X = (float)this.width / 2.0F + 45.0F;
         if (this.isIn(mx, my, btn1X, btnY, btnW, btnH)) {
            this.mc.setScreen(new SelectWorldScreen(this));
         } else if (this.isIn(mx, my, btn2X, btnY, btnW, btnH)) {
            this.mc.setScreen(new MultiplayerScreen(this));
         } else if (this.isIn(mx, my, btn4X, btnY, btnW, btnH)) {
            this.mc.setScreen(new OptionsScreen(this, this.mc.options));
         }

         float exitX = (float)this.width / 2.0F - 75.0F;
         float exitY = (float)this.height - 30.0F;
         float exitW = 150.0F;
         float exitH = 20.0F;
         if (this.isIn(mx, my, exitX, exitY, exitW, exitH)) {
            this.mc.stop();
         }
      }

   }

   private void drawButton(DrawContext context, float x, float y, float w, float h, String text, boolean hovered, int alpha) {
      int color = hovered ? -2130706433 : 1073741824;
      int outline = -1;
      context.fill((int)x, (int)y, (int)(x + w), (int)(y + h), this.applyAlpha(color, alpha));
      int textColor;
      if (hovered) {
         textColor = this.applyAlpha(outline, alpha);
         context.fill((int)x, (int)y, (int)(x + w), (int)y + 1, textColor);
         context.fill((int)x, (int)(y + h - 1.0F), (int)(x + w), (int)(y + h), textColor);
         context.fill((int)x, (int)y, (int)x + 1, (int)(y + h), textColor);
         context.fill((int)(x + w - 1.0F), (int)y, (int)(x + w), (int)(y + h), textColor);
      }

      textColor = this.applyAlpha(-1, alpha);
      context.drawCenteredTextWithShadow(this.mc.textRenderer, text, (int)(x + w / 2.0F), (int)(y + h / 2.0F - 4.0F), textColor);
   }

   private int applyAlpha(int color, int alpha) {
      int r = color >> 16 & 255;
      int g = color >> 8 & 255;
      int b = color & 255;
      int a = color >> 24 & 255;
      if (a == 0) {
         a = 255;
      }

      int newAlpha = (int)((double)a / 255.0D * (double)alpha);
      return newAlpha << 24 | r << 16 | g << 8 | b;
   }

   private boolean isIn(double mx, double my, float x, float y, float w, float h) {
      return mx >= (double)x && mx <= (double)(x + w) && my >= (double)y && my <= (double)(y + h);
   }

   public static class Decelerate {
      private float value = 0.0F;
      private long ms = 250L;
      private long startTime = 0L;
      private boolean direction = true;

      public MainMenu.Decelerate setMs(long ms) {
         this.ms = ms;
         return this;
      }

      public MainMenu.Decelerate setValue(double v) {
         this.value = (float)v;
         return this;
      }

      public void setDirection(boolean dir) {
         if (this.direction != dir) {
            this.direction = dir;
            this.startTime = System.currentTimeMillis();
         }

      }

      public double getOutput() {
         long now = System.currentTimeMillis();
         long elapsed = now - this.startTime;
         float progress = Math.min(1.0F, (float)elapsed / (float)this.ms);
         float ease = 1.0F - (1.0F - progress) * (1.0F - progress);
         if (this.direction) {
            return progress >= 1.0F ? 1.0D : (double)ease;
         } else {
            return progress >= 1.0F ? 0.0D : (double)(1.0F - ease);
         }
      }
   }
}
