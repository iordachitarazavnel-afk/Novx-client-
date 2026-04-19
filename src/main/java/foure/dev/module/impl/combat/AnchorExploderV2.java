package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * AnchorExploder v2 — explodes respawn anchors with glowstone when looking at them.
 */
@ModuleInfo(
    name = "AnchorExploder v2",
    category = Category.COMBAT,
    desc = "Explodes anchors with glowstone when looking at them"
)
public class AnchorExploderV2 extends Function {

    private final NumberSetting delay     = new NumberSetting("Delay",      this, 50.0, 0.0, 200.0, 1.0);
    private final NumberSetting switchTo  = new NumberSetting("Switch Slot",this, 1.0,  1.0, 9.0,   1.0);
    private final TimerUtil     timer     = new TimerUtil();

    public AnchorExploderV2() {
        addSettings(new Setting[]{ delay, switchTo });
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (!timer.delay((float) delay.getValueFloat())) return;

        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr)) return;

        var state = mc.world.getBlockState(bhr.getBlockPos());
        if (state.getBlock() != Blocks.RESPAWN_ANCHOR) return;
        if (state.get(Properties.CHARGES) == 0) return;

        // If holding glowstone → switch to explode slot then interact
        if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
            int slot = (int) switchTo.getValueFloat() - 1;
            mc.player.getInventory().selectedSlot = slot;
            timer.reset();
            return;
        }

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.player.swingHand(Hand.MAIN_HAND);
        timer.reset();
    }
}

