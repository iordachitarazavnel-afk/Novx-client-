package foure.dev.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ── Camera ────────────────────────────────────────────────────────────

    public static Camera getCamera() {
        return mc.getEntityRenderDispatcher().camera;
    }

    // ── Filled box ────────────────────────────────────────────────────────

    public static void renderFilledBox(MatrixStack matrices,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       Color color) {
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        VertexConsumerProvider.Immediate provider =
            mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buf = provider.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // bottom
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        // top
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        // north
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        // south
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        // west
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        // east
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);

        provider.draw(RenderLayer.getDebugQuads());
    }

    // ── Box outline ───────────────────────────────────────────────────────

    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        VertexConsumerProvider.Immediate provider =
            mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buf = provider.getBuffer(RenderLayer.getDebugLineStrip(1.5));
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // bottom loop
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        // up west edge + top loop
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);

        provider.draw(RenderLayer.getDebugLineStrip(1.5));

        // remaining vertical edges
        VertexConsumer buf2 = provider.getBuffer(RenderLayer.getDebugLineStrip(1.5));
        buf2.vertex(mat, x2, y1, z1).color(r, g, b, a);
        buf2.vertex(mat, x2, y2, z1).color(r, g, b, a);
        provider.draw(RenderLayer.getDebugLineStrip(1.5));

        VertexConsumer buf3 = provider.getBuffer(RenderLayer.getDebugLineStrip(1.5));
        buf3.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf3.vertex(mat, x2, y2, z2).color(r, g, b, a);
        provider.draw(RenderLayer.getDebugLineStrip(1.5));

        VertexConsumer buf4 = provider.getBuffer(RenderLayer.getDebugLineStrip(1.5));
        buf4.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf4.vertex(mat, x1, y2, z2).color(r, g, b, a);
        provider.draw(RenderLayer.getDebugLineStrip(1.5));
    }

    // ── Single line ───────────────────────────────────────────────────────

    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();

        VertexConsumerProvider.Immediate provider =
            mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer buf = provider.getBuffer(RenderLayer.getDebugLineStrip(1.5));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        provider.draw(RenderLayer.getDebugLineStrip(1.5));
    }

    // ── Box overloads ─────────────────────────────────────────────────────

    public static void renderFilledBox(MatrixStack matrices, Box box, Color color) {
        renderFilledBox(matrices,
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ, color);
    }

    public static void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        renderBoxOutline(matrices,
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ, color);
    }
}
