package foure.dev.module.impl.combat.helper.attack;

import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.player.UsingItemEvent;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.util.Player.PlayerIntersectionUtil;
import foure.dev.util.Player.PlayerInventoryComponent;
import foure.dev.util.Player.PlayerInventoryUtil;
import foure.dev.util.Player.RaytracingUtil;
import foure.dev.util.Player.SimulatedPlayer;
import foure.dev.util.math.TimerUtil;
import foure.dev.util.others.Lisener.EventListener;
import foure.dev.util.wrapper.Wrapper;
import lombok.Generated;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;

public class AttackHandler implements Wrapper {
   private final TimerUtil attackTimer = new TimerUtil();
   private final TimerUtil shieldWatch = new TimerUtil();
   private final ClickScheduler clickScheduler = new ClickScheduler();
   private int count = 0;

   void tick() {
   }

   void onPacket(PacketEvent e) {
      Packet<?> packet = e.getPacket();
      if (packet instanceof HandSwingC2SPacket || packet instanceof UpdateSelectedSlotC2SPacket) {
         this.clickScheduler.recalculate();
      }

   }

   void onUsingItem(UsingItemEvent e) {
      if (e.getType() == -1 && !this.shieldWatch.finished(50.0D)) {
         e.cancel();
      }

   }

   void handleAttack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (this.canAttack(config, 1)) {
         this.preAttackEntity(config);
      }

      if (RaytracingUtil.rayTrace(config) && this.canAttack(config, 0)) {
         this.attackEntity(config);
      }

   }

   void preAttackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      if (config.isShouldUnPressShield() && mc.player.isUsingItem() && mc.player.getActiveItem().getItem().equals(Items.SHIELD)) {
         mc.interactionManager.stopUsingItem(mc.player);
         this.shieldWatch.reset();
      }

      if (!mc.player.isSwimming()) {
         mc.player.setSprinting(false);
      }

   }

   void attackEntity(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      this.attack(config);
      this.breakShield(config);
      this.attackTimer.reset();
      ++this.count;
   }

   private void breakShield(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      LivingEntity target = config.getTarget();
      Angle angleToPlayer = AngleUtil.fromVec3d(mc.player.getBoundingBox().getCenter().subtract(target.getEyePos()));
      boolean targetOnShield = target.isUsingItem() && target.getActiveItem().getItem().equals(Items.SHIELD);
      boolean angle = Math.abs(RotationController.computeAngleDifference(target.getYaw(), angleToPlayer.getYaw())) < 90.0F;
      Slot axe = PlayerInventoryUtil.getSlot((s) -> {
         return s.getStack().getItem() instanceof AxeItem;
      });
      if (config.isShouldBreakShield() && targetOnShield && axe != null && angle && PlayerInventoryComponent.script.isFinished()) {
         PlayerInventoryUtil.swapHand(axe, Hand.MAIN_HAND, false);
         PlayerInventoryUtil.closeScreen(true);
         this.attack(config);
         PlayerInventoryUtil.swapHand(axe, Hand.MAIN_HAND, false, true);
         PlayerInventoryUtil.closeScreen(true);
      }

   }

   private void attack(AttackPerpetrator.AttackPerpetratorConfigurable config) {
      mc.player.setSprinting(false);
      mc.interactionManager.attackEntity(mc.player, config.getTarget());
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private boolean isSprinting() {
      return EventListener.serverSprint && !mc.player.isGliding() && !mc.player.isTouchingWater();
   }

   public boolean canAttack(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
      for(int i = 0; i <= ticks; ++i) {
         if (this.canCrit(config, i)) {
            return true;
         }
      }

      return false;
   }

   public boolean canCrit(AttackPerpetrator.AttackPerpetratorConfigurable config, int ticks) {
      if (mc.player.isUsingItem() && !mc.player.getActiveItem().getItem().equals(Items.SHIELD) && config.isEatAndAttack()) {
         return false;
      } else {
         boolean isMace = mc.player.getMainHandStack().getItem() == Items.MACE;
         double heightDifference = mc.player.getY() - config.getTarget().getY();
         boolean isMaceSmash = isMace && heightDifference >= 2.0D;
         if (!isMaceSmash && !this.clickScheduler.isCooldownComplete(config.isUseDynamicCooldown(), ticks)) {
            return false;
         } else {
            SimulatedPlayer simulated = SimulatedPlayer.simulateLocalPlayer(ticks);
            return config.isOnlyCritical() && !this.hasMovementRestrictions(simulated) ? this.isPlayerInCriticalState(simulated, ticks) : true;
         }
      }
   }

   private boolean hasMovementRestrictions(SimulatedPlayer simulated) {
      return simulated.hasStatusEffect(StatusEffects.BLINDNESS) || simulated.hasStatusEffect(StatusEffects.LEVITATION) || PlayerIntersectionUtil.isBoxInBlock(simulated.boundingBox.expand(-0.001D), Blocks.COBWEB) || simulated.isSubmergedInWater() || simulated.isInLava() || simulated.isClimbing() || !PlayerIntersectionUtil.canChangeIntoPose(EntityPose.STANDING, simulated.pos) || simulated.player.getAbilities().flying;
   }

   private boolean isPlayerInCriticalState(SimulatedPlayer simulated, int ticks) {
      boolean fall = simulated.fallDistance > 0.0F && ((double)simulated.fallDistance < 0.08D || !SimulatedPlayer.simulateLocalPlayer(ticks + 1).onGround);
      return !simulated.onGround && fall;
   }

   @Generated
   public void setCount(int count) {
      this.count = count;
   }

   @Generated
   public TimerUtil getAttackTimer() {
      return this.attackTimer;
   }

   @Generated
   public TimerUtil getShieldWatch() {
      return this.shieldWatch;
   }

   @Generated
   public ClickScheduler getClickScheduler() {
      return this.clickScheduler;
   }

   @Generated
   public int getCount() {
      return this.count;
   }
}
