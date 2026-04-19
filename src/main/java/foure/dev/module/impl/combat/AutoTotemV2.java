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
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/**
 * AutoTotem v2 — automatically re-equips totem in offhand after popping.
 */
@ModuleInfo(
    name = "AutoTotem v2",
    category = Category.COMBAT,
    desc = "Automatically re-totems when popped"
)
public class AutoTotemV2 extends Function {

    private final NumberSetting  delay      = new NumberSetting("Delay",     this, 50.0, 0.0, 300.0, 1.0);
    private final NumberSetting  totemSlot  = new NumberSetting("Totem Slot",this, 9.0,  1.0, 9.0,   1.0);
    private final BooleanSetting openInv    = new BooleanSetting("Open Inventory", true);

    private final TimerUtil timer = new TimerUtil();
    private boolean needsTotem = false;

    public AutoTotemV2() {
        addSettings(new Setting[]{ delay, totemSlot, openInv });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        needsTotem = false;
        timer.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        // Check if offhand is missing totem
        boolean hasOffhand = mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
        if (hasOffhand) { needsTotem = false; return; }

        needsTotem = true;
        if (!timer.delay((float) delay.getValueFloat())) return;

        // Find totem in inventory (hotbar first, then inventory)
        int totemInvSlot = -1;
        // Hotbar
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                totemInvSlot = i; break;
            }
        }
        // Main inventory slots 9-35
        if (totemInvSlot == -1) {
            for (int i = 9; i < 36; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
                    totemInvSlot = i; break;
                }
            }
        }

        if (totemInvSlot == -1) return;

        // Swap slot to offhand (slot 40 = offhand)
        mc.interactionManager.clickSlot(
            mc.player.playerScreenHandler.syncId,
            totemInvSlot < 9 ? totemInvSlot + 36 : totemInvSlot,
            40,
            SlotActionType.SWAP,
            mc.player
        );

        timer.reset();
        needsTotem = false;
    }

    public boolean needsTotem() { return needsTotem; }
}

