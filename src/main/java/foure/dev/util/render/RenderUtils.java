package foure.dev.util.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;

import java.awt.Color;

/**
 * RenderUtils for MC 1.21.11 / Yarn 1.21.11+build.2
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
        int argb = color.getRGB();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        try (BufferAllocator alloc = new BufferAllocator(1024)) {
            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(alloc);
            VertexConsumer vc = vcp.getBuffer(RenderLayers.debugFilledBox());

            vc.vertex(mat,x1,y1,z2).color(argb);
            vc.vertex(mat,x2,y1,z2).color(argb);
            vc.vertex(mat,x2,y2,z2).color(argb);
            vc.vertex(mat,x1,y2,z2).color(argb);

            vc.vertex(mat,x2,y1,z1).color(argb);
            vc.vertex(mat,x1,y1,z1).color(argb);
            vc.vertex(mat,x1,y2,z1).color(argb);
            vc.vertex(mat,x2,y2,z1).color(argb);

            vc.vertex(mat,x1,y1,z1).color(argb);
            vc.vertex(mat,x1,y1,z2).color(argb);
            vc.vertex(mat,x1,y2,z2).color(argb);
            vc.vertex(mat,x1,y2,z1).color(argb);

            vc.vertex(mat,x2,y1,z2).color(argb);
            vc.vertex(mat,x2,y1,z1).color(argb);
            vc.vertex(mat,x2,y2,z1).color(argb);
            vc.vertex(mat,x2,y2,z2).color(argb);

            vc.vertex(mat,x1,y2,z2).color(argb);
            vc.vertex(mat,x2,y2,z2).color(argb);
            vc.vertex(mat,x2,y2,z1).color(argb);
            vc.vertex(mat,x1,y2,z1).color(argb);

            vc.vertex(mat,x1,y1,z1).color(argb);
            vc.vertex(mat,x2,y1,z1).color(argb);
            vc.vertex(mat,x2,y1,z2).color(argb);
            vc.vertex(mat,x1,y1,z2).color(argb);

            vcp.draw();
        }
    }

    public static void renderBoxOutline(MatrixStack matrices,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        int argb = color.getRGB();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        try (BufferAllocator alloc = new BufferAllocator(1024)) {
            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(alloc);
            VertexConsumer vc = vcp.getBuffer(RenderLayers.lines());
            
            vc.vertex(mat,x1,y1,z1).color(argb); vc.vertex(mat,x2,y1,z1).color(argb);
            vc.vertex(mat,x2,y1,z1).color(argb); vc.vertex(mat,x2,y1,z2).color(argb);
            vc.vertex(mat,x2,y1,z2).color(argb); vc.vertex(mat,x1,y1,z2).color(argb);
            vc.vertex(mat,x1,y1,z2).color(argb); vc.vertex(mat,x1,y1,z1).color(argb);
            vc.vertex(mat,x1,y2,z1).color(argb); vc.vertex(mat,x2,y2,z1).color(argb);
            vc.vertex(mat,x2,y2,z1).color(argb); vc.vertex(mat,x2,y2,z2).color(argb);
            vc.vertex(mat,x2,y2,z2).color(argb); vc.vertex(mat,x1,y2,z2).color(argb);
            vc.vertex(mat,x1,y2,z2).color(argb); vc.vertex(mat,x1,y2,z1).color(argb);
            vc.vertex(mat,x1,y1,z1).color(argb); vc.vertex(mat,x1,y2,z1).color(argb);
            vc.vertex(mat,x2,y1,z1).color(argb); vc.vertex(mat,x2,y2,z1).color(argb);
            vc.vertex(mat,x2,y1,z2).color(argb); vc.vertex(mat,x2,y2,z2).color(argb);
            vc.vertex(mat,x1,y1,z2).color(argb); vc.vertex(mat,x1,y2,z2).color(argb);

            vcp.draw();
        }
    }

    public static void drawLine(MatrixStack matrices,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        int argb = color.getRGB();
        Matrix4f mat = matrices.peek().getPositionMatrix();

        try (BufferAllocator alloc = new BufferAllocator(128)) {
            VertexConsumerProvider.Immediate vcp = VertexConsumerProvider.immediate(alloc);
            VertexConsumer vc = vcp.getBuffer(RenderLayers.lines());
            vc.vertex(mat,x1,y1,z1).color(argb);
            vc.vertex(mat,x2,y2,z2).color(argb);
            vcp.draw();
        }
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
