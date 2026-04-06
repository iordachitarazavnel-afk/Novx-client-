package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.Comparator;
import java.util.stream.StreamSupport;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "SpawnerDropper",
   category = Category.DONUT,
   desc = "Automates dropping items from a spawner."
)
public class SpawnerDropper extends Function {
   private final NumberSetting delay = new NumberSetting("Delay (ticks)", this, 10.0D, 1.0D, 40.0D, 1.0D);
   private final BooleanSetting boneOnly = new BooleanSetting("Bone Only", this, false);
   private SpawnerDropper.State currentState;
   private BlockPos spawnerPos;
   private int waitTicks;

   public SpawnerDropper() {
      this.currentState = SpawnerDropper.State.IDLE;
      this.waitTicks = 0;
   }

   public void onEnable() {
      if (mc.player != null && mc.world != null) {
         this.currentState = SpawnerDropper.State.FINDING_SPAWNER;
         this.spawnerPos = null;
         this.waitTicks = 0;
         super.onEnable();
      } else {
         this.toggle();
      }
   }

   public void onDisable() {
      this.currentState = SpawnerDropper.State.IDLE;
      this.spawnerPos = null;
      this.waitTicks = 0;
      super.onDisable();
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.waitTicks > 0) {
            --this.waitTicks;
         } else {
            switch(this.currentState.ordinal()) {
            case 0:
            default:
               break;
            case 1:
               this.findSpawner();
               break;
            case 2:
               this.openSpawner();
               break;
            case 3:
               this.waitForGui();
               break;
            case 4:
               this.clickSlot46();
               break;
            case 5:
               this.waitDelay();
               break;
            case 6:
               this.clickSlot50();
               break;
            case 7:
               this.checkSlot50();
               break;
            case 8:
               this.checkSlotsForArrows();
               break;
            case 9:
               this.clickDropAll();
               break;
            case 10:
               this.clickNextPage();
               break;
            case 11:
               this.reCheckSlotsForArrows();
            }

         }
      } else {
         this.toggle();
      }
   }

   private void findSpawner() {
      this.spawnerPos = (BlockPos)StreamSupport.stream(BlockPos.iterate(mc.player.getBlockPos().add(-8, -8, -8), mc.player.getBlockPos().add(8, 8, 8)).spliterator(), false).filter((pos) -> {
         return mc.world.getBlockState(pos).isOf(Blocks.SPAWNER);
      }).min(Comparator.comparingDouble((pos) -> {
         return (new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ())).squaredDistanceTo(Vec3d.ofCenter(pos));
      })).map(BlockPos::toImmutable).orElse((BlockPos) null);
      if (this.spawnerPos != null) {
         this.currentState = SpawnerDropper.State.OPENING_SPAWNER;
      } else {
         this.toggle();
      }

   }

   private void openSpawner() {
      if (this.spawnerPos == null) {
         this.currentState = SpawnerDropper.State.FINDING_SPAWNER;
      } else {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(this.spawnerPos), Direction.UP, this.spawnerPos, false));
         this.currentState = SpawnerDropper.State.WAITING_FOR_GUI;
         this.waitTicks = 600;
      }
   }

   private void waitForGui() {
      if (mc.currentScreen instanceof GenericContainerScreen) {
         if ((Boolean)this.boneOnly.getValue()) {
            this.currentState = SpawnerDropper.State.CHECKING_SLOTS_FOR_ARROWS;
            this.waitTicks = 2;
         } else {
            this.currentState = SpawnerDropper.State.CLICKING_SLOT_46;
            this.waitTicks = 2;
         }
      } else if (this.waitTicks <= 1) {
         this.toggle();
      }

   }

   private void clickSlot46() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 46, 0, SlotActionType.PICKUP, mc.player);
         this.currentState = SpawnerDropper.State.WAITING_DELAY;
      }
   }

   private void waitDelay() {
      this.waitTicks = ((Double)this.delay.getValue()).intValue();
      this.currentState = SpawnerDropper.State.CLICKING_SLOT_50;
   }

   private void clickSlot50() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 50, 0, SlotActionType.PICKUP, mc.player);
         this.currentState = SpawnerDropper.State.CHECKING_SLOT_50;
         this.waitTicks = 2;
      }
   }

   private void checkSlot50() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         if (mc.player.currentScreenHandler.getSlot(50).getStack().getItem() != Items.ARROW) {
            mc.player.closeHandledScreen();
            this.toggle();
         } else {
            this.currentState = SpawnerDropper.State.CLICKING_SLOT_46;
         }

      }
   }

   private void checkSlotsForArrows() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         for(int slot = 0; slot <= 44; ++slot) {
            if (mc.player.currentScreenHandler.getSlot(slot).getStack().getItem() == Items.ARROW) {
               mc.player.closeHandledScreen();
               this.toggle();
               return;
            }
         }

         this.currentState = SpawnerDropper.State.CLICKING_DROP_ALL;
         this.waitTicks = 2;
      }
   }

   private void clickDropAll() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 50, 0, SlotActionType.PICKUP, mc.player);
         this.currentState = SpawnerDropper.State.CLICKING_NEXT_PAGE;
         this.waitTicks = 2;
      }
   }

   private void clickNextPage() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 53, 0, SlotActionType.PICKUP, mc.player);
         this.currentState = SpawnerDropper.State.RE_CHECKING_SLOTS;
         this.waitTicks = 2;
      }
   }

   private void reCheckSlotsForArrows() {
      if (!(mc.currentScreen instanceof GenericContainerScreen)) {
         this.toggle();
      } else {
         for(int slot = 0; slot <= 44; ++slot) {
            if (mc.player.currentScreenHandler.getSlot(slot).getStack().getItem() == Items.ARROW) {
               mc.player.closeHandledScreen();
               this.toggle();
               return;
            }
         }

         this.currentState = SpawnerDropper.State.CLICKING_DROP_ALL;
         this.waitTicks = 2;
      }
   }

   private static enum State {
      IDLE,
      FINDING_SPAWNER,
      OPENING_SPAWNER,
      WAITING_FOR_GUI,
      CLICKING_SLOT_46,
      WAITING_DELAY,
      CLICKING_SLOT_50,
      CHECKING_SLOT_50,
      CHECKING_SLOTS_FOR_ARROWS,
      CLICKING_DROP_ALL,
      CLICKING_NEXT_PAGE,
      RE_CHECKING_SLOTS;

      // $FF: synthetic method
      private static SpawnerDropper.State[] $values() {
         return new SpawnerDropper.State[]{IDLE, FINDING_SPAWNER, OPENING_SPAWNER, WAITING_FOR_GUI, CLICKING_SLOT_46, WAITING_DELAY, CLICKING_SLOT_50, CHECKING_SLOT_50, CHECKING_SLOTS_FOR_ARROWS, CLICKING_DROP_ALL, CLICKING_NEXT_PAGE, RE_CHECKING_SLOTS};
      }
   }
}
