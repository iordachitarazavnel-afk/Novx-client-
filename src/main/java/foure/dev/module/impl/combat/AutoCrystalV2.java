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
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * AutoCrystal v2 — places and explodes end crystals on obsidian/bedrock.
 */
@ModuleInfo(
    name = "AutoCrystal v2",
    category = Category.COMBAT,
    desc = "Automatically places and explodes end crystals"
)
public class AutoCrystalV2 extends Function {

    private final NumberSetting  delay       = new NumberSetting("Delay",      this, 50.0, 0.0, 200.0, 1.0);
    private final BooleanSetting silentSwap  = new BooleanSetting("Silent Swap", false);
    private final BooleanSetting inAir       = new BooleanSetting("In Air",      false);
    private final BooleanSetting switchMode  = new BooleanSetting("Switch On Enable", false);

    private final TimerUtil timer = new TimerUtil();
    private BlockPos lastPlaced = null;

    public AutoCrystalV2() {
        addSettings(new Setting[]{ delay, silentSwap, inAir, switchMode });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if ((Boolean) switchMode.getValue()) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                    mc.player.getInventory().selectedSlot = i; break;
                }
            }
        }
        timer.reset();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        lastPlaced = null;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;
        if (!mc.player.isOnGround() && !(Boolean) inAir.getValue()) return;
        if (!timer.delay((float) delay.getValueFloat())) return;

        HitResult hit = mc.crosshairTarget;

        // Try break crystal
        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EndCrystalEntity crystal) {
            mc.interactionManager.attackEntity(mc.player, crystal);
            mc.player.swingHand(Hand.MAIN_HAND);
            lastPlaced = null;
            timer.reset();
            return;
        }

        // Try place crystal
        if (hit instanceof BlockHitResult bhr) {
            var block = mc.world.getBlockState(bhr.getBlockPos()).getBlock();
            if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
                BlockPos above = bhr.getBlockPos().up();
                if (!mc.world.getBlockState(above).isAir()) return;

                boolean hasCrystal = mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL;
                int savedSlot = mc.player.getInventory().selectedSlot;

                if (!hasCrystal && (Boolean) silentSwap.getValue()) {
                    for (int i = 0; i < 9; i++) {
                        if (mc.player.getInventory().getStack(i).getItem() == Items.END_CRYSTAL) {
                            mc.player.getInventory().selectedSlot = i;
                            hasCrystal = true;
                            break;
                        }
                    }
                }

                if (hasCrystal) {
                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
                    lastPlaced = bhr.getBlockPos();
                    if ((Boolean) silentSwap.getValue())
                        mc.player.getInventory().selectedSlot = savedSlot;
                    timer.reset();
                }
            }
        }
    }
}

