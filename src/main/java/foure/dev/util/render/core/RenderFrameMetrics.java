package foure.dev.util.render.core;

public final class RenderFrameMetrics {
   private static final RenderFrameMetrics INSTANCE = new RenderFrameMetrics();
   private final Object lock = new Object();
   private long frameStartNanos = 0L;
   private long lastFrameDurationNanos = 0L;
   private int currentDrawCalls = 0;
   private int currentTriangles = 0;
   private int lastFrameDrawCalls = 0;
   private int lastFrameTriangles = 0;

   private RenderFrameMetrics() {
   }

   public static RenderFrameMetrics getInstance() {
      return INSTANCE;
   }

   public void beginFrame(int width, int height) {
      if (width > 0 && height > 0) {
         long now = System.nanoTime();
         synchronized(this.lock) {
            this.frameStartNanos = now;
            this.currentDrawCalls = 0;
            this.currentTriangles = 0;
         }
      }
   }

   public void recordDrawCall(int triangleCount) {
      if (triangleCount < 0) {
         triangleCount = 0;
      }

      synchronized(this.lock) {
         ++this.currentDrawCalls;
         this.currentTriangles += triangleCount;
      }
   }

   public void endFrame() {
      long now = System.nanoTime();
      synchronized(this.lock) {
         if (this.frameStartNanos <= 0L) {
            this.frameStartNanos = now;
            this.lastFrameDurationNanos = 0L;
            this.lastFrameDrawCalls = this.currentDrawCalls;
            this.lastFrameTriangles = this.currentTriangles;
            this.currentDrawCalls = 0;
            this.currentTriangles = 0;
         } else {
            this.lastFrameDurationNanos = Math.max(0L, now - this.frameStartNanos);
            this.lastFrameDrawCalls = this.currentDrawCalls;
            this.lastFrameTriangles = this.currentTriangles;
            this.frameStartNanos = now;
            this.currentDrawCalls = 0;
            this.currentTriangles = 0;
         }
      }
   }

   public RenderFrameMetrics.FrameMetricsSnapshot snapshot() {
      synchronized(this.lock) {
         return new RenderFrameMetrics.FrameMetricsSnapshot(this.lastFrameDurationNanos, this.lastFrameDrawCalls, this.lastFrameTriangles);
      }
   }

   public static record FrameMetricsSnapshot(long frameDurationNanos, int drawCalls, int triangles) {
      public FrameMetricsSnapshot(long frameDurationNanos, int drawCalls, int triangles) {
         frameDurationNanos = Math.max(0L, frameDurationNanos);
         drawCalls = Math.max(0, drawCalls);
         triangles = Math.max(0, triangles);
         this.frameDurationNanos = frameDurationNanos;
         this.drawCalls = drawCalls;
         this.triangles = triangles;
      }

      public double frameTimeMillis() {
         return (double)this.frameDurationNanos / 1000000.0D;
      }

      public double framesPerSecond() {
         return this.frameDurationNanos > 0L ? 1.0E9D / (double)this.frameDurationNanos : 0.0D;
      }

      public long frameDurationNanos() {
         return this.frameDurationNanos;
      }

      public int drawCalls() {
         return this.drawCalls;
      }

      public int triangles() {
         return this.triangles;
      }
   }
}
