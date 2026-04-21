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
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

@ModuleInfo(
    name = "AutoWindCharge",
    category = Category.COMBAT,
    desc = "Efectueaza sariturile wind charge + mace automat."
)
public class AutoWindCharge extends Function {

    private final BooleanSetting autoJump = new BooleanSetting("Auto Jump", this, true);
    private final NumberSetting jumpDelay = new NumberSetting("Jump Delay (ms)", this, 150.0, 0.0, 500.0, 10.0);
    private final BooleanSetting autoCrouch = new BooleanSetting("Auto Crouch", this, true);
    private final NumberSetting crouchDelay = new NumberSetting("Crouch Delay (ms)", this, 150.0, 0.0, 500.0, 10.0);
    private final NumberSetting preparationDelay = new NumberSetting("Prep Delay (ms)", this, 0.0, 0.0, 1000.0, 10.0);

    private State currentState = State.SEARCHING;
    private int chargeSlot = -1;
    private int originalSlot = -1;
    private long throwTime = 0L;
    private long enableTime = 0L;

    public AutoWindCharge() {
        this.addSettings(new Setting[]{
            autoJump, jumpDelay, autoCrouch, crouchDelay, preparationDelay
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentState = State.SEARCHING;
        chargeSlot = -1;
        originalSlot = -1;
        throwTime = 0L;
        enableTime = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.options != null) {
            mc.options.jumpKey.setPressed(false);
            mc.options.sneakKey.setPressed(false);
        }
        if (originalSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = originalSlot;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) { toggle(); return; }

        switch (currentState) {
            case SEARCHING -> {
                chargeSlot = findWindChargeSlot();
                if (chargeSlot == -1) { toggle(); return; }
                originalSlot = mc.player.getInventory().selectedSlot;
                currentState = State.ROTATING;
            }
            case ROTATING -> {
                boolean delayPassed = (System.currentTimeMillis() - enableTime) >= preparationDelay.getValue();
                // Look down — in foure.dev there's no rotation manager exposed so we set pitch directly
                mc.player.setPitch(89.9f);
                if (delayPassed) currentState = State.SWAPPING;
            }
            case SWAPPING -> {
                mc.player.getInventory().selectedSlot = chargeSlot;
                currentState = State.THROWING;
            }
            case THROWING -> {
                // Trigger use item
                mc.options.useKey.setPressed(true);
                throwTime = System.currentTimeMillis();
                currentState = State.WAITING_FOR_ACTION;
            }
            case WAITING_FOR_ACTION -> {
                mc.options.useKey.setPressed(false);
                currentState = State.EXECUTING_ACTION;
            }
            case EXECUTING_ACTION -> {
                long elapsed = System.currentTimeMillis() - throwTime;
                if ((Boolean) autoJump.getValue() && elapsed >= jumpDelay.getValue() && mc.player.isOnGround()) {
                    mc.options.jumpKey.setPressed(true);
                }
                if ((Boolean) autoCrouch.getValue() && elapsed >= crouchDelay.getValue()) {
                    mc.options.sneakKey.setPressed(true);
                }
                double maxDelay = 0.0;
                if ((Boolean) autoJump.getValue()) maxDelay = Math.max(maxDelay, jumpDelay.getValue());
                if ((Boolean) autoCrouch.getValue()) maxDelay = Math.max(maxDelay, crouchDelay.getValue());
                if (elapsed > maxDelay + 50.0) currentState = State.CLEANUP;
            }
            case CLEANUP -> {
                mc.player.getInventory().selectedSlot = originalSlot;
                mc.options.sneakKey.setPressed(false);
                mc.options.jumpKey.setPressed(false);
                currentState = State.FINISHING;
            }
            case FINISHING -> toggle();
        }
    }

    private int findWindChargeSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.WIND_CHARGE) return i;
        }
        return -1;
    }

    private enum State {
        SEARCHING, ROTATING, SWAPPING, THROWING, WAITING_FOR_ACTION, EXECUTING_ACTION, CLEANUP, FINISHING
    }
}
