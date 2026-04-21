package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ArmorMaterials;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(
    name = "AutoElytra",
    category = Category.COMBAT,
    desc = "Schimba automat elytra si pieptarul in jurul ferestrelor de mace."
)
public class AutoElytra extends Function {

    private final BooleanSetting randomization = new BooleanSetting("Randomization", this, false);
    private final NumberSetting randomMinDelay = new NumberSetting("Min Random (ms)", this, 0.0, 0.0, 500.0, 10.0);
    private final NumberSetting randomMaxDelay = new NumberSetting("Max Random (ms)", this, 25.0, 0.0, 500.0, 10.0);
    private final BooleanSetting inAir = new BooleanSetting("In Air", this, false);
    private final BooleanSetting disableOnGround = new BooleanSetting("Disable on Ground", this, true);
    private final NumberSetting reequipDelay = new NumberSetting("Reequip Delay (ms)", this, 100.0, 0.0, 1000.0, 10.0);
    private final NumberSetting heightDiff = new NumberSetting("Height Diff", this, 4.0, 1.0, 10.0, 0.5);
    private final NumberSetting heightRange = new NumberSetting("Height Range", this, 10.0, 1.0, 50.0, 1.0);
    private final NumberSetting preHitRange = new NumberSetting("Mace Pre-Hit Range", this, 3.4, 2.5, 5.0, 0.1);
    private final NumberSetting minDropDistance = new NumberSetting("Min Drop Dist", this, 1.5, 0.5, 5.0, 0.5);

    private boolean swapInProgress = false;
    private boolean swapTargetElytra = false;
    private int swapOriginalSlot = -1;
    private int swapWaitTicks = 0;
    private long swapUseAt = 0L;
    private boolean swapUseSent = false;
    private boolean queuedSwap = false;
    private boolean queuedSwapTargetElytra = false;
    private long queuedSwapAt = 0L;
    private int airTicks = 0;
    private double highestY = 0.0;
    private boolean wasOnGround = true;
    private long suppressChestplateUntil = 0L;
    private long maceReequipAt = 0L;

    private static final int MAX_WAIT_TICKS = 10;
    private static final int MIN_AIR_TICKS = 4;
    private static final double IN_AIR_MIN_GROUND_DIST = 2.0;

