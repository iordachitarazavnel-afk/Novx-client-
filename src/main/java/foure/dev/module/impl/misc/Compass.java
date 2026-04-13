package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.ColorSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import net.minecraft.entity.player.PlayerEntity;

import java.awt.Color;

@ModuleInfo(
    name = "Compass",
    category = Category.MISC,
    desc = "Shows directions and nearby players on a radar"
)
public class Compass extends Function {

    private final NumberSetting posX  = new NumberSetting("X",     this, 10.0, 0.0, 2000.0, 1.0);
    private final NumberSetting posY  = new NumberSetting("Y",     this, 10.0, 0.0, 2000.0, 1.0);
    private final NumberSetting size  = new NumberSetting("Size",  this, 120.0, 40.0, 300.0, 1.0);
    private final NumberSetting range = new NumberSetting("Range", this, 100.0, 10.0, 200.0, 1.0);
    private final ColorSetting bgColor     = new ColorSetting("Background",   this, new Color(15,  20,  35,  220));
    private final ColorSetting gridColor   = new ColorSetting("Grid Color",   this, new Color(100, 100, 255, 100));
    private final ColorSetting playerColor = new ColorSetting("Player Color", this, new Color(255, 50,  50,  255));
    private final ColorSetting textColor   = new ColorSetting("Text Color",   this, new Color(200, 200, 255, 255));

    public Compass() {
        this.addSettings(new Setting[]{
            this.posX, this.posY, this.size, this.range,
            this.bgColor, this.gridColor, this.playerColor, this.textColor
        });
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (fullNullCheck()) return;
        doRender(event.renderer(), (int) posX.getValueFloat(), (int) posY.getValueFloat());
    }

    private void doRender(Renderer2D r, int px, int py) {
        int d  = (int) size.getValueFloat();
        int rd = d / 2;
        int cx = px + rd;
        int cy = py + rd;

        Color bg  = (Color) bgColor.getValue();
        Color gc  = (Color) gridColor.getValue();
        Color tc  = (Color) textColor.getValue();
        Color pc  = (Color) playerColor.getValue();

        // background
        r.rect(px, py, d, d, rd, bg.getRGB());

        // grid circles
        drawCircle(r, cx, cy, rd,              gc);
        drawCircle(r, cx, cy, (int)(rd * 0.75), gc);
        drawCircle(r, cx, cy, (int)(rd * 0.5),  gc);
        drawCircle(r, cx, cy, (int)(rd * 0.25), gc);

        // direction labels N W S E
        float yaw = mc.player.getYaw();
        double baseAngle = Math.toRadians(yaw + 180.0);
        String[] dirs = {"N", "W", "S", "E"};
        for (int i = 0; i < 4; i++) {
            double angle = baseAngle + i * Math.PI / 2.0 - Math.PI / 2.0;
            float lx = (float)(cx + Math.cos(angle) * (rd - 14));
            float ly = (float)(cy + Math.sin(angle) * (rd - 14));
            r.text(FontRegistry.INTER_MEDIUM, lx, ly + 3, 7f, dirs[i], tc.getRGB(), "c");
        }

        // tick marks every 30°
        for (int a = 0; a < 360; a += 30) {
            double angle = Math.toRadians(a) + baseAngle;
            float x1 = (float)(cx + Math.cos(angle) * (rd - 5));
            float y1 = (float)(cy + Math.sin(angle) * (rd - 5));
            float x2 = (float)(cx + Math.cos(angle) * rd);
            float y2 = (float)(cy + Math.sin(angle) * rd);
            r.line(x1, y1, x2, y2, 1f, gc.getRGB());
        }

        // nearby players
        double rangeVal = range.getValueFloat();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            double dist = mc.player.distanceTo(player);
            if (dist > rangeVal) continue;
            double dx = player.getX() - mc.player.getX();
            double dz = player.getZ() - mc.player.getZ();
            double angleToTarget = Math.atan2(dz, dx);
            double relAngle = angleToTarget - Math.toRadians(yaw + 90.0);
            double renderDist = dist / rangeVal * (rd - 10);
            float rx = (float)(cx + Math.cos(relAngle) * renderDist);
            float ry = (float)(cy + Math.sin(relAngle) * renderDist);
            r.rect(rx - 2, ry - 2, 4, 4, 2f, pc.getRGB());
        }
    }

    private void drawCircle(Renderer2D r, int cx, int cy, int radius, Color color) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a1 = 2.0 * Math.PI * i       / segments;
            double a2 = 2.0 * Math.PI * (i + 1) / segments;
            r.line(
                (float)(cx + Math.cos(a1) * radius), (float)(cy + Math.sin(a1) * radius),
                (float)(cx + Math.cos(a2) * radius), (float)(cy + Math.sin(a2) * radius),
                1f, color.getRGB()
            );
        }
    }
}

