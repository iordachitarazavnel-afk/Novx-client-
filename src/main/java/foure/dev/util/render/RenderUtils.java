package foure.dev.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.awt.Color;

public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Camera getCamera() {
        return mc.gameRenderer.getCamera();
    }

    /**
     * Desenează un cub plin (fill)
     */
    public static void renderFilledBox(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float red = color.getRed() / 255f;
        float green = color.getGreen() / 255f;
        float blue = color.getBlue() / 255f;
        float alpha = color.getAlpha() / 255f;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // Jos
        bufferBuilder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();

        // Sus
        bufferBuilder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();

        // Fețe laterale
        bufferBuilder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y1, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y2, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x1, y2, z1).color(red, green, blue, alpha).next();

        bufferBuilder.vertex(matrix, x2, y1, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z1).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).next();
        bufferBuilder.vertex(matrix, x2, y1, z2).color(red, green, blue, alpha).next();

        tessellator.draw();
    }

    /**
     * Desenează marginile unui cub (outline)
     */
    public static void renderBoxOutline(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;

        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        // Liniile de jos
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();

        // Liniile de sus
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();

        // Liniile verticale
        buffer.vertex(matrix, x1, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z1).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x2, y2, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y1, z2).color(r, g, b, a).next();
        buffer.vertex(matrix, x1, y2, z2).color(r, g, b, a).next();

        tessellator.draw();
    }

    /**
     * Desenează o linie simplă (folosită pentru raze/beams)
     */
    public static void drawLine(MatrixStack matrices, float x1, float y1, float z1, float x2, float y2, float z2, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x1, y1, z1).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        buffer.vertex(matrix, x2, y2, z2).color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()).next();
        tessellator.draw();
    }
}

