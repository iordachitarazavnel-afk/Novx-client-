package foure.dev.module.impl.combat.helper.attack;

import foure.dev.event.impl.game.PacketEvent;
import foure.dev.event.impl.player.UsingItemEvent;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.util.wrapper.Wrapper;
import java.util.List;
import lombok.Generated;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;

public class AttackPerpetrator implements Wrapper {
   AttackHandler attackHandler = new AttackHandler();

   public void tick() {
      this.attackHandler.tick();
   }

   public void onPacket(PacketEvent e) {
      this.attackHandler.onPacket(e);
   }

   public void onUsingItem(UsingItemEvent e) {
      this.attackHandler.onUsingItem(e);
   }

   public void performAttack(AttackPerpetrator.AttackPerpetratorConfigurable configurable) {
      this.attackHandler.handleAttack(configurable);
   }

   @Generated
   public AttackHandler getAttackHandler() {
      return this.attackHandler;
   }

   public static class AttackPerpetratorConfigurable {
      private final LivingEntity target;
      private final Angle angle;
      private final float maximumRange;
      private final boolean onlyCritical;
      private final boolean shouldBreakShield;
      private final boolean shouldUnPressShield;
      private final boolean useDynamicCooldown;
      private final boolean eatAndAttack;
      private final Box box;
      private final ModeSetting aimMode;

      public AttackPerpetratorConfigurable(LivingEntity target, Angle angle, float maximumRange, List<String> options, ModeSetting aimMode, Box box) {
         this.target = target;
         this.angle = angle;
         this.maximumRange = maximumRange;
         this.onlyCritical = options.contains("Only Critical");
         this.shouldBreakShield = options.contains("Break Shield");
         this.shouldUnPressShield = options.contains("UnPress Shield");
         this.useDynamicCooldown = options.contains("Dynamic Cooldown");
         this.eatAndAttack = options.contains("No Attack When Eat");
         this.box = box;
         this.aimMode = aimMode;
      }

      @Generated
      public LivingEntity getTarget() {
         return this.target;
      }

      @Generated
      public Angle getAngle() {
         return this.angle;
      }

      @Generated
      public float getMaximumRange() {
         return this.maximumRange;
      }

      @Generated
      public boolean isOnlyCritical() {
         return this.onlyCritical;
      }

      @Generated
      public boolean isShouldBreakShield() {
         return this.shouldBreakShield;
      }

      @Generated
      public boolean isShouldUnPressShield() {
         return this.shouldUnPressShield;
      }

      @Generated
      public boolean isUseDynamicCooldown() {
         return this.useDynamicCooldown;
      }

      @Generated
      public boolean isEatAndAttack() {
         return this.eatAndAttack;
      }

      @Generated
      public Box getBox() {
         return this.box;
      }

      @Generated
      public ModeSetting getAimMode() {
         return this.aimMode;
      }
   }
}
