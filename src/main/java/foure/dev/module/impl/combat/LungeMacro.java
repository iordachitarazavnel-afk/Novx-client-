package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;

@ModuleInfo(
    name = "LungeMacro",
    category = Category.COMBAT,
    desc = "Schimba automat la sulita lunge si ataca."
)
public class LungeMacro extends Function {

    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", this, true);
    private final BooleanSetting randomization = new BooleanSetting("Randomization", this, false);
    private final NumberSetting minRandom = new NumberSetting("Min Random", this, 0.0, 0.0, 10.0, 1.0);
    private final NumberSetting maxRandom = new NumberSetting("Max Random", this, 1.0, 0.0, 10.0, 1.0);

    private boolean isSwapping = false;
    private int originalSlot = -1;
    private int swapTicks = 0;
    private int targetTicks = 3;

    public LungeMacro() {
        this.addSettings(new Setting[]{swapBack, randomization, minRandom, maxRandom});
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (fullNullCheck()) { toggle(); return; }

        originalSlot = mc.player.getInventory().selectedSlot;
        targetTicks = 3;
        if ((Boolean) randomization.getValue()) {
            targetTicks += (int)(Math.random() * 2.0);
        }

        int lungeSlot = findLungeSlot();
        if (lungeSlot == -1) { toggle(); return; }

        isSwapping = true;
        swapTicks = 0;
        mc.player.getInventory().selectedSlot = lungeSlot;

        if (mc.player.getAttackCooldownProgress(0f) >= 1.0f) {
            mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            swapTicks = 1;
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        isSwapping = false;
        originalSlot = -1;
        swapTicks = 0;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isSwapping || fullNullCheck()) return;
        ++swapTicks;

        if (swapTicks == 1) {
            if (mc.player.getAttackCooldownProgress(0f) >= 1.0f && mc.targetedEntity != null) {
                mc.interactionManager.attackEntity(mc.player, mc.targetedEntity);
                mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
            }
            int lungeSlot = findLungeSlot();
            if (lungeSlot != -1) {
                mc.player.getInventory().selectedSlot = lungeSlot;
            } else {
                toggle();
            }
            return;
        }

        if (swapTicks >= targetTicks) {
            if ((Boolean) swapBack.getValue() && originalSlot != -1) {
                mc.player.getInventory().selectedSlot = originalSlot;
            }
            toggle();
        }
    }

    private int findLungeSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (isLungeSpear(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private boolean isLungeSpear(ItemStack stack) {
        if (stack.isEmpty()) return false;
        // Check if it's a trident (spear) by item type
        boolean isSpear = stack.getItem() == Items.TRIDENT ||
            stack.getName().getString().toLowerCase().contains("spear");
        if (!isSpear) return false;

        // Check for Lunge enchantment by name
        ItemEnchantmentsComponent enchantments = stack.get(DataComponentTypes.ENCHANTMENTS);
        if (enchantments == null) return false;

        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            String id = entry.getKey().map(k -> k.getValue().toString()).orElse("").toLowerCase();
            String name = entry.value().description().getString().toLowerCase();
            if (id.contains("lunge") || name.contains("lunge")) return true;
        }
        return false;
    }
}
