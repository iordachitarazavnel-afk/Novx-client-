package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

@ModuleInfo(
   name = "AutoEat",
   category = Category.MOVEMENT,
   desc = "Automatically eats food when health or hunger is low"
)
public class AutoEat extends Function {
   private final NumberSetting healthThreshold = new NumberSetting("Health Threshold", this, 10.0D, 1.0D, 19.0D, 1.0D);
   private final NumberSetting hungerThreshold = new NumberSetting("Hunger Threshold", this, 16.0D, 1.0D, 19.0D, 1.0D);
   private boolean eating;
   private int slot;
   private int prevSlot;

   public AutoEat() {
      this.addSettings(new Setting[]{this.healthThreshold, this.hungerThreshold});
   }

   public void onEnable() {
      super.onEnable();
      this.eating = false;
      this.slot = -1;
      this.prevSlot = -1;
   }

   public void onDisable() {
      super.onDisable();
      if (this.eating) {
         this.stopEating();
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         if (this.eating) {
            if (!this.shouldEat()) {
               this.stopEating();
               return;
            }

            ItemStack currentStack = mc.player.getMainHandStack();
            if (this.slot == 45) {
               currentStack = mc.player.getOffHandStack();
            }

            if (currentStack.get(DataComponentTypes.FOOD) == null) {
               int newSlot = this.findFoodSlot();
               if (newSlot == -1) {
                  this.stopEating();
                  return;
               }

               this.changeSlot(newSlot);
            }

            this.eat();
         } else if (this.shouldEat()) {
            this.startEating();
         }

      }
   }

   private void startEating() {
      this.prevSlot = mc.player.getInventory().selectedSlot;
      this.eating = true;
      this.eat();
   }

   private void eat() {
      if (this.slot != 45) {
         this.changeSlot(this.slot);
         mc.options.useKey.setPressed(true);
         if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         }
      } else {
         mc.options.useKey.setPressed(true);
         if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
         }
      }

   }

   private void stopEating() {
      mc.options.useKey.setPressed(false);
      if (this.slot != 45 && this.prevSlot != -1) {
         this.changeSlot(this.prevSlot);
      }

      this.eating = false;
      this.slot = -1;
   }

   private void changeSlot(int slot) {
      if (slot != -1 && slot != 45) {
         mc.player.getInventory().selectedSlot = slot;
         mc.interactionManager.syncSelectedSlot();
         this.slot = slot;
      }
   }

   private boolean shouldEat() {
      if (mc.player == null) {
         return false;
      } else {
         boolean healthLow = mc.player.getHealth() <= this.healthThreshold.getValueFloat();
         boolean hungerLow = mc.player.getHungerManager().getFoodLevel() <= (int)this.hungerThreshold.getValueFloat();
         if (!healthLow && !hungerLow) {
            return false;
         } else {
            this.slot = this.findFoodSlot();
            if (this.slot == -1) {
               return false;
            } else {
               ItemStack foodStack = this.slot == 45 ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(this.slot);
               FoodComponent food = (FoodComponent)foodStack.get(DataComponentTypes.FOOD);
               if (food == null) {
                  return false;
               } else {
                  return mc.player.getHungerManager().isNotFull() || food.canAlwaysEat();
               }
            }
         }
      }
   }

   private int findFoodSlot() {
      ItemStack offhand = mc.player.getOffHandStack();
      if (offhand.get(DataComponentTypes.FOOD) != null) {
         FoodComponent food = (FoodComponent)offhand.get(DataComponentTypes.FOOD);
         if (mc.player.getHungerManager().isNotFull() || food.canAlwaysEat()) {
            return 45;
         }
      }

      int bestSlot = -1;
      int bestHunger = -1;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         FoodComponent foodComponent = (FoodComponent)stack.get(DataComponentTypes.FOOD);
         if (foodComponent != null) {
            int hunger = foodComponent.nutrition();
            if (hunger > bestHunger) {
               bestSlot = i;
               bestHunger = hunger;
            }
         }
      }

      return bestSlot;
   }
}
