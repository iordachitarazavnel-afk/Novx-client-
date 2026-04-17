package foure.dev.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * RenderUtils for MC 1.21.11 / Yarn 1.21.11+build.2
 *
 * Uses Tessellator/BufferBuilder pattern via reflection-safe approach.
 * In 1.21.11, BufferRenderer and VertexFormat moved packages.
 * We use the WorldRenderer helper approach instead.
 */
public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static Camera getCamera() {
        return mc.getEntityRenderDispatcher().camera;
    }

    public static void renderFilledBox(MatrixStack matrices,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        net.minecraft.client.render.Tessellator tess =
            net.minecraft.client.render.Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            net.minecraft.client.render.VertexFormat.DrawMode.QUADS,
            net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        // bottom
        buf.vertex(mat, x1,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z2).color(r,g,b,a);
        buf.vertex(mat, x1,y1,z2).color(r,g,b,a);
        // top
        buf.vertex(mat, x1,y2,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z1).color(r,g,b,a);
        buf.vertex(mat, x1,y2,z1).color(r,g,b,a);
        // north
        buf.vertex(mat, x1,y2,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x1,y1,z1).color(r,g,b,a);
        // south
        buf.vertex(mat, x1,y1,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z2).color(r,g,b,a);
        buf.vertex(mat, x1,y2,z2).color(r,g,b,a);
        // west
        buf.vertex(mat, x1,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x1,y1,z2).color(r,g,b,a);
        buf.vertex(mat, x1,y2,z2).color(r,g,b,a);
        buf.vertex(mat, x1,y2,z1).color(r,g,b,a);
        // east
        buf.vertex(mat, x2,y2,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z2).color(r,g,b,a);
        buf.vertex(mat, x2,y1,z1).color(r,g,b,a);

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        net.minecraft.client.render.Tessellator tess =
            net.minecraft.client.render.Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES,
            net.minecraft.client.render.VertexFormats.POSITION_COLOR);

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

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        net.minecraft.client.render.Tessellator tess =
            net.minecraft.client.render.Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            net.minecraft.client.render.VertexFormat.DrawMode.DEBUG_LINES,
            net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        buf.vertex(mat, x1,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z2).color(r,g,b,a);

        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

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
