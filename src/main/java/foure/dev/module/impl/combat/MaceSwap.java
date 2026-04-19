package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.item.Items;

/**
 * MaceSwap — switches to a mace when pressing attack, optionally switches back.
 */
@ModuleInfo(
    name = "MaceSwap",
    category = Category.COMBAT,
    desc = "Switches to a mace when attacking"
)
public class MaceSwap extends Function {

    private final BooleanSetting onlySword   = new BooleanSetting("Only Sword",    false);
    private final BooleanSetting onlyAxe     = new BooleanSetting("Only Axe",      false);
    private final BooleanSetting switchBack  = new BooleanSetting("Switch Back",   true);
    private final NumberSetting  switchDelay = new NumberSetting("Switch Delay",   this, 0.0, 0.0, 20.0, 1.0);
    private final BooleanSetting onlyAirborne = new BooleanSetting("Only Airborne", false);

    private boolean switching  = false;
    private int     prevSlot   = -1;
    private int     delayTimer = 0;

    public MaceSwap() {
        addSettings(new Setting[]{ onlySword, onlyAxe, switchBack, switchDelay, onlyAirborne });
    }

    @Override
    public void onEnable()  { super.onEnable();  resetState(); }
    @Override
    public void onDisable() { super.onDisable(); resetState(); }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        if (switching) {
            if (delayTimer > 0) { delayTimer--; return; }
            if ((Boolean) switchBack.getValue() && prevSlot != -1)
                mc.player.getInventory().selectedSlot = prevSlot;
            resetState();
            return;
        }

        if ((Boolean) onlyAirborne.getValue() && mc.player.isOnGround()) return;
        if (!mc.options.attackKey.isPressed()) return;

        var held = mc.player.getMainHandStack();
        boolean isSword = held.isOf(Items.NETHERITE_SWORD) || held.isOf(Items.DIAMOND_SWORD) || held.isOf(Items.IRON_SWORD) || held.isOf(Items.GOLDEN_SWORD) || held.isOf(Items.STONE_SWORD) || held.isOf(Items.WOODEN_SWORD);
        boolean isAxe   = held.isOf(Items.NETHERITE_AXE)  || held.isOf(Items.DIAMOND_AXE)   || held.isOf(Items.IRON_AXE)  || held.isOf(Items.GOLDEN_AXE)  || held.isOf(Items.STONE_AXE)  || held.isOf(Items.WOODEN_AXE);

        if ((Boolean) onlySword.getValue() && !isSword) return;
        if ((Boolean) onlyAxe.getValue()   && !isAxe)  return;

        int maceSlot = findMace();
        if (maceSlot == -1 || maceSlot == mc.player.getInventory().selectedSlot) return;

        prevSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = maceSlot;
        switching  = true;
        delayTimer = (int) switchDelay.getValueFloat();
    }

    private int findMace() {
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(Items.MACE)) return i;
        return -1;
    }

    private void resetState() { switching = false; prevSlot = -1; delayTimer = 0; }
}

