package foure.dev.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * RenderUtils for MC 1.21.4+ / Yarn 1.21.4+
 *
 * Changes vs older versions:
 *  - RenderSystem.enableBlend/disableBlend/defaultBlendFunc/enableDepthTest/disableDepthTest removed
 *  - VertexFormat moved to net.minecraft.client.render.VertexFormat (DrawMode is inner class)
 *  - BufferRenderer.drawWithGlobalProgram -> BufferRenderer.draw
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

        Tessellator tess = Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            VertexFormat.DrawMode.QUADS,
            VertexFormats.POSITION_COLOR);

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

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        Tessellator tess = Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            VertexFormat.DrawMode.DEBUG_LINES,
            VertexFormats.POSITION_COLOR);

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

        BufferRenderer.drawWithGlobalProgram(buf.end());
    }

    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        Tessellator tess = Tessellator.getInstance();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        var buf = tess.begin(
            VertexFormat.DrawMode.DEBUG_LINES,
            VertexFormats.POSITION_COLOR);

        buf.vertex(mat, x1,y1,z1).color(r,g,b,a);
        buf.vertex(mat, x2,y2,z2).color(r,g,b,a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
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
