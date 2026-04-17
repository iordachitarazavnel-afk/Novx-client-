package foure.dev.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * RenderUtils for MC 1.21.11 / Yarn 1.21.11+build.2
 *
 * The old Tessellator/BufferRenderer/VertexFormat pattern has been removed.
 * Rendering now uses the RenderPipeline / GpuBuffer system (com.mojang.blaze3d).
 */
public class RenderUtils {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Pipelines — depth test enabled (normal) and disabled (through walls)
    public static final RenderPipeline FILLED_BOX_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.of("foure", "pipeline/filled_box"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    );
    public static final RenderPipeline FILLED_BOX_NODEPTH_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.of("foure", "pipeline/filled_box_nodepth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );
    public static final RenderPipeline LINES_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_LINE_SNIPPET)
            .withLocation(Identifier.of("foure", "pipeline/lines"))
            .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
            .build()
    );
    public static final RenderPipeline LINES_NODEPTH_PIPELINE = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_LINE_SNIPPET)
            .withLocation(Identifier.of("foure", "pipeline/lines_nodepth"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    );

    private static final ByteBufferBuilder ALLOCATOR = new ByteBufferBuilder(RenderLayer.SMALL_BUFFER_SIZE);
    private static final Vector4f COLOR_MOD   = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFF   = new Vector3f();
    private static final Matrix4f TEX_MATRIX  = new Matrix4f();

    // Per-frame ring buffer for vertex uploads (lazy-init)
    private static com.mojang.blaze3d.systems.MappableRingBuffer ringBuffer;

    // ── Public API ────────────────────────────────────────────────────────

    public static Camera getCamera() {
        return mc.getEntityRenderDispatcher().camera;
    }

    /** Draw a filled RGBA box (depth-tested). */
    public static void renderFilledBox(org.joml.Matrix4fc mat,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       Color color) {
        drawFilledBox(mat, x1, y1, z1, x2, y2, z2, color, FILLED_BOX_NODEPTH_PIPELINE);
    }

    /** Draw a box outline (depth-tested). */
    public static void renderBoxOutline(org.joml.Matrix4fc mat,
                                        float x1, float y1, float z1,
                                        float x2, float y2, float z2,
                                        Color color) {
        drawBoxOutline(mat, x1, y1, z1, x2, y2, z2, color, LINES_NODEPTH_PIPELINE);
    }

    /** Draw a single line (no depth). */
    public static void drawLine(org.joml.Matrix4fc mat,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                Color color) {
        BufferBuilder buf = new BufferBuilder(ALLOCATOR,
            LINES_NODEPTH_PIPELINE.getVertexFormatMode(),
            LINES_NODEPTH_PIPELINE.getVertexFormat());
        float r = color.getRed()/255f, g = color.getGreen()/255f,
              b = color.getBlue()/255f, a = color.getAlpha()/255f;
        buf.addVertex(mat, x1, y1, z1).setColor(r, g, b, a);
        buf.addVertex(mat, x2, y2, z2).setColor(r, g, b, a);
        flushAndDraw(buf, LINES_NODEPTH_PIPELINE);
    }

    // Box overloads
    public static void renderFilledBox(org.joml.Matrix4fc mat, Box box, Color color) {
        renderFilledBox(mat,
            (float)box.minX,(float)box.minY,(float)box.minZ,
            (float)box.maxX,(float)box.maxY,(float)box.maxZ, color);
    }
    public static void renderBoxOutline(org.joml.Matrix4fc mat, Box box, Color color) {
        renderBoxOutline(mat,
            (float)box.minX,(float)box.minY,(float)box.minZ,
            (float)box.maxX,(float)box.maxY,(float)box.maxZ, color);
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private static void drawFilledBox(Matrix4fc mat,
                                      float x1, float y1, float z1,
                                      float x2, float y2, float z2,
                                      Color color, RenderPipeline pipeline) {
        BufferBuilder buf = new BufferBuilder(ALLOCATOR,
            pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        float r = color.getRed()/255f, g = color.getGreen()/255f,
              b = color.getBlue()/255f, a = color.getAlpha()/255f;
        // front
        buf.addVertex(mat,x1,y1,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y1,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y2,z2).setColor(r,g,b,a);
        // back
        buf.addVertex(mat,x2,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y2,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z1).setColor(r,g,b,a);
        // left
        buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y2,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y2,z1).setColor(r,g,b,a);
        // right
        buf.addVertex(mat,x2,y1,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z2).setColor(r,g,b,a);
        // top
        buf.addVertex(mat,x1,y2,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y2,z1).setColor(r,g,b,a);
        // bottom
        buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y1,z2).setColor(r,g,b,a);
        buf.addVertex(mat,x1,y1,z2).setColor(r,g,b,a);
        flushAndDraw(buf, pipeline);
    }

    private static void drawBoxOutline(Matrix4fc mat,
                                       float x1, float y1, float z1,
                                       float x2, float y2, float z2,
                                       Color color, RenderPipeline pipeline) {
        BufferBuilder buf = new BufferBuilder(ALLOCATOR,
            pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
        float r = color.getRed()/255f, g = color.getGreen()/255f,
              b = color.getBlue()/255f, a = color.getAlpha()/255f;
        addLine(buf,mat, x1,y1,z1, x2,y1,z1, r,g,b,a);
        addLine(buf,mat, x2,y1,z1, x2,y1,z2, r,g,b,a);
        addLine(buf,mat, x2,y1,z2, x1,y1,z2, r,g,b,a);
        addLine(buf,mat, x1,y1,z2, x1,y1,z1, r,g,b,a);
        addLine(buf,mat, x1,y2,z1, x2,y2,z1, r,g,b,a);
        addLine(buf,mat, x2,y2,z1, x2,y2,z2, r,g,b,a);
        addLine(buf,mat, x2,y2,z2, x1,y2,z2, r,g,b,a);
        addLine(buf,mat, x1,y2,z2, x1,y2,z1, r,g,b,a);
        addLine(buf,mat, x1,y1,z1, x1,y2,z1, r,g,b,a);
        addLine(buf,mat, x2,y1,z1, x2,y2,z1, r,g,b,a);
        addLine(buf,mat, x2,y1,z2, x2,y2,z2, r,g,b,a);
        addLine(buf,mat, x1,y1,z2, x1,y2,z2, r,g,b,a);
        flushAndDraw(buf, pipeline);
    }

    private static void addLine(BufferBuilder buf, Matrix4fc mat,
                                 float x1,float y1,float z1,
                                 float x2,float y2,float z2,
                                 float r,float g,float b,float a) {
        buf.addVertex(mat,x1,y1,z1).setColor(r,g,b,a);
        buf.addVertex(mat,x2,y2,z2).setColor(r,g,b,a);
    }

    /** Build the BufferBuilder, upload to GPU and execute a render pass. */
    private static void flushAndDraw(BufferBuilder buf, RenderPipeline pipeline) {
        MeshData mesh = buf.buildOrThrow();
        MeshData.DrawState dp = mesh.drawState();
        VertexFormat fmt = dp.format();
        int vbSize = dp.vertexCount() * fmt.getVertexSize();

        if (ringBuffer == null || ringBuffer.size() < vbSize) {
            if (ringBuffer != null) ringBuffer.close();
            ringBuffer = new com.mojang.blaze3d.systems.MappableRingBuffer(
                () -> "foure_render_utils",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_MAP_WRITE,
                Math.max(vbSize, 65536));
        }

        CommandEncoder enc = RenderSystem.getDevice().createCommandEncoder();
        try (GpuBuffer.MappedView mv = enc.mapBuffer(
                ringBuffer.currentBuffer().slice(0, mesh.vertexBuffer().remaining()), false, true)) {
            MemoryUtil.memCopy(mesh.vertexBuffer(), mv.data());
        }
        GpuBuffer vertices = ringBuffer.currentBuffer();

        GpuBuffer indices;
        VertexFormat.IndexType indexType;
        if (pipeline.getVertexFormatMode() == VertexFormat.Mode.QUADS) {
            mesh.sortQuads(ALLOCATOR, RenderSystem.getProjectionType().vertexSorting());
            indices = fmt.uploadImmediateIndexBuffer(mesh.indexBuffer());
            indexType = dp.indexType();
        } else {
            RenderSystem.AutoStorageIndexBuffer seq = RenderSystem.getSequentialBuffer(pipeline.getVertexFormatMode());
            indices = seq.getBuffer(dp.indexCount());
            indexType = seq.type();
        }

        GpuBufferSlice dynTransforms = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), COLOR_MOD, MODEL_OFF, TEX_MATRIX);

        try (RenderPass rp = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "foure_render_pass",
                mc.getFramebuffer().getColorAttachment(),
                OptionalInt.empty(),
                mc.getFramebuffer().getDepthAttachment(),
                OptionalDouble.empty())) {
            rp.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(rp);
            rp.setUniform("DynamicTransforms", dynTransforms);
            rp.setVertexBuffer(0, vertices);
            rp.setIndexBuffer(indices, indexType);
            rp.drawIndexed(0, 0, dp.indexCount(), 1);
        }

        mesh.close();
        ringBuffer.rotate();
    }

    /** Call from GameRenderer#close mixin to avoid leaking GPU buffers. */
    public static void close() {
        ALLOCATOR.close();
        if (ringBuffer != null) { ringBuffer.close(); ringBuffer = null; }
    }
}
