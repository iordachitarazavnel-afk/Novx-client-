package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.input.KeyNameUtil;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;

@ModuleInfo(name = "HotkeysHud", category = Category.RENDER, desc = "Shows module keybinds")
public class HotkeysHud extends HudModule {

    public HotkeysHud() {
        setX(130); setY(60);
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isToggled() || mc.player == null) return;
        Renderer2D r = event.renderer();

        List<Function> bound = FourEClient.getInstance().getFunctionManager().getModules()
            .stream()
            .filter(m -> m.getKey() > 0)
            .collect(Collectors.toList());

        float x = getX(), y = getY();
        float fontSize = 8f, itemH = 12f, gap = 2f, pad = 6f;
        float maxW = 0;

        // Measure max width
        for (Function m : bound) {
            String line = m.getName() + "  " + KeyNameUtil.getKeyName(m.getKey());
            float w = r.getStringWidth(FontRegistry.INTER_MEDIUM, line, fontSize) + pad * 2;
            if (w > maxW) maxW = w;
        }
        if (bound.isEmpty()) {
            maxW = 80f;
        }

        float totalH = bound.isEmpty() ? itemH : bound.size() * (itemH + gap) - gap + itemH;
        setWidth(maxW); setHeight(totalH + 14f);

        // Header
        r.rect(x, y, maxW, 12f, 3f, new Color(8,4,16,200).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD, x + maxW/2f, y + 8f, 7.5f, "HOTKEYS",
            new Color(180,180,200).getRGB(), "c");

        float cy = y + 14f;
        if (bound.isEmpty()) {
            r.rect(x, cy, maxW, itemH, 2f, new Color(8,4,16,160).getRGB());
            r.text(FontRegistry.INTER_MEDIUM, x + pad, cy + itemH/2f + 2.5f, fontSize,
                "None bound", new Color(80,80,95).getRGB(), "l");
            return;
        }

        for (Function m : bound) {
            String modName = m.getName();
            String keyName = KeyNameUtil.getKeyName(m.getKey());
            boolean active = m.isToggled();

            r.rect(x, cy, maxW, itemH, 2f, new Color(8,4,16,active?180:140).getRGB());

            // Module name
            int nameColor = active ? new Color(220,220,235).getRGB() : new Color(120,120,135).getRGB();
            r.text(FontRegistry.INTER_MEDIUM, x + pad, cy + itemH/2f + 2.5f, fontSize,
                modName, nameColor, "l");

            // Key name right-aligned
            r.text(FontRegistry.INTER_MEDIUM, x + maxW - pad,
                cy + itemH/2f + 2.5f, fontSize, keyName,
                HUD.getAnimColorStatic(bound.indexOf(m)).getRGB(), "r");

            cy += itemH + gap;
        }
    }
}
