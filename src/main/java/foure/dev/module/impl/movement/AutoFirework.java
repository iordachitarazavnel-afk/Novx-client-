package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "AutoFirework",
   category = Category.MOVEMENT,
   desc = "Automatically uses fireworks for elytra flying"
)
public class AutoFirework extends Function {
   private final BooleanSetting onlyElytra = new BooleanSetting("Only Elytra", true);
   private final NumberSetting delay = new NumberSetting("Delay", this, 10.0D, 1.0D, 40.0D, 1.0D);
   private final NumberSetting durabilityWarningThreshold = new NumberSetting("Durability Warning %", this, 10.0D, 1.0D, 50.0D, 1.0D);
   private int cooldown = 0;
   private int previousSlot = -1;
   private boolean durabilityAlerted = false;

   public AutoFirework() {
      this.addSettings(new Setting[]{this.onlyElytra, this.delay, this.durabilityWarningThreshold});
   }

   public void onEnable() {
      super.onEnable();
      this.cooldown = 0;
      this.previousSlot = -1;
      this.durabilityAlerted = false;
   }

   public void onDisable() {
      super.onDisable();
      this.cooldown = 0;
      this.previousSlot = -1;
      this.durabilityAlerted = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck() && mc.currentScreen == null) {
         this.checkElytraDurability();
         if (this.cooldown > 0) {
            --this.cooldown;
         } else if (!(Boolean)this.onlyElytra.getValue() || mc.player.isGliding()) {
            int fireworkSlot = this.findFireworkInHotbar();
            if (fireworkSlot != -1) {
               if (this.previousSlot == -1) {
                  this.previousSlot = mc.player.getInventory().selectedSlot;
               }

               if (mc.player.getInventory().selectedSlot != fireworkSlot) {
                  mc.player.getInventory().selectedSlot = fireworkSlot;
               } else {
                  mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               }

               this.cooldown = (int)this.delay.getValueFloat();
            }

         }
      }
   }

   private void checkElytraDurability() {
      ItemStack chestplate = mc.player.getEquippedStack(EquipmentSlot.CHEST);
      if (chestplate.getItem() == Items.ELYTRA) {
         int maxDurability = chestplate.getMaxDamage();
         int currentDamage = chestplate.getDamage();
         int durabilityLeft = maxDurability - currentDamage;
         double durabilityPercent = (double)durabilityLeft / (double)maxDurability * 100.0D;
         if (durabilityPercent <= (double)this.durabilityWarningThreshold.getValueFloat() && !this.durabilityAlerted) {
            if (mc.player != null) {
               ClientPlayerEntity var10000 = mc.player;
               Object[] var10002 = new Object[]{durabilityPercent};
               var10000.sendMessage(Text.literal("§cElytra Durability at " + String.format("%.1f", var10002) + "%"), false);
            }

            mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ANVIL_PLACE, 1.0F));
            this.durabilityAlerted = true;
         } else if (durabilityPercent > (double)this.durabilityWarningThreshold.getValueFloat()) {
            this.durabilityAlerted = false;
         }
      }

   }

   private int findFireworkInHotbar() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }

      return -1;
   }
}
