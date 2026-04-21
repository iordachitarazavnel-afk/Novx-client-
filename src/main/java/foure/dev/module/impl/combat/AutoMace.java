package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.EventRender3D;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.render.Render3D;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ShieldItem;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.tag.ItemTags;

import java.awt.Color;

@ModuleInfo(
    name = "AutoMace",
    category = Category.COMBAT,
    desc = "Automat loveste cu mace-ul dupa cadere."
)
public class AutoMace extends Function {

    private final NumberSetting swingRange = new NumberSetting("Swing Range", this, 3.0, 2.5, 3.0, 0.1);
    private final NumberSetting aimRange = new NumberSetting("Aim Range", this, 15.0, 0.0, 20.0, 0.5);
    private final NumberSetting minFallDist = new NumberSetting("Min Fall Dist", this, 1.5, 0.0, 5.0, 0.5);
    private final NumberSetting cooldownMs = new NumberSetting("Cooldown (ms)", this, 500.0, 100.0, 2000.0, 50.0);
    private final NumberSetting maceSwapDelayMs = new NumberSetting("Mace Swap Delay (ms)", this, 1.0, 0.0, 100.0, 1.0);
    private final BooleanSetting autoSwitch = new BooleanSetting("Auto Switch", this, true);
    private final BooleanSetting swapBack = new BooleanSetting("Swap Back", this, true);
    private final BooleanSetting stunSlam = new BooleanSetting("Stun Slam", this, true);
    private final BooleanSetting ignoreFriends = new BooleanSetting("Ignore Friends", this, true);
    private final ModeSetting aimMode = new ModeSetting("Aim Mode", this, "Strict", "Strict", "Loose", "Horizontal");

    private PlayerEntity currentTarget = null;
    private int maceClicksLeft = 0;
    private int originalSlot = -1;
    private int preSequenceSlot = -1;
    private long lastComboTime = 0L;
    private long axeHitTime = 0L;
    private int resetTimer = 0;
    private double highestY = 0.0;
    private boolean wasOnGround = true;
    private boolean shouldAttackThisTick = false;
    private boolean shouldBreakShield = false;
    private boolean shouldMaceSmash = false;
    private int targetSlotForAttack = -1;

