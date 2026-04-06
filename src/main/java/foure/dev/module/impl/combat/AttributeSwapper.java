package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.entity.player.PlayerEntity;

@ModuleInfo(
    name = "AttributeSwapper",
    category = Category.COMBAT,
    desc = "Swaps to a target slot on attack"
)
public class AttributeSwapper extends Function {

    private final NumberSetting targetSlot = new NumberSetting("Target Slot", this, 3.0, 1.0, 9.0, 1.0);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", true);
    private final NumberSetting swapBackDelay = new NumberSetting("Swap Back Delay", this, 1.0, 1.0, 20.0, 1.0);

    private int prevSlot = -1;
    private int dDelay = 0;
    private boolean wasAttacking = false;

    public AttributeSwapper() {
        this.addSettings(new Setting[]{this.targetSlot, this.swapBack, this.swapBackDelay});
    }

    public void onEnable() {
        super.onEnable();
        this.prevSlot = -1;
        this.dDelay = 0;
        this.wasAttacking = false;
    }

    public void onDisable() {
        super.onDisable();
        if (this.prevSlot != -1 && mc.player != null) {
            mc.player.getInventory().selectedSlot = this.prevSlot;
        }
        this.prevSlot = -1;
        this.dDelay = 0;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!fullNullCheck() && mc.currentScreen == null) {

            // tick down swap-back delay
            if (this.dDelay > 0) {
                this.dDelay--;
                if (this.dDelay == 0 && this.prevSlot != -1) {
                    mc.player.getInventory().selectedSlot = this.prevSlot;
                    this.prevSlot = -1;
                }
            }

            boolean isAttacking = mc.options.attackKey.isPressed();

            if (isAttacking && !this.wasAttacking) {
                handleSwapLogic();
            }

            this.wasAttacking = isAttacking;
        }
    }

    private void handleSwapLogic() {
        if (mc.player == null || mc.world == null) return;

        // skip if targeted entity is blocking (shield check)
        if (mc.targetedEntity instanceof PlayerEntity target) {
            if (target.isBlocking()) return;
        }

        int currentSlot = mc.player.getInventory().selectedSlot;
        int targetIndex = (int) this.targetSlot.getValueFloat() - 1;

        if (currentSlot == targetIndex) return;

        if ((Boolean) this.swapBack.getValue() && this.dDelay <= 0) {
            this.prevSlot = currentSlot;
            this.dDelay = (int) this.swapBackDelay.getValueFloat();
        }

        mc.player.getInventory().selectedSlot = targetIndex;
    }
}