    public AutoElytra() {
        this.addSettings(new Setting[]{
            randomization, randomMinDelay, randomMaxDelay,
            inAir, disableOnGround, reequipDelay,
            heightDiff, heightRange, preHitRange, minDropDistance
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetSwapState();
        clearQueuedSwap();
        suppressChestplateUntil = 0L;
        maceReequipAt = 0L;
        if (mc.player != null) {
            highestY = mc.player.getY();
            wasOnGround = mc.player.isOnGround();
        }
        airTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        finishSwap();
        clearQueuedSwap();
        airTicks = 0;
        suppressChestplateUntil = 0L;
        maceReequipAt = 0L;
    }

    // Chiamato da AutoMace quando colpisce — se vuoi integrarlo, chiama questo metodo
    public void onMaceHit() {
        if (fullNullCheck()) return;
        scheduleMaceReequip();
        suppressChestplateUntil = System.currentTimeMillis() + (long) reequipDelay.getValue() + getRandomMaxDelay() + 250L;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        updateAirTracking();
        processPendingEventReequips();

        boolean forceElytraForAir = (Boolean) inAir.getValue() && shouldForceElytraInAir();

        if (forceElytraForAir) {
            if (!isWearingElytra()) {
                if (swapInProgress && !swapTargetElytra) finishSwap();
                queueSwap(true, 0L);
            }
            if (queuedSwap && !queuedSwapTargetElytra) clearQueuedSwap();
        } else if ((Boolean) disableOnGround.getValue() && mc.player.isOnGround()) {
            if (isWearingElytra()) queueSwap(false, 0L);
            if (queuedSwap && queuedSwapTargetElytra) clearQueuedSwap();
        } else if (shouldSwapToChestplateForCombat()) {
            queueSwap(false, 0L);
        }

        if (!swapInProgress) tryStartQueuedSwap();
        if (swapInProgress) processSwap();
    }

    // ─── swap logic ───────────────────────────────────────────────────────────

    private void scheduleMaceReequip() {
        maceReequipAt = System.currentTimeMillis() + (long) reequipDelay.getValue();
    }

    private void processPendingEventReequips() {
        long now = System.currentTimeMillis();
        if (maceReequipAt > 0L && now >= maceReequipAt) {
            queueSwap(true, 0L);
            maceReequipAt = 0L;
        }
    }

    private boolean shouldSwapToChestplateForCombat() {
        if (!isWearingElytra()) return false;
        if (System.currentTimeMillis() < suppressChestplateUntil) return false;
        if (mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing()) return false;
        if (airTicks < MIN_AIR_TICKS) return false;
        double manualDrop = getManualDropDistance();
        if (manualDrop < minDropDistance.getValueFloat()) return false;
        if (mc.player.getVelocity().y > -0.03) return false;

        PlayerEntity nearest = findNearestEnemy(Math.max(heightRange.getValueFloat(), preHitRange.getValueFloat()));
        if (nearest == null) return false;

        double diffY = mc.player.getY() - nearest.getY();
        if (diffY <= 0.0) return false;

        double horizontalDist = horizontalDistanceTo(nearest);
        double totalDist = mc.player.distanceTo(nearest);
        boolean higherThanEnemy = diffY >= heightDiff.getValueFloat() && horizontalDist <= heightRange.getValueFloat();
        boolean almostMaceHitRange = diffY >= 1.0 && totalDist <= preHitRange.getValueFloat();
        return higherThanEnemy || almostMaceHitRange;
    }

    private void queueSwap(boolean toElytra, long baseDelayMs) {
        queuedSwap = true;
        queuedSwapTargetElytra = toElytra;
        queuedSwapAt = System.currentTimeMillis() + Math.max(0L, baseDelayMs);
    }

    private void clearQueuedSwap() {
        queuedSwap = false;
        queuedSwapTargetElytra = false;
        queuedSwapAt = 0L;
    }

    private void tryStartQueuedSwap() {
        if (!queuedSwap || mc.player == null) return;
        if (System.currentTimeMillis() < queuedSwapAt) return;
        if (queuedSwapTargetElytra && isWearingElytra()) { clearQueuedSwap(); return; }
        if (!queuedSwapTargetElytra && !isWearingElytra() && isChestplate(mc.player.getEquippedStack(EquipmentSlot.CHEST))) { clearQueuedSwap(); return; }
        startSwap(queuedSwapTargetElytra);
        clearQueuedSwap();
    }

    private void startSwap(boolean toElytra) {
        if (mc.player == null || swapInProgress) return;
        int targetSlot = toElytra ? findElytraSlot() : findChestplateSlot();
        if (targetSlot == -1) return;
        swapOriginalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = targetSlot;
        swapTargetElytra = toElytra;
        swapWaitTicks = 0;
        swapUseSent = false;
        swapUseAt = System.currentTimeMillis() + getRandomExtraDelay();
        swapInProgress = true;
    }

    private void processSwap() {
        if (mc.player == null) { finishSwap(); return; }
        if (!swapUseSent) {
            if (System.currentTimeMillis() < swapUseAt) return;
            mc.options.useKey.setPressed(true);
            // trigger use via key timesPressed — same as slither client
            ((net.minecraft.client.option.StickyKeyBinding) mc.options.useKey).timesPressed++;
            swapUseSent = true;
            return;
        }
        ++swapWaitTicks;
        ItemStack currentChest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean nowElytra = currentChest.getItem() instanceof ElytraItem;
        boolean swapComplete = (swapTargetElytra && nowElytra) || (!swapTargetElytra && !nowElytra && isChestplate(currentChest));
        if (swapComplete || swapWaitTicks >= MAX_WAIT_TICKS) {
            finishSwap();
        }
    }

    private void finishSwap() {
        if (mc.player != null && swapOriginalSlot != -1) {
            mc.player.getInventory().selectedSlot = swapOriginalSlot;
        }
        resetSwapState();
    }

    private void resetSwapState() {
        swapInProgress = false;
        swapTargetElytra = false;
        swapOriginalSlot = -1;
        swapWaitTicks = 0;
        swapUseAt = 0L;
        swapUseSent = false;
    }

    // ─── air tracking ─────────────────────────────────────────────────────────

    private void updateAirTracking() {
        if (mc.player == null) return;
        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            highestY = mc.player.getY();
            airTicks = 0;
        } else if (wasOnGround) {
            highestY = mc.player.getY();
            airTicks = 1;
        } else {
            highestY = Math.max(highestY, mc.player.getY());
            ++airTicks;
        }
        wasOnGround = onGround;
    }

    private double getManualDropDistance() {
        if (mc.player == null) return 0.0;
        return Math.max(0.0, highestY - mc.player.getY());
    }

    private boolean shouldForceElytraInAir() {
        if (fullNullCheck()) return false;
        if (mc.player.isOnGround() || mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing()) return false;
        return getManualDropDistance() >= IN_AIR_MIN_GROUND_DIST;
    }

    // ─── item finders ─────────────────────────────────────────────────────────

    private boolean isWearingElytra() {
        return mc.player != null && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() instanceof ElytraItem;
    }

    private int findElytraSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof ElytraItem) return i;
        }
        return -1;
    }

    private int findChestplateSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isChestplate(stack)) return i;
        }
        return -1;
    }

    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof ElytraItem) return false;
        if (stack.getItem() instanceof ArmorItem ai) {
            return ai.getSlotType() == EquipmentSlot.CHEST;
        }
        return false;
    }

    private PlayerEntity findNearestEnemy(double maxDistance) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
            .filter(p -> mc.player.distanceTo(p) <= maxDistance)
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    private double horizontalDistanceTo(PlayerEntity target) {
        double dx = mc.player.getX() - target.getX();
        double dz = mc.player.getZ() - target.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private long getRandomExtraDelay() {
        if (!(Boolean) randomization.getValue()) return 0L;
        long min = Math.max(0L, (long) randomMinDelay.getValue());
        long max = Math.max(min, (long) randomMaxDelay.getValue());
        if (min == max) return min;
        return ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    private long getRandomMaxDelay() {
        return (long) randomMaxDelay.getValue();
    }
}
