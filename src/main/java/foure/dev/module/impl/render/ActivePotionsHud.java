package foure.dev.module.impl.render;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.HudModule;
import foure.dev.module.api.ModuleInfo;
import foure.dev.util.render.core.Renderer2D;
import foure.dev.util.render.text.FontRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@ModuleInfo(name = "PotionsHud", category = Category.RENDER, desc = "Active potion effects")
public class ActivePotionsHud extends HudModule {

    public ActivePotionsHud() {
        // Default: top-right area (like Krypton screenshot)
        setX(530); setY(35);
    }

    @Subscribe
    public void onRender(RenderEvent event) {
        if (!isToggled() || mc.player == null) return;
        Renderer2D r = event.renderer();

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        List<StatusEffectInstance> list = new ArrayList<>(effects);
        if (list.isEmpty()) return;

        float x = getX(), y = getY();
        float fontSize = 8f, itemH = 13f, gap = 1f;
        float maxW = 0;

        for (StatusEffectInstance eff : list) {
            String name = getEffectName(eff);
            float w = r.getStringWidth(FontRegistry.INTER_MEDIUM, name, fontSize) + 50f;
            if (w > maxW) maxW = w;
        }

        float totalH = list.size() * (itemH + gap) - gap + 14f;
        setWidth(maxW); setHeight(totalH);

        // Header
        r.rect(x, y, maxW, 12f, 3f, new Color(8,4,16,200).getRGB());
        r.text(FontRegistry.INTER_SEMIBOLD, x + maxW/2f, y + 8f, 7.5f, "ACTIVE POTIONS",
            new Color(180,180,200).getRGB(), "c");

        float cy = y + 14f;
        for (StatusEffectInstance eff : list) {
            String name     = getEffectName(eff);
            String duration = formatDuration(eff.getDuration());
            boolean good    = !eff.getEffectType().value().isBeneficial() ? false : true;

            Color effColor = good ? new Color(80,200,120) : new Color(200,80,80);
            int amp = eff.getAmplifier() + 1;

            r.rect(x, cy, maxW, itemH, 2f, new Color(8,4,16,170).getRGB());

            // Colored dot
            r.rect(x + 4f, cy + itemH/2f - 3f, 6f, 6f, 3f, effColor.getRGB());

            // Name + level
            String label = name + (amp > 1 ? " " + toRoman(amp) : "");
            r.text(FontRegistry.INTER_MEDIUM, x + 14f, cy + itemH/2f + 2.5f, fontSize,
                label, new Color(220,220,235).getRGB(), "l");

            // Duration right
            r.text(FontRegistry.INTER_MEDIUM, x + maxW - 5f, cy + itemH/2f + 2.5f, fontSize,
                duration, effColor.brighter().getRGB(), "r");

            cy += itemH + gap;
        }
    }

    private String getEffectName(StatusEffectInstance eff) {
        return eff.getEffectType().value().getName().getString();
    }

    private String formatDuration(int ticks) {
        if (ticks > 32767) return "∞";
        int secs = ticks / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }

    private String toRoman(int n) {
        return switch (n) {
            case 2  -> "II";
            case 3  -> "III";
            case 4  -> "IV";
            case 5  -> "V";
            default -> String.valueOf(n);
        };
    }
}
