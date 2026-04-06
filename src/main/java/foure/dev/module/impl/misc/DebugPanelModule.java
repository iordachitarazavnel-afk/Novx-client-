package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.core.RenderFrameMetrics;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import foure.dev.util.render.text.FontRegistry;
import foure.dev.util.render.utils.Color;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

@ModuleInfo(
   name = "DebugPanel",
   category = Category.MISC
)
public final class DebugPanelModule extends Function {
   private static final double MIN_UPDATE_INTERVAL_SECONDS = 0.2D;
   private static final double MAX_UPDATE_INTERVAL_SECONDS = 3.0D;
   private static final double DEFAULT_UPDATE_INTERVAL_SECONDS = 0.6D;
   private static final double UPDATE_INTERVAL_STEP_SECONDS = 0.1D;
   private static final float PANEL_MARGIN = 12.0F;
   private static final float PANEL_WIDTH = 240.0F;
   private static final float PANEL_PADDING = 12.0F;
   private static final float PANEL_CORNER_RADIUS = 10.0F;
   private static final float PANEL_BORDER_THICKNESS = 1.25F;
   private static final float HEADER_TEXT_SIZE = 15.0F;
   private static final float ENTRY_TEXT_SIZE = 13.0F;
   private static final float ENTRY_SPACING = 4.0F;
   private static final int PANEL_BACKGROUND_COLOR = Color.getRGB(1183760, 0.88D);
   private static final int LABEL_TEXT_COLOR = Color.getRGB(13358561, 0.94D);
   private static final DecimalFormat FPS_FORMAT = decimalFormat("0.0");
   private static final DecimalFormat TIME_FORMAT = decimalFormat("0.00");
   private static final DecimalFormat INTEGER_FORMAT = decimalFormat("intermediary");
   private static final DecimalFormat MEMORY_FORMAT = decimalFormat("0.0");
   private static final String DRAG_HANDLE_ID = "module.debug_panel";
   private AutoCloseable renderSubscription;
   private long lastSampleNanos = 0L;
   private int framesAccumulated = 0;
   private int drawCallsAccumulated = 0;
   private int trianglesAccumulated = 0;
   private double fpsAccumulated = 0.0D;
   private int fpsSamples = 0;
   private double displayedFps = 0.0D;
   private double displayedFrameTimeMs = 0.0D;
   private double displayedDrawCalls = 0.0D;
   private double displayedTriangles = 0.0D;
   private int displayedLatencyMs = -1;
   private double displayedMemoryUsedMb = 0.0D;
   private double displayedMemoryMaxMb = 0.0D;
   private float panelX = 12.0F;
   private float panelY = 12.0F;

   public DebugPanelModule() {
      this.setKey(346);
   }

   public void onEnable() {
      super.onEnable();
      this.resetState();
      this.unsubscribe();
   }

   public void onDisable() {
      super.onDisable();
      this.unsubscribe();
   }

   @Subscribe
   public void handleRenderEvent(RenderEvent event) {
      Objects.requireNonNull(event, "event");
      this.updateMetrics(event);
      this.renderPanel(event);
   }

