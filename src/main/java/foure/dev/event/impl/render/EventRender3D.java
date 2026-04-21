package foure.dev.event.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import java.awt.Color;

public class EventRender3D {
    private static final EventRender3D INSTANCE = new EventRender3D();
    
    public MatrixStack matrices;
    public Renderer3D renderer;
    public Renderer3D depthRenderer;
    public double frameTime;
    public float tickDelta;
    public double offsetX;
    public double offsetY;
    public double offsetZ;

    public EventRender3D() {
        super();
    }

    public static EventRender3D get(MatrixStack matrices, Renderer3D renderer, Renderer3D depthRenderer, float tickDelta, double offsetX, double offsetY, double offsetZ) {
        INSTANCE.matrices = matrices;
        INSTANCE.renderer = renderer;
        INSTANCE.depthRenderer = depthRenderer;
        INSTANCE.frameTime = System.currentTimeMillis() / 1000.0;
        INSTANCE.tickDelta = tickDelta;
        INSTANCE.offsetX = offsetX;
        INSTANCE.offsetY = offsetY;
        INSTANCE.offsetZ = offsetZ;
        return INSTANCE;
    }

    // Interfața pentru a permite apelurile renderer.box și renderer.line
    public interface Renderer3D {
        void box(Box box, Color side, Color line, Object shapeMode, int exclude);
        void line(double x1, double y1, double z1, double x2, double y2, double z2, Color color);
    }
}
