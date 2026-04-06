package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.combat.helper.Angle;
import foure.dev.module.impl.combat.helper.AngleUtil;
import foure.dev.module.impl.combat.helper.RotationConfig;
import foure.dev.module.impl.combat.helper.RotationController;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.util.Script.TaskPriority;
import foure.dev.util.render.utils.ChatUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AutoTreeFarmer",
   category = Category.DONUT,
   desc = "AFK farms 2x2 spruce podzol patches with auto refill"
)
public class AutoTreeFarmer extends Function {
   private final BooleanSetting enableHotbarRefillSetting = new BooleanSetting("Hotbar Refill", this, true);
   private final BooleanSetting enableOrderRefillSetting = new BooleanSetting("Order Refill", this, true);
   private final int HOTBAR_REFILL_MIN_BONE_MEAL = 5;
   private final int HOTBAR_REFILL_MIN_SAPLING = 4;
   private final int ORDER_REFILL_AMOUNT = 3;
   private final Random random = new Random();
   private final List<BlockPos> saplingPositions = new ArrayList();
   private final int searchRadius = 16;
   private final int orderDelay = 400;
   private final int maxRefillRetries = 3;
   private AutoTreeFarmer.FarmState state;
   private BlockPos targetPodzol;
   private int currentSaplingIndex;
   private int waitTicks;
   private int previousSlot;
   private boolean isPlacing;
   private boolean logMined;
   private AutoTreeFarmer.RefillState refillState;
   private long refillStageStart;
   private Item currentRefillItem;
   private int stacksCollected;
   private int refillRetryCount;
   private boolean isRefilling;
   private int refillStep;
   private int refillFromSlot;
   private int refillToSlot;
   private long refillStepStart;
   private Item refillTargetItem;
   private int refillMinAmount;

   public AutoTreeFarmer() {
      this.state = AutoTreeFarmer.FarmState.SEARCHING;
      this.targetPodzol = null;
      this.currentSaplingIndex = 0;
      this.waitTicks = 0;
      this.previousSlot = -1;
      this.isPlacing = false;
      this.logMined = false;
      this.refillState = AutoTreeFarmer.RefillState.NONE;
      this.refillStageStart = 0L;
      this.currentRefillItem = null;
      this.stacksCollected = 0;
      this.refillRetryCount = 0;
      this.isRefilling = false;
      this.refillStep = 0;
      this.refillFromSlot = -1;
      this.refillToSlot = -1;
      this.refillStepStart = 0L;
      this.refillTargetItem = null;
      this.refillMinAmount = 0;
   }

   public void onEnable() {
      this.reset();
   }

   public void onDisable() {
      if (this.previousSlot != -1 && mc.player != null) {
         mc.player.getInventory().selectedSlot = this.previousSlot;
      }

      mc.options.useKey.setPressed(false);
      mc.options.attackKey.setPressed(false);
      mc.options.sneakKey.setPressed(false);
      this.refillState = AutoTreeFarmer.RefillState.NONE;
      if (this.isRefilling) {
         this.cancelHotbarRefill();
      }

      RotationController.INSTANCE.setRotation((Angle)null);
   }

   public void reset() {
      if (mc.options != null) {
         this.saplingPositions.clear();
         this.targetPodzol = null;
         this.currentSaplingIndex = 0;
         this.waitTicks = 0;
         this.previousSlot = -1;
         this.isPlacing = false;
         this.logMined = false;
         this.state = AutoTreeFarmer.FarmState.SEARCHING;
         this.refillState = AutoTreeFarmer.RefillState.NONE;
         this.refillStageStart = 0L;
         this.currentRefillItem = null;
         this.stacksCollected = 0;
         this.refillRetryCount = 0;
         this.isRefilling = false;
         this.refillStep = 0;
         this.refillFromSlot = -1;
         this.refillToSlot = -1;
         this.refillStepStart = 0L;
         this.refillTargetItem = null;
         this.refillMinAmount = 0;
         if (mc.player != null) {
            mc.player.getInventory().selectedSlot = this.previousSlot != -1 ? this.previousSlot : 0;
         }

         mc.options.useKey.setPressed(false);
         mc.options.attackKey.setPressed(false);
         mc.options.sneakKey.setPressed(false);
      }
   }

