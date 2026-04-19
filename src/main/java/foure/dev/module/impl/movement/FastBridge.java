package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;

/**
 * FastBridge — automatically sneaks on block edges when bridging.
 */
@ModuleInfo(
    name = "FastBridge",
    category = Category.MOVEMENT,
    desc = "Automatically sneaks on block edges"
)
public class FastBridge extends Function {

    private boolean bridging = false;

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        boolean holdingBlock = mc.player.getMainHandStack().getItem() instanceof BlockItem
                            || mc.player.getOffHandStack().getItem() instanceof BlockItem;

        if (!holdingBlock) {
            if (bridging) { mc.options.sneakKey.setPressed(false); bridging = false; }
            return;
        }

        if (mc.player.getPitch() < 70) {
            if (bridging) { mc.options.sneakKey.setPressed(false); bridging = false; }
            return;
        }

        BlockPos below = BlockPos.ofFloored(mc.player.getPos()).down();
        boolean open = mc.world.getBlockState(below).isReplaceable()
                    && mc.world.getBlockState(below.down()).isReplaceable()
                    && mc.world.getBlockState(below.down().down()).isReplaceable();

        if (open) {
            mc.options.sneakKey.setPressed(true);
            bridging = true;
        } else {
            if (bridging) { mc.options.sneakKey.setPressed(false); bridging = false; }
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (bridging) { mc.options.sneakKey.setPressed(false); bridging = false; }
    }
}

