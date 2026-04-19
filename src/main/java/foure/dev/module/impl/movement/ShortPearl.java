package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.TimerUtil;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

/**
 * ShortPearl — throws an ender pearl downward at your feet.
 */
@ModuleInfo(
    name = "ShortPearl",
    category = Category.MOVEMENT,
    desc = "Throws a pearl at your feet"
)
public class ShortPearl extends Function {

    private final NumberSetting  delay      = new NumberSetting("Delay",      this, 50.0, 0.0, 200.0, 1.0);
    private final NumberSetting  pitch      = new NumberSetting("Pitch",      this, 70.0, 30.0, 90.0, 1.0);
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);

    private final TimerUtil timer = new TimerUtil();
    private int progress  = 0;
    private int prevSlot  = 0;
    private float origPitch = 0;

    public ShortPearl() {
        addSettings(new Setting[]{ delay, pitch, switchBack });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        progress = 0;
        origPitch = mc.player.getPitch();
        timer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (!timer.delay((float) delay.getValueFloat())) return;

        switch (progress) {
            case 0 -> {
                // Find pearl in hotbar
                if (mc.player.getMainHandStack().getItem() == Items.ENDER_PEARL) {
                    next(); return;
                }
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() == Items.ENDER_PEARL) {
                        prevSlot = mc.player.getInventory().selectedSlot;
                        mc.player.getInventory().selectedSlot = i;
                        next(); return;
                    }
                }
                toggle(); // no pearl
            }
            case 1 -> {
                // Aim down
                mc.player.setPitch((float) pitch.getValueFloat());
                next();
            }
            case 2 -> {
                // Throw
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                next();
            }
            case 3 -> {
                // Reset pitch
                mc.player.setPitch(origPitch);
                if ((Boolean) switchBack.getValue())
                    mc.player.getInventory().selectedSlot = prevSlot;
                toggle();
            }
        }
    }

    private void next() { progress++; timer.reset(); }
}

