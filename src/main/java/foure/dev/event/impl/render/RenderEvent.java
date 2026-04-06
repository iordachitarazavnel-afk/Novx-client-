package foure.dev.event.impl.render;

import foure.dev.event.api.Event;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontObject;
import java.util.Objects;
import net.minecraft.client.MinecraftClient;

public final class RenderEvent extends Event {
   private final MinecraftClient client;
   private final Renderer2D renderer;
   private final FontObject defaultFont;
   private final int viewportWidth;
   private final int viewportHeight;
   private final float scaledWidth;
   private final float scaledHeight;

   public RenderEvent(MinecraftClient client, Renderer2D renderer, FontObject defaultFont, int viewportWidth, int viewportHeight, float scaledWidth, float scaledHeight) {
      this.client = (MinecraftClient)Objects.requireNonNull(client, "client");
      this.renderer = (Renderer2D)Objects.requireNonNull(renderer, "renderer");
      this.defaultFont = (FontObject)Objects.requireNonNull(defaultFont, "defaultFont");
      this.viewportWidth = viewportWidth;
      this.viewportHeight = viewportHeight;
      this.scaledWidth = scaledWidth;
      this.scaledHeight = scaledHeight;
   }

   public MinecraftClient client() {
      return this.client;
   }

   public Renderer2D renderer() {
      return this.renderer;
   }

   public FontObject defaultFont() {
      return this.defaultFont;
   }

   public int viewportWidth() {
      return this.viewportWidth;
   }

   public int viewportHeight() {
      return this.viewportHeight;
   }

   public float scaledWidth() {
      return this.scaledWidth;
   }

   public float scaledHeight() {
      return this.scaledHeight;
   }
}
