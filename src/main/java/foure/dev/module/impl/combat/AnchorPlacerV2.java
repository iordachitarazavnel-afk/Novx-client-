package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.state.property.Properties;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.Direction;

/**
 * AnchorPlacer v2 — places a respawn anchor and optionally glowstones it.
 */
@ModuleInfo(
    name = "AnchorPlacer v2",
    category = Category.COMBAT,
    desc = "Places an anchor and glowstones it"
)
public class AnchorPlacerV2 extends Function {

    private final ModeSetting    mode        = new ModeSetting("Mode",        this, "Normal", new String[]{"Normal","Glowstone"});
    private final NumberSetting  delay       = new NumberSetting("Delay",     this, 50.0, 0.0, 200.0, 1.0);
    private final BooleanSetting glowstone   = new BooleanSetting("Glowstone", true);
    private final BooleanSetting switchBack  = new BooleanSetting("Switch Back", true);
    private final NumberSetting  switchSlot  = new NumberSetting("Switch Slot", this, 1.0, 1.0, 9.0, 1.0);

    private final TimerUtil timer = new TimerUtil();
    private int progress = 0;

    public AnchorPlacerV2() {
        addSettings(new Setting[]{ mode, delay, glowstone, switchBack, switchSlot });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        progress = 0;
        timer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (!timer.delay((float) delay.getValueFloat())) return;

        if (mode.getValue().equals("Normal")) {
            runNormal();
        } else {
            runGlowstone();
        }
    }

    private void runNormal() {
        switch (progress) {
            case 0 -> {
                // Find anchor in hotbar
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() == Items.RESPAWN_ANCHOR) {
                        mc.player.getInventory().selectedSlot = i;
                        next(); return;
                    }
                }
                toggle();
            }
            case 1 -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
                if (bhr.getBlockPos() == null) return;
                Direction dir = bhr.getSide();
                var placePos = bhr.getBlockPos().add(dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ());
                if (!mc.world.getBlockState(placePos).isAir()) { next(); return; }
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.player.swingHand(Hand.MAIN_HAND);
                next();
            }
            case 2 -> {
                if (!(Boolean) glowstone.getValue()) { toggle(); return; }
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() == Items.GLOWSTONE) {
                        mc.player.getInventory().selectedSlot = i;
                        next(); return;
                    }
                }
                toggle();
            }
            case 3 -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
                if (mc.world.getBlockState(bhr.getBlockPos()).getBlock() != Blocks.RESPAWN_ANCHOR) return;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.player.swingHand(Hand.MAIN_HAND);
                next();
            }
            case 4 -> toggle();
        }
    }

    private void runGlowstone() {
        switch (progress) {
            case 0 -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
                var state = mc.world.getBlockState(bhr.getBlockPos());
                if (state.getBlock() != Blocks.RESPAWN_ANCHOR || state.get(Properties.CHARGES) != 0) return;
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).getItem() == Items.GLOWSTONE) {
                        mc.player.getInventory().selectedSlot = i;
                        next(); return;
                    }
                }
                toggle();
            }
            case 1 -> {
                if (!(mc.crosshairTarget instanceof BlockHitResult bhr)) return;
                var state = mc.world.getBlockState(bhr.getBlockPos());
                if (state.getBlock() != Blocks.RESPAWN_ANCHOR || state.get(Properties.CHARGES) != 0) return;
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                mc.player.swingHand(Hand.MAIN_HAND);
                next();
                if ((Boolean) switchBack.getValue()) {
                    mc.player.getInventory().selectedSlot = (int) switchSlot.getValueFloat() - 1;
                    progress = 0; timer.reset();
                }
            }
        }
    }

    private void next() { progress++; timer.reset(); }
}