   private void sendMessage(String msg) {
      ChatUtils.sendMessage(msg);
   }

   private void autoRefillHotbar(Item targetItem, int minAmount) {
      if ((Boolean)this.enableHotbarRefillSetting.getValue() && !this.isRefilling) {
         int totalInHotbar = 0;

         int invSlot;
         for(invSlot = 0; invSlot < 9; ++invSlot) {
            ItemStack stack = mc.player.getInventory().getStack(invSlot);
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
               totalInHotbar += stack.getCount();
            }
         }

         if (totalInHotbar < minAmount) {
            invSlot = this.findInInventory(targetItem);
            if (invSlot != -1 && invSlot >= 9) {
               int targetHotbarSlot = this.findBestHotbarSlot(targetItem);
               if (targetHotbarSlot != -1) {
                  this.startHotbarRefill(invSlot, targetHotbarSlot, targetItem, minAmount);
               }
            }

         }
      }
   }

   private int findBestHotbarSlot(Item item) {
      int i;
      for(i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      for(i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private int findInInventory(Item item) {
      for(int i = 9; i < mc.player.getInventory().size(); ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private void startHotbarRefill(int fromSlot, int toSlot, Item item, int minAmount) {
      this.isRefilling = true;
      this.refillStep = 0;
      this.refillFromSlot = fromSlot;
      this.refillToSlot = toSlot;
      this.refillTargetItem = item;
      this.refillMinAmount = minAmount;
      this.refillStepStart = System.currentTimeMillis();
   }

   private void handleHotbarRefill() {
      if (this.isRefilling) {
         long now = System.currentTimeMillis();
         long timeSinceStep = now - this.refillStepStart;
         int stepDelay = 150 + this.random.nextInt(100);
         switch(this.refillStep) {
         case 0:
            if (timeSinceStep < 50L) {
               return;
            }

            mc.setScreen(new InventoryScreen(mc.player));
            this.refillStep = 1;
            this.refillStepStart = now;
            break;
         case 1:
            if (timeSinceStep < (long)stepDelay) {
               return;
            }

            if (mc.currentScreen instanceof InventoryScreen) {
               this.refillStep = 2;
               this.refillStepStart = now;
            } else if (timeSinceStep > 2000L) {
               this.cancelHotbarRefill();
            }
            break;
         case 2:
            if (timeSinceStep < (long)stepDelay) {
               return;
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.refillFromSlot, 0, SlotActionType.PICKUP, mc.player);
            this.refillStep = 3;
            this.refillStepStart = now;
            break;
         case 3:
            if (timeSinceStep < (long)stepDelay) {
               return;
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.refillToSlot, 0, SlotActionType.PICKUP, mc.player);
            this.refillStep = 4;
            this.refillStepStart = now;
            break;
         case 4:
            if (timeSinceStep < (long)stepDelay) {
               return;
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, this.refillFromSlot, 0, SlotActionType.PICKUP, mc.player);
            this.refillStep = 5;
            this.refillStepStart = now;
            break;
         case 5:
            if (timeSinceStep < (long)stepDelay) {
               return;
            }

            mc.player.closeHandledScreen();
            this.refillStep = 6;
            this.refillStepStart = now;
            break;
         case 6:
            if (timeSinceStep < 100L) {
               return;
            }

            this.finishHotbarRefill();
         }

      }
   }

   private void cancelHotbarRefill() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      this.finishHotbarRefill();
   }

   private void finishHotbarRefill() {
      this.isRefilling = false;
      this.refillStep = 0;
      this.refillFromSlot = -1;
      this.refillToSlot = -1;
      this.refillTargetItem = null;
      this.refillMinAmount = 0;
      this.refillStepStart = 0L;
   }

   private void checkAndStartOrderRefill() {
      if ((Boolean)this.enableOrderRefillSetting.getValue() && !this.isRefilling && this.refillState == AutoTreeFarmer.RefillState.NONE) {
         if (this.countItemInInventory(Items.BONE_MEAL) == 0) {
            this.startOrderRefill(Items.BONE_MEAL);
         } else if (this.countItemInInventory(Items.SPRUCE_SAPLING) == 0) {
            this.startOrderRefill(Items.SPRUCE_SAPLING);
         }

      }
   }

   private void startOrderRefill(Item item) {
      if (this.refillRetryCount >= 3) {
         this.sendMessage("Max refill retries reached for " + item.getName().getString());
         this.refillRetryCount = 0;
      } else {
         this.currentRefillItem = item;
         this.refillState = AutoTreeFarmer.RefillState.OPEN_ORDERS;
         this.refillStageStart = System.currentTimeMillis();
         this.stacksCollected = 0;
         this.sendMessage("Starting order refill for " + item.getName().getString());
      }
   }

   private int countItemInInventory(Item item) {
      int count = 0;

      for(int i = 0; i < mc.player.getInventory().size(); ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty() && stack.getItem() == item) {
            count += stack.getCount();
         }
      }

      return count;
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.world != null && mc.player != null) {
         if (this.isRefilling) {
            this.handleHotbarRefill();
         } else if (this.refillState != AutoTreeFarmer.RefillState.NONE) {
            this.handleOrderRefill();
         } else {
            this.autoRefillHotbar(Items.BONE_MEAL, 5);
            this.autoRefillHotbar(Items.SPRUCE_SAPLING, 4);
            this.checkAndStartOrderRefill();
            switch(this.state.ordinal()) {
            case 0:
               this.findPodzol();
               break;
            case 1:
               this.plantSaplings();
               break;
            case 2:
               this.boneMealSaplings();
               break;
            case 3:
               this.mineLog();
               break;
            case 4:
               this.waitAfterMining();
            }

         }
      }
   }

   private void findPodzol() {
      BlockPos playerPos = mc.player.getBlockPos();
      double nearestDist = Double.MAX_VALUE;
      this.targetPodzol = null;

      int dx;
      int dz;
      for(dx = playerPos.getY() - 2; dx <= playerPos.getY(); ++dx) {
         for(dz = -16; dz <= 16; ++dz) {
            for(int z = -16; z <= 16; ++z) {
               BlockPos pos = new BlockPos(playerPos.getX() + dz, dx, playerPos.getZ() + z);
               BlockPos corner = this.find2x2PodzolAt(pos);
               if (corner != null) {
                  double dist = playerPos.getSquaredDistance(corner.toCenterPos());
                  if (dist < nearestDist) {
                     nearestDist = dist;
                     this.targetPodzol = corner;
                  }
               }
            }
         }
      }

      if (this.targetPodzol != null) {
         this.saplingPositions.clear();

         for(dx = 0; dx < 2; ++dx) {
            for(dz = 0; dz < 2; ++dz) {
               this.saplingPositions.add(this.targetPodzol.add(dx, 1, dz));
            }
         }

         this.saplingPositions.sort(Comparator.comparingDouble((posx) -> {
            return -mc.player.getBlockPos().getSquaredDistance(posx.toCenterPos());
         }));
         this.currentSaplingIndex = 0;
         this.logMined = false;
         this.state = AutoTreeFarmer.FarmState.PLANTING;
         this.sendMessage("Found 2x2 podzol at " + this.targetPodzol.toShortString());
      }
   }

   private BlockPos find2x2PodzolAt(BlockPos pos) {
      for(int offsetX = -1; offsetX <= 0; ++offsetX) {
         for(int offsetZ = -1; offsetZ <= 0; ++offsetZ) {
            boolean valid = true;
            BlockPos corner = pos.add(offsetX, 0, offsetZ);

            for(int x = 0; x < 2; ++x) {
               for(int z = 0; z < 2; ++z) {
                  if (mc.world.getBlockState(corner.add(x, 0, z)).getBlock() != Blocks.PODZOL) {
                     valid = false;
                     break;
                  }
               }

               if (!valid) {
                  break;
               }
            }

            if (valid) {
               return corner;
            }
         }
      }

      return null;
   }

   private int findSpruceSaplingSlot() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == Blocks.SPRUCE_SAPLING.asItem()) {
            return i;
         }
      }

      return -1;
   }

   private int findBoneMealSlot() {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() instanceof BoneMealItem) {
            return i;
         }
      }

      return -1;
   }

   private int findBestAxeSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.getItem() instanceof AxeItem) {
            return i;
         }
      }

      return -1;
   }

   private void rotateTo(BlockPos pos) {
      Vec3d target = new Vec3d((double)pos.getX() + 0.5D, (double)pos.getY() + 0.1D, (double)pos.getZ() + 0.5D);
      Angle angle = AngleUtil.calculateAngle(target);
      RotationController.INSTANCE.rotateTo(angle, RotationConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
   }

   private void plantSaplings() {
      if (this.currentSaplingIndex >= this.saplingPositions.size()) {
         this.state = AutoTreeFarmer.FarmState.BONEMEALING;
         this.isPlacing = false;
         RotationController.INSTANCE.setRotation((Angle)null);
      } else {
         BlockPos pos = (BlockPos)this.saplingPositions.get(this.currentSaplingIndex);
         if (!mc.world.getBlockState(pos).isAir()) {
            ++this.currentSaplingIndex;
            this.isPlacing = false;
         } else {
            int slot = this.findSpruceSaplingSlot();
            if (slot == -1) {
               this.sendMessage("No spruce saplings!");
               this.state = AutoTreeFarmer.FarmState.SEARCHING;
               this.isPlacing = false;
               RotationController.INSTANCE.setRotation((Angle)null);
            } else {
               if (this.previousSlot == -1) {
                  this.previousSlot = mc.player.getInventory().selectedSlot;
               }

               mc.player.getInventory().selectedSlot = slot;
               this.rotateTo(pos);
               mc.options.useKey.setPressed(true);
               this.isPlacing = true;
               if (this.isPlacing && mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                  ++this.currentSaplingIndex;
                  this.isPlacing = false;
                  mc.options.useKey.setPressed(false);
               }

            }
         }
      }
   }

   private void boneMealSaplings() {
      int slot = this.findBoneMealSlot();
      if (slot == -1) {
         this.sendMessage("No bone meal!");
         this.state = AutoTreeFarmer.FarmState.SEARCHING;
      } else {
         if (this.previousSlot == -1) {
            this.previousSlot = mc.player.getInventory().selectedSlot;
         }

         mc.player.getInventory().selectedSlot = slot;
         BlockPos pos = (BlockPos)this.saplingPositions.get(0);
         this.rotateTo(pos);
         boolean grown = false;

         for(int dx = 0; dx < 2; ++dx) {
            for(int dz = 0; dz < 2; ++dz) {
               Block block = mc.world.getBlockState(this.targetPodzol.add(dx, 1, dz)).getBlock();
               if (block != Blocks.SPRUCE_SAPLING && block != Blocks.AIR) {
                  grown = true;
                  break;
               }
            }

            if (grown) {
               break;
            }
         }

         if (grown) {
            mc.options.useKey.setPressed(false);
            this.state = AutoTreeFarmer.FarmState.MINING;
            this.sendMessage("Tree grown! Starting to mine...");
            RotationController.INSTANCE.setRotation((Angle)null);
         } else {
            mc.options.useKey.setPressed(true);
         }
      }
   }

   private void mineLog() {
      mc.options.useKey.setPressed(false);
      if (this.logMined) {
         mc.options.attackKey.setPressed(false);
         this.waitTicks = 10;
         this.state = AutoTreeFarmer.FarmState.WAIT;
      } else {
         BlockPos pos = this.targetPodzol.add(0, 1, 0);
         if (mc.world.getBlockState(pos).getBlock() == Blocks.SPRUCE_LOG) {
            this.rotateTo(pos);
            int slot = this.findBestAxeSlot();
            if (slot == -1) {
               this.sendMessage("No axe found!");
               this.state = AutoTreeFarmer.FarmState.SEARCHING;
               return;
            }

            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().selectedSlot;
            }

            mc.player.getInventory().selectedSlot = slot;
            mc.options.attackKey.setPressed(true);
         } else {
            mc.options.attackKey.setPressed(false);
            this.logMined = true;
            this.waitTicks = 10;
            this.state = AutoTreeFarmer.FarmState.WAIT;
            RotationController.INSTANCE.setRotation((Angle)null);
         }

      }
   }

   private void waitAfterMining() {
      if (this.waitTicks > 0) {
         --this.waitTicks;
      } else {
         this.state = AutoTreeFarmer.FarmState.SEARCHING;
      }
   }

   private void handleOrderRefill() {
      long now = System.currentTimeMillis();
      long timeInState = now - this.refillStageStart;
      int fastOrderDelay = 400 + this.random.nextInt(100);
      if (timeInState > 5000L) {
         this.sendMessage("Order refill stage timeout. Resetting module...");
         mc.player.closeHandledScreen();
         this.reset();
      } else {
         GenericContainerScreen screen;
         Screen var7;
         switch(this.refillState.ordinal()) {
         case 1:
            if (timeInState < 100L) {
               return;
            }

            mc.player.networkHandler.sendChatCommand("order");
            this.refillState = AutoTreeFarmer.RefillState.WAIT_ORDERS_GUI;
            this.refillStageStart = now;
            break;
         case 2:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            if (mc.currentScreen instanceof GenericContainerScreen) {
               this.refillState = AutoTreeFarmer.RefillState.CLICK_SLOT_51;
               this.refillStageStart = now;
            }
            break;
         case 3:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            var7 = mc.currentScreen;
            if (!(var7 instanceof GenericContainerScreen)) {
               return;
            }

            screen = (GenericContainerScreen)var7;
            ScreenHandler handler = screen.getScreenHandler();
            if (handler.slots.size() > 51) {
               mc.interactionManager.clickSlot(handler.syncId, 51, 0, SlotActionType.PICKUP, mc.player);
               this.refillState = AutoTreeFarmer.RefillState.WAIT_SECOND_GUI;
               this.refillStageStart = now;
            }
            break;
         case 4:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            if (mc.currentScreen instanceof GenericContainerScreen) {
               this.refillState = AutoTreeFarmer.RefillState.CLICK_TARGET_ITEM;
               this.refillStageStart = now;
            }
            break;
         case 5:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            var7 = mc.currentScreen;
            if (!(var7 instanceof GenericContainerScreen)) {
               return;
            }

            screen = (GenericContainerScreen)var7;
            if (this.findAndClickTargetItem(screen.getScreenHandler())) {
               this.refillState = AutoTreeFarmer.RefillState.WAIT_THIRD_GUI;
               this.refillStageStart = now;
            }
            break;
         case 6:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            if (mc.currentScreen instanceof GenericContainerScreen) {
               this.refillState = AutoTreeFarmer.RefillState.CLICK_CHEST_SLOT;
               this.refillStageStart = now;
            }
            break;
         case 7:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            var7 = mc.currentScreen;
            if (!(var7 instanceof GenericContainerScreen)) {
               return;
            }

            screen = (GenericContainerScreen)var7;
            if (this.clickChestSlot(screen.getScreenHandler())) {
               this.refillState = AutoTreeFarmer.RefillState.WAIT_ITEMS_GUI;
               this.refillStageStart = now;
            }
            break;
         case 8:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            if (mc.currentScreen instanceof GenericContainerScreen) {
               this.refillState = AutoTreeFarmer.RefillState.COLLECT_ITEMS;
               this.refillStageStart = now;
            }
            break;
         case 9:
            if (timeInState < (long)fastOrderDelay) {
               return;
            }

            var7 = mc.currentScreen;
            if (!(var7 instanceof GenericContainerScreen)) {
               return;
            }

            screen = (GenericContainerScreen)var7;
            if (this.collectItems(screen.getScreenHandler())) {
               this.refillStageStart = now;
               if (this.stacksCollected >= 3) {
                  this.finishRefill(true);
               }
            } else if (this.stacksCollected > 0) {
               this.finishRefill(true);
            } else {
               this.retryRefill("No items found to collect");
            }
            break;
         case 10:
            mc.player.closeHandledScreen();
            this.finishRefill(false);
         }

      }
   }

   private boolean findAndClickTargetItem(ScreenHandler handler) {
      for(int i = 0; i < Math.min(handler.slots.size(), 54); ++i) {
         Slot slot = (Slot)handler.slots.get(i);
         if (slot.hasStack() && slot.getStack().getItem() == this.currentRefillItem) {
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            return true;
         }
      }

      return false;
   }

   private boolean clickChestSlot(ScreenHandler handler) {
      int[] var2 = new int[]{11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24};
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         int i = var2[var4];
         if (handler.slots.size() > i) {
            Slot slot = (Slot)handler.slots.get(i);
            if (slot.hasStack() && slot.getStack().getItem() == Items.CHEST) {
               mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
               return true;
            }
         }
      }

      return false;
   }

   private boolean collectItems(ScreenHandler handler) {
      for(int i = 0; i < Math.min(handler.slots.size(), 54); ++i) {
         Slot slot = (Slot)handler.slots.get(i);
         if (slot.hasStack() && slot.getStack().getItem() == this.currentRefillItem) {
            mc.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            ++this.stacksCollected;
            return true;
         }
      }

      return false;
   }

   private void retryRefill(String reason) {
      ++this.refillRetryCount;
      if (this.refillRetryCount < 3) {
         this.sendMessage("Refill failed (" + reason + "), retrying... (" + this.refillRetryCount + "/3)");
         mc.player.closeHandledScreen();
         this.refillState = AutoTreeFarmer.RefillState.OPEN_ORDERS;
         this.refillStageStart = System.currentTimeMillis() + 800L + (long)this.random.nextInt(400);
      } else {
         this.sendMessage("Refill failed after max retries: " + reason);
         this.finishRefill(false);
      }

   }

   private void finishRefill(boolean success) {
      mc.player.closeHandledScreen();
      if (success) {
         int var10001 = this.stacksCollected;
         this.sendMessage("Refill completed! Collected " + var10001 + " stacks of " + (this.currentRefillItem != null ? this.currentRefillItem.getName().getString() : "items"));
      }

      this.refillState = AutoTreeFarmer.RefillState.NONE;
      this.currentRefillItem = null;
      this.stacksCollected = 0;
      this.refillRetryCount = 0;
   }

   static enum FarmState {
      SEARCHING,
      PLANTING,
      BONEMEALING,
      MINING,
      WAIT;

      // $FF: synthetic method
      private static AutoTreeFarmer.FarmState[] $values() {
         return new AutoTreeFarmer.FarmState[]{SEARCHING, PLANTING, BONEMEALING, MINING, WAIT};
      }
   }

   static enum RefillState {
      NONE,
      OPEN_ORDERS,
      WAIT_ORDERS_GUI,
      CLICK_SLOT_51,
      WAIT_SECOND_GUI,
      CLICK_TARGET_ITEM,
      WAIT_THIRD_GUI,
      CLICK_CHEST_SLOT,
      WAIT_ITEMS_GUI,
      COLLECT_ITEMS,
      CLOSE_GUI;

      // $FF: synthetic method
      private static AutoTreeFarmer.RefillState[] $values() {
         return new AutoTreeFarmer.RefillState[]{NONE, OPEN_ORDERS, WAIT_ORDERS_GUI, CLICK_SLOT_51, WAIT_SECOND_GUI, CLICK_TARGET_ITEM, WAIT_THIRD_GUI, CLICK_CHEST_SLOT, WAIT_ITEMS_GUI, COLLECT_ITEMS, CLOSE_GUI};
      }
   }
}
