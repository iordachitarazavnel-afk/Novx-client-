package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Comparator;
import java.util.concurrent.ThreadLocalRandom;

@ModuleInfo(
    name = "AutoElytra",
    category = Category.COMBAT,
    desc = "Schimba automat elytra si pieptarul in jurul ferestrelor de mace."
)
public class AutoElytra extends Function {

    private final NumberSetting randomMinDelay  = new NumberSetting("Min Random (ms)", this, 0.0,   0.0,  500.0, 10.0);
    private final NumberSetting randomMaxDelay  = new NumberSetting("Max Random (ms)", this, 25.0,  0.0,  500.0, 10.0);
    private final BooleanSetting randomization  = new BooleanSetting("Randomization",  this, false);
    private final BooleanSetting inAir          = new BooleanSetting("In Air",          this, false);
    private final BooleanSetting disableOnGround= new BooleanSetting("Disable on Ground",this,true);
    private final NumberSetting reequipDelay    = new NumberSetting("Reequip Delay (ms)",this,100.0, 0.0, 1000.0,10.0);
    private final NumberSetting heightDiff      = new NumberSetting("Height Diff",      this, 4.0,  1.0,  10.0,  0.5);
    private final NumberSetting heightRange     = new NumberSetting("Height Range",     this,10.0,  1.0,  50.0,  1.0);
    private final NumberSetting preHitRange     = new NumberSetting("Mace Pre-Hit Range",this,3.4,  2.5,   5.0,  0.1);
    private final NumberSetting minDropDistance = new NumberSetting("Min Drop Dist",    this, 1.5,  0.5,   5.0,  0.5);

    private boolean swapInProgress        = false;
    private boolean swapTargetElytra      = false;
    private int     swapOriginalSlot      = -1;
    private int     swapWaitTicks         = 0;
    private long    swapUseAt             = 0L;
    private boolean swapUseSent           = false;
    private boolean queuedSwap            = false;
    private boolean queuedSwapTargetElytra= false;
    private long    queuedSwapAt          = 0L;
    private int     airTicks              = 0;
    private double  highestY              = 0.0;
    private boolean wasOnGround           = true;
    private long    suppressChestUntil    = 0L;
    private long    maceReequipAt         = 0L;

