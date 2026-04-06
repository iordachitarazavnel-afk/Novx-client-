package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "TargetPearl",
   desc = "Automatically throws pearls at targets",
   category = Category.MOVEMENT
)
public class TargetPearl extends Function {
   private final Set<UUID> seenPearls = new HashSet();
   public NumberSetting radius = new NumberSetting("Radius", this, 30.0D, 5.0D, 100.0D, 1.0D);
   public NumberSetting delay = new NumberSetting("Delay Ticks", this, 10.0D, 1.0D, 40.0D, 1.0D);
   private int lastThrowTick = 0;

   public TargetPearl() {
      this.addSettings(new Setting[]{this.radius, this.delay});
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         ++this.lastThrowTick;
         Iterator var2 = mc.world.getEntities().iterator();

         while(var2.hasNext()) {
            Entity entity = (Entity)var2.next();
            if (entity instanceof EnderPearlEntity) {
               EnderPearlEntity pearl = (EnderPearlEntity)entity;
               UUID pearlId = pearl.getUuid();
               if (!this.seenPearls.contains(pearlId) && pearl.age <= 2) {
                  Entity owner = pearl.getOwner();
                  if (owner instanceof PlayerEntity) {
                     PlayerEntity player = (PlayerEntity)owner;
                     if (player != mc.player && !((double)mc.player.distanceTo(player) > (Double)this.radius.getValue()) && !((double)this.lastThrowTick < (Double)this.delay.getValue())) {
                        int pearlSlot = this.findPearlSlot();
                        if (pearlSlot != -1) {
                           float[] angles = this.getAnglesFromVelocity(pearl.getVelocity().x, pearl.getVelocity().y, pearl.getVelocity().z);
                           this.smoothLookAt(angles[0], angles[1]);
                           this.throwPearl(pearlSlot);
                           this.lastThrowTick = 0;
                           this.seenPearls.add(pearlId);
                        }
                     }
                  }
               }
            }
         }

      }
   }

   private void throwPearl(int pearlSlot) {
      int oldSlot = mc.player.getInventory().selectedSlot;
      mc.player.getInventory().selectedSlot = pearlSlot;
      this.usePearl();
      mc.player.getInventory().selectedSlot = oldSlot;
   }

   private void usePearl() {
      ItemStack stack = mc.player.getMainHandStack();
      if (stack.getItem() instanceof EnderPearlItem) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
      }

   }

   private int findPearlSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof EnderPearlItem) {
            return i;
         }
      }

      return -1;
   }

   private float[] getAnglesFromVelocity(double vx, double vy, double vz) {
      float yaw = (float)(Math.toDegrees(Math.atan2(vz, vx)) - 90.0D);
      float pitch = (float)(-Math.toDegrees(Math.atan2(vy, Math.sqrt(vx * vx + vz * vz))));
      return new float[]{yaw, pitch};
   }

   private void smoothLookAt(float targetYaw, float targetPitch) {
      mc.player.setYaw(targetYaw);
      mc.player.setPitch(targetPitch);
   }
}