   private void renderPanel(RenderEvent event) {
      Renderer2D renderer = event.renderer();
      FontObject headerFont = FontRegistry.INTER_SEMIBOLD;
      FontObject entryFont = FontRegistry.INTER_MEDIUM;
      float headerHeight = renderer.measureText(headerFont, "Ag", 15.0F).height();
      float entryHeight = renderer.measureText(entryFont, "Ag", 13.0F).height();
      DebugPanelModule.MetricEntry[] entries = this.buildEntries(event);
      float contentHeight = 0.0F;
      if (entries.length > 0) {
         contentHeight += entryHeight * (float)entries.length;
         contentHeight += 4.0F * (float)(entries.length - 1);
      }

      float panelHeight = 24.0F + contentHeight;
      this.panelX = 25.0F;
      this.panelY = 25.0F;
      float panelX = this.panelX;
      float panelY = this.panelY;
      float innerX = panelX + 1.25F;
      float innerY = panelY + 1.25F;
      float innerWidth = 237.5F;
      float innerHeight = panelHeight - 2.5F;
      float innerRadius = Math.max(0.0F, 8.75F);
      if (innerWidth > 0.0F && innerHeight > 0.0F) {
         renderer.rect(innerX, innerY, innerWidth, innerHeight, innerRadius, PANEL_BACKGROUND_COLOR);
      }

      float textX = panelX + 12.0F;
      float valueX = panelX + 240.0F - 12.0F;
      float entryBaseline = panelY + 12.0F + entryHeight * 0.8F;
      int valueColor = resolveValueTextColor();
      DebugPanelModule.MetricEntry[] var21 = entries;
      int var22 = entries.length;

      for(int var23 = 0; var23 < var22; ++var23) {
         DebugPanelModule.MetricEntry entry = var21[var23];
         renderer.text(entryFont, textX, entryBaseline, 13.0F, entry.label(), LABEL_TEXT_COLOR, "l");
         renderer.text(headerFont, valueX, entryBaseline, 13.0F, entry.value(), valueColor, "r");
         entryBaseline += entryHeight + 4.0F;
      }

   }

   private DebugPanelModule.MetricEntry[] buildEntries(RenderEvent event) {
      String fpsValue = FPS_FORMAT.format(Math.max(0.0D, this.displayedFps));
      DecimalFormat var10000 = TIME_FORMAT;
      String frameTimeValue = var10000.format(Math.max(0.0D, this.displayedFrameTimeMs)) + " ms";
      String drawCallValue = FPS_FORMAT.format(Math.max(0.0D, this.displayedDrawCalls));
      String triangleValue = INTEGER_FORMAT.format(Math.max(0.0D, (double)Math.round(this.displayedTriangles)));
      String latencyValue = this.displayedLatencyMs >= 0 ? this.displayedLatencyMs + " ms" : "N/A";
      String memoryValue = MEMORY_FORMAT.format(Math.max(0.0D, this.displayedMemoryUsedMb)) + " / " + MEMORY_FORMAT.format(Math.max(0.0D, this.displayedMemoryMaxMb)) + " MB";
      int var9 = event.viewportWidth();
      String resolutionValue = var9 + "×" + event.viewportHeight();
      return new DebugPanelModule.MetricEntry[]{new DebugPanelModule.MetricEntry("FPS", fpsValue), new DebugPanelModule.MetricEntry("Frame Time", frameTimeValue), new DebugPanelModule.MetricEntry("Draw Calls", drawCallValue), new DebugPanelModule.MetricEntry("Triangles", triangleValue), new DebugPanelModule.MetricEntry("Latency", latencyValue), new DebugPanelModule.MetricEntry("Memory", memoryValue), new DebugPanelModule.MetricEntry("Resolution", resolutionValue)};
   }