    public AutoMace() {
        this.addSettings(new Setting[]{
            swingRange, aimRange, minFallDist, cooldownMs, maceSwapDelayMs,
            autoSwitch, swapBack, stunSlam, ignoreFriends, aimMode
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        currentTarget = null;
        maceClicksLeft = 0;
        originalSlot = -1;
        preSequenceSlot = -1;
        lastComboTime = 0L;
        axeHitTime = 0L;
        resetTimer = 0;
        highestY = mc.player != null ? mc.player.getY() : 0.0;
        wasOnGround = true;
        shouldAttackThisTick = false;
        shouldBreakShield = false;
        shouldMaceSmash = false;
        targetSlotForAttack = -1;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        currentTarget = null;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        // Tick logic (was onTick)
        if (shouldBreakShield) {
            executeShieldBreak();
        } else if (shouldMaceSmash) {
            executeMaceSmash();
        } else if (shouldAttackThisTick) {
            executeAttack();
        }

        // Render logic (was onRender)
        shouldAttackThisTick = false;
        shouldBreakShield = false;
        shouldMaceSmash = false;
        targetSlotForAttack = -1;
        runMainLogic();
    }

    // ─── main logic ──────────────────────────────────────────────────────────

    private void runMainLogic() {
        if (fullNullCheck()) return;

        boolean isOnGroundNow = mc.player.isOnGround();
        highestY = isOnGroundNow ? mc.player.getY() : Math.max(highestY, mc.player.getY());
        double manualFallDist = Math.max(0.0, highestY - mc.player.getY());
        wasOnGround = isOnGroundNow;

        int bestMaceSlot = findBestMace();
        boolean isHoldingMace = mc.player.getMainHandStack().getItem() instanceof MaceItem;
        boolean canUseMace = isHoldingMace || ((Boolean) autoSwitch.getValue() && bestMaceSlot != -1);

        if (!canUseMace) { stopAiming(); return; }
        if (resetTimer > 0) { handleResetSequence(); return; }
        if (maceClicksLeft > 0) { calculateMaceLogic(); return; }
        if ((System.currentTimeMillis() - lastComboTime) < cooldownMs.getValueLong()) return;

        currentTarget = findTarget();
        if (currentTarget == null) { stopAiming(); return; }

        boolean gameFalling = mc.player.fallDistance >= minFallDist.getValueFloat();
        boolean manualFalling = manualFallDist >= minFallDist.getValueFloat();
        boolean isFalling = gameFalling || manualFalling;

        if (!isFalling && minFallDist.getValueFloat() > 0.1f) { stopAiming(); return; }

        boolean isBlocking = isTargetBlocking(currentTarget);
        boolean canStunSlam = (Boolean) stunSlam.getValue() && isBlocking;

        if (canStunSlam) {
            calculateStunSlam();
        } else {
            calculateDirectMaceLogic();
        }
    }

    private void calculateStunSlam() {
        if (mc.player.distanceTo(currentTarget) > aimRange.getValueFloat()) {
            stopAndReset(); return;
        }
        if (canExecuteAttack()) {
            int axeSlot = findAxe();
            int maceSlot = findBestMace();
            if (axeSlot != -1 && maceSlot != -1) {
                if (preSequenceSlot == -1) preSequenceSlot = mc.player.getInventory().selectedSlot;
                shouldBreakShield = true;
                targetSlotForAttack = axeSlot;
                originalSlot = maceSlot;
            }
        }
    }

    private void executeShieldBreak() {
        if (currentTarget == null) return;
        if (!syncToAttackSlot()) return;
        if (!canExecuteAttack()) return;
        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        maceClicksLeft = 1;
        axeHitTime = System.currentTimeMillis();
    }

    private void calculateMaceLogic() {
        if (currentTarget == null || !currentTarget.isAlive() ||
            mc.player.distanceTo(currentTarget) > aimRange.getValueFloat()) {
            swapBackToPreSequence();
            maceClicksLeft = 0;
            originalSlot = -1;
            stopAiming();
            return;
        }
        long timeSinceAxe = System.currentTimeMillis() - axeHitTime;
        if (timeSinceAxe < maceSwapDelayMs.getValueLong()) return;
        if (timeSinceAxe > 1500L) {
            swapBackToPreSequence();
            maceClicksLeft = 0;
            originalSlot = -1;
            stopAiming();
            return;
        }
        if (canExecuteAttack()) {
            shouldMaceSmash = true;
            targetSlotForAttack = originalSlot;
        }
    }

    private void executeMaceSmash() {
        if (!syncToAttackSlot()) return;
        if (!canExecuteAttack()) return;
        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        maceClicksLeft = 0;
        resetTimer = 8;
        lastComboTime = System.currentTimeMillis();
    }

    private void calculateDirectMaceLogic() {
        if (currentTarget == null || !currentTarget.isAlive() ||
            mc.player.distanceTo(currentTarget) > aimRange.getValueFloat()) {
            stopAiming(); return;
        }
        if (canExecuteAttack()) {
            int maceSlot = findBestMace();
            if (maceSlot != -1) {
                if (preSequenceSlot == -1) preSequenceSlot = mc.player.getInventory().selectedSlot;
                shouldAttackThisTick = true;
                targetSlotForAttack = maceSlot;
            }
        }
    }

    private void executeAttack() {
        if (!syncToAttackSlot()) return;
        if (!canExecuteAttack()) return;
        mc.interactionManager.attackEntity(mc.player, currentTarget);
        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        lastComboTime = System.currentTimeMillis();
        resetTimer = 5;
    }

    private void handleResetSequence() {
        --resetTimer;
        if (resetTimer <= 0) {
            swapBackToPreSequence();
            stopAiming();
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private boolean canExecuteAttack() {
        if (fullNullCheck() || currentTarget == null) return false;
        if (mc.player.distanceTo(currentTarget) > swingRange.getValueFloat()) return false;
        return mc.player.getAttackCooldownProgress(0f) >= 1.0f;
    }

    private boolean isTargetBlocking(PlayerEntity target) {
        if (target == null) return false;
        if (target.isUsingItem()) {
            ItemStack active = target.getActiveItem();
            return !active.isEmpty() && active.getItem() instanceof ShieldItem;
        }
        return false;
    }

    private PlayerEntity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        PlayerEntity best = null;
        double bestDist = Double.MAX_VALUE;
        double range = aimRange.getValueFloat();
        for (PlayerEntity e : mc.world.getPlayers()) {
            if (e == mc.player) continue;
            if (!e.isAlive()) continue;
            double dist = mc.player.distanceTo(e);
            if (dist > range || dist >= bestDist) continue;
            bestDist = dist;
            best = e;
        }
        return best;
    }

    private int findBestMace() {
        int bestSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof MaceItem) { bestSlot = i; break; }
        }
        return bestSlot;
    }

    private int findAxe() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private boolean syncToAttackSlot() {
        if (mc.player == null) return false;
        if (!(Boolean) autoSwitch.getValue() || targetSlotForAttack < 0 || targetSlotForAttack > 8) return true;
        mc.player.getInventory().selectedSlot = targetSlotForAttack;
        return true;
    }

    private void swapBackToPreSequence() {
        if ((Boolean) swapBack.getValue() && (Boolean) autoSwitch.getValue() && preSequenceSlot >= 0 && preSequenceSlot < 9) {
            mc.player.getInventory().selectedSlot = preSequenceSlot;
        }
        preSequenceSlot = -1;
    }

    private void stopAiming() {
        currentTarget = null;
        maceClicksLeft = 0;
        shouldAttackThisTick = false;
        shouldBreakShield = false;
        shouldMaceSmash = false;
        targetSlotForAttack = -1;
        originalSlot = -1;
    }

    private void stopAndReset() {
        swapBackToPreSequence();
        maceClicksLeft = 0;
        originalSlot = -1;
        stopAiming();
    }
}
