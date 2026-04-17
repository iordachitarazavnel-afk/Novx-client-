package foure.dev.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * RenderUtils for MC 1.21.11 / Yarn 1.21.11+build.2
 *
 * API în Yarn 1.21.11:
 *  - BufferBuilder(BufferAllocator, VertexFormat.DrawMode, VertexFormat) în net.minecraft.client.render
 *  - VertexFormat / VertexFormat.DrawMode în com.mojang.blaze3d.vertex
 *  - BuiltBuffer (nu MeshData) în net.minecraft.client.render
 *  - BufferRenderer.drawWithGlobalProgram(BuiltBuffer) în net.minecraft.client.render
 *  - VertexFormats în net.minecraft.client.render
 *  - RenderSystem.setShader() pentru a seta shader-ul înainte de draw
 */
public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Camera getCamera() {
        return mc.getEntityRenderDispatcher().camera;
    }

    // ── Filled Box ────────────────────────────────────────────────────────
    public static void renderFilledBox(MatrixStack matrices,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       Color color) {
        int r = color.getRed(), g = color.getGreen(),
            b = color.getBlue(), a = color.getAlpha();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        try (BufferAllocator alloc = new BufferAllocator(4096)) {
            BufferBuilder buf = new BufferBuilder(alloc,
                VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            // front
            buf.vertex(mat,x1,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
            // back
            buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
            // left
            buf.vertex(mat,x1,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
            // right
            buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z2).color(r,g,b,a);
            // top
            buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
            // bottom
            buf.vertex(mat,x1,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z2).color(r,g,b,a);

            BuiltBuffer built = buf.end();
            BufferRenderer.drawWithGlobalProgram(built);
            built.close();
        }
    }

    // ── Box Outline ───────────────────────────────────────────────────────
    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        int r = color.getRed(), g = color.getGreen(),
            b = color.getBlue(), a = color.getAlpha();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        try (BufferAllocator alloc = new BufferAllocator(2048)) {
            BufferBuilder buf = new BufferBuilder(alloc,
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            buf.vertex(mat,x1,y1,z1).color(r,g,b,a); buf.vertex(mat,x2,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z1).color(r,g,b,a); buf.vertex(mat,x2,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z2).color(r,g,b,a); buf.vertex(mat,x1,y1,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z2).color(r,g,b,a); buf.vertex(mat,x1,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z1).color(r,g,b,a); buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z1).color(r,g,b,a); buf.vertex(mat,x2,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z2).color(r,g,b,a); buf.vertex(mat,x1,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y2,z2).color(r,g,b,a); buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z1).color(r,g,b,a); buf.vertex(mat,x1,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z1).color(r,g,b,a); buf.vertex(mat,x2,y2,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y1,z2).color(r,g,b,a); buf.vertex(mat,x2,y2,z2).color(r,g,b,a);
            buf.vertex(mat,x1,y1,z2).color(r,g,b,a); buf.vertex(mat,x1,y2,z2).color(r,g,b,a);

            BuiltBuffer built = buf.end();
            BufferRenderer.drawWithGlobalProgram(built);
            built.close();
        }
    }

    // ── Single Line ───────────────────────────────────────────────────────
    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        int r = color.getRed(), g = color.getGreen(),
            b = color.getBlue(), a = color.getAlpha();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        try (BufferAllocator alloc = new BufferAllocator(256)) {
            BufferBuilder buf = new BufferBuilder(alloc,
                VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

            buf.vertex(mat,x1,y1,z1).color(r,g,b,a);
            buf.vertex(mat,x2,y2,z2).color(r,g,b,a);

            BuiltBuffer built = buf.end();
            BufferRenderer.drawWithGlobalProgram(built);
            built.close();
        }
    }

    // ── Convenience overloads ─────────────────────────────────────────────
    public static void renderFilledBox(MatrixStack matrices, Box box, Color color) {
        renderFilledBox(matrices,
            (float)box.minX,(float)box.minY,(float)box.minZ,
            (float)box.maxX,(float)box.maxY,(float)box.maxZ, color);
    }

    public static void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        renderBoxOutline(matrices,
            (float)box.minX,(float)box.minY,(float)box.minZ,
            (float)box.maxX,(float)box.maxY,(float)box.maxZ, color);
    }
}