   private void updateMetrics(RenderEvent event) {
      RenderFrameMetrics.FrameMetricsSnapshot snapshot = RenderFrameMetrics.getInstance().snapshot();
      MinecraftClient client = event.client();
      ++this.framesAccumulated;
      this.drawCallsAccumulated += snapshot.drawCalls();
      this.trianglesAccumulated += snapshot.triangles();
      double fpsSample = Math.max(0.0D, (double)client.getCurrentFps());
      this.fpsAccumulated += fpsSample;
      ++this.fpsSamples;
      long now = System.nanoTime();
      if (this.lastSampleNanos == 0L) {
         this.lastSampleNanos = now;
      }

      long updateInterval = this.resolveUpdateIntervalNanos();
      long elapsed = now - this.lastSampleNanos;
      if (elapsed >= updateInterval || this.framesAccumulated <= 0) {
         double averageFps = this.fpsSamples > 0 ? this.fpsAccumulated / (double)this.fpsSamples : fpsSample;
         if (averageFps < 0.0D) {
            averageFps = 0.0D;
         }

         this.displayedFps = averageFps;
         this.displayedFrameTimeMs = averageFps > 0.0D ? 1000.0D / averageFps : 0.0D;
         this.displayedDrawCalls = this.framesAccumulated > 0 ? (double)this.drawCallsAccumulated / (double)this.framesAccumulated : (double)snapshot.drawCalls();
         this.displayedTriangles = this.framesAccumulated > 0 ? (double)this.trianglesAccumulated / (double)this.framesAccumulated : (double)snapshot.triangles();
         this.displayedLatencyMs = this.resolveLatency(client);
         this.updateMemoryStats();
         this.lastSampleNanos = now;
         this.framesAccumulated = 0;
         this.drawCallsAccumulated = 0;
         this.trianglesAccumulated = 0;
         this.fpsAccumulated = 0.0D;
         this.fpsSamples = 0;
      }
   }

   private void updateMemoryStats() {
      Runtime runtime = Runtime.getRuntime();
      long usedBytes = runtime.totalMemory() - runtime.freeMemory();
      long maxBytes = runtime.maxMemory();
      this.displayedMemoryUsedMb = bytesToMegabytes(usedBytes);
      this.displayedMemoryMaxMb = bytesToMegabytes(maxBytes);
   }

   private long resolveUpdateIntervalNanos() {
      double seconds = 1.5D;
      seconds = Math.max(0.2D, Math.min(3.0D, seconds));
      long millis = Math.max(1L, Math.round(seconds * 1000.0D));
      return TimeUnit.MILLISECONDS.toNanos(millis);
   }

   private int resolveLatency(MinecraftClient client) {
      if (client != null && client.player != null) {
         ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
         if (networkHandler == null) {
            return -1;
         } else {
            PlayerListEntry entry = networkHandler.getPlayerListEntry(client.player.getUuid());
            return entry == null ? -1 : entry.getLatency();
         }
      } else {
         return -1;
      }
   }

   private void resetState() {
      this.lastSampleNanos = 0L;
      this.framesAccumulated = 0;
      this.drawCallsAccumulated = 0;
      this.trianglesAccumulated = 0;
      this.displayedLatencyMs = -1;
      this.displayedFps = 0.0D;
      this.displayedFrameTimeMs = 0.0D;
      this.displayedDrawCalls = 0.0D;
      this.displayedTriangles = 0.0D;
      this.fpsAccumulated = 0.0D;
      this.fpsSamples = 0;
      this.updateMemoryStats();
      this.panelX = 12.0F;
      this.panelY = 12.0F;
   }

   private void unsubscribe() {
      if (this.renderSubscription != null) {
         try {
            this.renderSubscription.close();
         } catch (Exception var2) {
         }

         this.renderSubscription = null;
      }
   }

   private static int resolveValueTextColor() {
      return java.awt.Color.ORANGE.getRGB();
   }

   private static DecimalFormat decimalFormat(String pattern) {
      NumberFormat base = NumberFormat.getNumberInstance(Locale.US);
      if (base instanceof DecimalFormat) {
         DecimalFormat format = (DecimalFormat)base;
         format.setRoundingMode(RoundingMode.HALF_UP);
         format.applyPattern(pattern);
         return format;
      } else {
         DecimalFormat fallback = new DecimalFormat(pattern);
         fallback.setRoundingMode(RoundingMode.HALF_UP);
         return fallback;
      }
   }

   private static double bytesToMegabytes(long bytes) {
      return (double)bytes / 1048576.0D;
   }

   private static record MetricEntry(String label, String value) {
      private MetricEntry(String label, String value) {
         this.label = label;
         this.value = value;
      }

      public String label() {
         return this.label;
      }

      public String value() {
         return this.value;
      }
   }
}
