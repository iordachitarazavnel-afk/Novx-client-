package foure.dev.util.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
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
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
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
        // north (z1)
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        // south (z2)
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        // west (x1)
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x1, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x1, y2, z1).color(r, g, b, a);
        // east (x2)
        buf.vertex(mat, x2, y2, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z2).color(r, g, b, a);
        buf.vertex(mat, x2, y1, z1).color(r, g, b, a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ── Box outline ───────────────────────────────────────────────────────

    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.5f);
        RenderSystem.depthMask(false);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();

        // bottom edges
        addLine(buf, mat, x1,y1,z1, x2,y1,z1, r,g,b,a);
        addLine(buf, mat, x2,y1,z1, x2,y1,z2, r,g,b,a);
        addLine(buf, mat, x2,y1,z2, x1,y1,z2, r,g,b,a);
        addLine(buf, mat, x1,y1,z2, x1,y1,z1, r,g,b,a);
        // top edges
        addLine(buf, mat, x1,y2,z1, x2,y2,z1, r,g,b,a);
        addLine(buf, mat, x2,y2,z1, x2,y2,z2, r,g,b,a);
        addLine(buf, mat, x2,y2,z2, x1,y2,z2, r,g,b,a);
        addLine(buf, mat, x1,y2,z2, x1,y2,z1, r,g,b,a);
        // vertical edges
        addLine(buf, mat, x1,y1,z1, x1,y2,z1, r,g,b,a);
        addLine(buf, mat, x2,y1,z1, x2,y2,z1, r,g,b,a);
        addLine(buf, mat, x2,y1,z2, x2,y2,z2, r,g,b,a);
        addLine(buf, mat, x1,y1,z2, x1,y2,z2, r,g,b,a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ── Single line ───────────────────────────────────────────────────────

    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        float r = color.getRed()   / 255.0f;
        float g = color.getGreen() / 255.0f;
        float b = color.getBlue()  / 255.0f;
        float a = color.getAlpha() / 255.0f;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(1.5f);
        RenderSystem.depthMask(false);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f mat = matrices.peek().getPositionMatrix();
        addLine(buf, mat, x1,y1,z1, x2,y2,z2, r,g,b,a);

        BufferRenderer.drawWithGlobalProgram(buf.end());
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // ── Box from net.minecraft.util.math.Box ─────────────────────────────

    public static void renderFilledBox(MatrixStack matrices, Box box, Color color) {
        renderFilledBox(matrices,
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ,
            color);
    }

    public static void renderBoxOutline(MatrixStack matrices, Box box, Color color) {
        renderBoxOutline(matrices,
            (float)box.minX, (float)box.minY, (float)box.minZ,
            (float)box.maxX, (float)box.maxY, (float)box.maxZ,
            color);
    }

    // ── Internal helper ───────────────────────────────────────────────────

    private static void addLine(BufferBuilder buf, Matrix4f mat,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        buf.vertex(mat, x1, y1, z1).color(r, g, b, a);
        buf.vertex(mat, x2, y2, z2).color(r, g, b, a);
    }
}