    private static final int    MAX_WAIT_TICKS         = 10;
    private static final int    MIN_AIR_TICKS          = 4;
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
        suppressChestUntil = 0L;
        maceReequipAt      = 0L;
        if (mc.player != null) {
            highestY    = mc.player.getY();
            wasOnGround = mc.player.isOnGround();
        }
        airTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        finishSwap();
        clearQueuedSwap();
        airTicks           = 0;
        suppressChestUntil = 0L;
        maceReequipAt      = 0L;
    }

    /** Apelat de AutoMace dupa lovire */
    public void onMaceHit() {
        if (fullNullCheck()) return;
        maceReequipAt      = System.currentTimeMillis() + reequipDelay.getValueLong();
        suppressChestUntil = System.currentTimeMillis()
            + reequipDelay.getValueLong()
            + getRandomMaxDelay() + 250L;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        updateAirTracking();
        processPendingReequips();

        boolean forceElytraInAir = (Boolean) inAir.getValue() && shouldForceElytraInAir();

        if (forceElytraInAir) {
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
        if (swapInProgress)  processSwap();
    }

    // ── swap ──────────────────────────────────────────────────────────────────

    private void processPendingReequips() {
        long now = System.currentTimeMillis();
        if (maceReequipAt > 0L && now >= maceReequipAt) {
            queueSwap(true, 0L);
            maceReequipAt = 0L;
        }
    }

    private boolean shouldSwapToChestplateForCombat() {
        if (!isWearingElytra()) return false;
        if (System.currentTimeMillis() < suppressChestUntil) return false;
        if (mc.player.isOnGround() || mc.player.isSubmergedInWater()
                || mc.player.isInLava() || mc.player.isClimbing()) return false;
        if (airTicks < MIN_AIR_TICKS) return false;
        if (getManualDropDistance() < minDropDistance.getValueFloat()) return false;
        if (mc.player.getVelocity().y > -0.03) return false;

        PlayerEntity nearest = findNearestEnemy(
            Math.max(heightRange.getValueFloat(), preHitRange.getValueFloat()));
        if (nearest == null) return false;

        double diffY     = mc.player.getY() - nearest.getY();
        if (diffY <= 0.0) return false;

        double hDist     = horizontalDistanceTo(nearest);
        double totalDist = mc.player.distanceTo(nearest);
        boolean higher   = diffY >= heightDiff.getValueFloat() && hDist     <= heightRange.getValueFloat();
        boolean closeHit = diffY >= 1.0                        && totalDist <= preHitRange.getValueFloat();
        return higher || closeHit;
    }

    private void queueSwap(boolean toElytra, long baseMs) {
        queuedSwap             = true;
        queuedSwapTargetElytra = toElytra;
        queuedSwapAt           = System.currentTimeMillis() + Math.max(0L, baseMs);
    }

    private void clearQueuedSwap() {
        queuedSwap             = false;
        queuedSwapTargetElytra = false;
        queuedSwapAt           = 0L;
    }

    private void tryStartQueuedSwap() {
        if (!queuedSwap || mc.player == null) return;
        if (System.currentTimeMillis() < queuedSwapAt) return;
        if ( queuedSwapTargetElytra &&  isWearingElytra()) { clearQueuedSwap(); return; }
        if (!queuedSwapTargetElytra && !isWearingElytra()
                && isChestplate(mc.player.getEquippedStack(EquipmentSlot.CHEST))) {
            clearQueuedSwap(); return;
        }
        startSwap(queuedSwapTargetElytra);
        clearQueuedSwap();
    }

    private void startSwap(boolean toElytra) {
        if (mc.player == null || swapInProgress) return;
        int slot = toElytra ? findElytraSlot() : findChestplateSlot();
        if (slot == -1) return;
        swapOriginalSlot = mc.player.getInventory().selectedSlot;
        mc.player.getInventory().selectedSlot = slot;
        swapTargetElytra = toElytra;
        swapWaitTicks    = 0;
        swapUseSent      = false;
        swapUseAt        = System.currentTimeMillis() + getRandomExtraDelay();
        swapInProgress   = true;
    }

    private void processSwap() {
        if (mc.player == null) { finishSwap(); return; }
        if (!swapUseSent) {
            if (System.currentTimeMillis() < swapUseAt) return;
            mc.options.useKey.setPressed(true);
            swapUseSent = true;
            return;
        }
        ++swapWaitTicks;
        mc.options.useKey.setPressed(false);
        ItemStack chest   = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        boolean nowElytra = chest.isOf(Items.ELYTRA);
        boolean done      = (swapTargetElytra && nowElytra)
            || (!swapTargetElytra && !nowElytra && isChestplate(chest));
        if (done || swapWaitTicks >= MAX_WAIT_TICKS) finishSwap();
    }

    private void finishSwap() {
        if (mc.player != null && swapOriginalSlot != -1)
            mc.player.getInventory().selectedSlot = swapOriginalSlot;
        resetSwapState();
    }

    private void resetSwapState() {
        swapInProgress   = false;
        swapTargetElytra = false;
        swapOriginalSlot = -1;
        swapWaitTicks    = 0;
        swapUseAt        = 0L;
        swapUseSent      = false;
    }

    // ── air tracking ─────────────────────────────────────────────────────────

    private void updateAirTracking() {
        if (mc.player == null) return;
        boolean onGround = mc.player.isOnGround();
        if (onGround) {
            highestY = mc.player.getY(); airTicks = 0;
        } else if (wasOnGround) {
            highestY = mc.player.getY(); airTicks = 1;
        } else {
            highestY = Math.max(highestY, mc.player.getY()); ++airTicks;
        }
        wasOnGround = onGround;
    }

    private double getManualDropDistance() {
        return mc.player == null ? 0.0 : Math.max(0.0, highestY - mc.player.getY());
    }

    private boolean shouldForceElytraInAir() {
        if (fullNullCheck()) return false;
        if (mc.player.isOnGround() || mc.player.isSubmergedInWater()
                || mc.player.isInLava() || mc.player.isClimbing()) return false;
        return getManualDropDistance() >= IN_AIR_MIN_GROUND_DIST;
    }

    // ── item helpers ─────────────────────────────────────────────────────────

    private boolean isWearingElytra() {
        return mc.player != null
            && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA);
    }

    private int findElytraSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++)
            if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA)) return i;
        return -1;
    }

    private int findChestplateSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++)
            if (isChestplate(mc.player.getInventory().getStack(i))) return i;
        return -1;
    }

    /**
     * Chestplate check via EquippableComponent — Yarn 1.21.x
     * Nu foloseste ArmorItem sau ArmorMaterials (nu exista in aceasta versiune).
     */
    private boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.ELYTRA)) return false;
        EquippableComponent eq = stack.get(DataComponentTypes.EQUIPPABLE);
        return eq != null && eq.slot() == EquipmentSlot.CHEST;
    }

    private PlayerEntity findNearestEnemy(double maxDist) {
        if (mc.world == null || mc.player == null) return null;
        return mc.world.getPlayers().stream()
            .filter(p -> p != mc.player && p.isAlive() && !p.isSpectator())
            .filter(p -> mc.player.distanceTo(p) <= maxDist)
            .min(Comparator.comparingDouble(p -> mc.player.distanceTo(p)))
            .orElse(null);
    }

    private double horizontalDistanceTo(PlayerEntity t) {
        double dx = mc.player.getX() - t.getX();
        double dz = mc.player.getZ() - t.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private long getRandomExtraDelay() {
        if (!(Boolean) randomization.getValue()) return 0L;
        long min = Math.max(0L, randomMinDelay.getValueLong());
        long max = Math.max(min, randomMaxDelay.getValueLong());
        return min == max ? min : ThreadLocalRandom.current().nextLong(min, max + 1L);
    }

    private long getRandomMaxDelay() {
        return randomMaxDelay.getValueLong();
    }
}
