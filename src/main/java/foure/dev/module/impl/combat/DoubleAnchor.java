package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.TimerUtil;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * DoubleAnchor — places two respawn anchors and glowstones them sequentially.
 */
@ModuleInfo(
    name = "DoubleAnchor",
    category = Category.COMBAT,
    desc = "Places and charges 2 anchors on the block you are looking at"
)
public class DoubleAnchor extends Function {

    private final NumberSetting switchDelay = new NumberSetting("Switch Delay", this, 5.0, 0.0, 20.0, 1.0);
    private final NumberSetting totemSlot   = new NumberSetting("Totem Slot",   this, 1.0, 1.0, 9.0,  1.0);

    private final TimerUtil timer = new TimerUtil();
    private int  step        = 0;
    private int  delayCount  = 0;
    private boolean anchoring = false;

    public DoubleAnchor() {
        addSettings(new Setting[]{ switchDelay, totemSlot });
    }

    @Override
    public void onEnable() { super.onEnable(); reset(); }
    @Override
    public void onDisable() { super.onDisable(); reset(); }

    private void reset() { step = 0; delayCount = 0; anchoring = false; }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (!mc.options.attackKey.isPressed() && !anchoring) return;
        if (!hasRequiredItems()) return;

        HitResult hit = mc.crosshairTarget;
        if (!(hit instanceof BlockHitResult bhr)) { reset(); return; }
        if (mc.world.getBlockState(bhr.getBlockPos()).isAir()) { reset(); return; }

        anchoring = true;

        if (delayCount < (int) switchDelay.getValueFloat()) { delayCount++; return; }
        delayCount = 0;

        switch (step) {
            case 0  -> swapTo(Items.RESPAWN_ANCHOR);
            case 1  -> interact(bhr);
            case 2  -> swapTo(Items.GLOWSTONE);
            case 3  -> interact(bhr);
            case 4  -> swapTo(Items.RESPAWN_ANCHOR);
            case 5  -> { interact(bhr); interact(bhr); }
            case 6  -> swapTo(Items.GLOWSTONE);
            case 7  -> interact(bhr);
            case 8  -> mc.player.getInventory().selectedSlot = (int) totemSlot.getValueFloat() - 1;
            case 9  -> interact(bhr);
            case 10 -> { reset(); toggle(); return; }
        }
        step++;
    }

    private void swapTo(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                mc.player.getInventory().selectedSlot = i; return;
            }
        }
    }

    private void interact(BlockHitResult bhr) {
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    private boolean hasRequiredItems() {
        boolean anchor = false, glow = false;
        for (int i = 0; i < 9; i++) {
            var item = mc.player.getInventory().getStack(i).getItem();
            if (item == Items.RESPAWN_ANCHOR) anchor = true;
            if (item == Items.GLOWSTONE) glow = true;
        }
        return anchor && glow;
    }
}

