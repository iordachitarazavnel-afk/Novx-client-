package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.util.character.CenterCharacter;
import foure.dev.util.character.RotateCharacter;
import foure.dev.util.tunnelbasefinder.CarrotBuyState;
import foure.dev.util.tunnelbasefinder.MendStage;
import foure.dev.util.tunnelbasefinder.ObiBuyState;
import foure.dev.util.tunnelbasefinder.PearlBuyState;
import foure.dev.util.tunnelbasefinder.Phase;
import foure.dev.util.tunnelbasefinder.State;
import foure.dev.util.tunnelbasefinder.XpBuyState;
import java.util.Iterator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec2f;
import net.minecraft.world.chunk.WorldChunk;

@ModuleInfo(
   name = "TunnelBaseFinder",
   category = Category.DONUT,
   desc = "Digs in tunnels until you find a base"
)
public class TunnelBaseFinder extends Function {
   private static final int MIN_Y_LEVEL = -59;
   private static final long PHASE_TIME_MS = 5000L;
   private static final Item[] JUNK_ITEMS;
   public final ModeSetting mode = new ModeSetting("Mining Style", this, "Amethyst", new String[]{"Amethyst", "Crawl", "Standing"});
   public final BooleanSetting spawnercritical = new BooleanSetting("Spawner Critical", this, false);
   private final NumberSetting BlockSlot = new NumberSetting("Obsidian Slot", this, 2.0D, 1.0D, 9.0D, 1.0D);
   private final NumberSetting PearlSlot = new NumberSetting("Pearl Slot", this, 3.0D, 1.0D, 9.0D, 1.0D);
   private final NumberSetting XPSlot = new NumberSetting("Bottle Slot", this, 4.0D, 1.0D, 9.0D, 1.0D);
   private final NumberSetting CarrotSlot = new NumberSetting("GoldenCarrot Slot", this, 5.0D, 1.0D, 9.0D, 1.0D);
   public CenterCharacter centerCharacter;
   public RotateCharacter rotateCharacter;
   public Direction currentDirection;
   public State state;
   public State backup;
   boolean isRotating;
   XpBuyState xpbuystate;
   int xpwaitcounter;
   PearlBuyState pearlbuystate;
   int pearlwaitcounter;
   ObiBuyState obibuystate;
   int obiwaitcounter;
   CarrotBuyState carrotbuystate;
   int carrotwaitcounter;
   MendStage mendStage;
   int stuckTicks;
   BlockPos lastCoords;
   int chests;
   int hoppers;
   int dispensers;
   int enderChests;
   int shulkers;
   int movingPiston;
   boolean foundSpawner;
   boolean goddamnihateniggers;
   boolean jumped;
   private boolean yRecoveryRotationDone;
   private BlockPos yRecoveryBasePos;
   private boolean shouldCloseInventory;
   private int resetMiningTick;
   private int resetUseTick;
   private boolean wasScreenOpen;
   private boolean isBackup;
   private Direction backupDirection;
   private int mendingGraceTicks;
   private Phase phase;
   private long phaseStartTime;
   private boolean towerRotationDone;
   private BlockPos towerBasePos;

   public TunnelBaseFinder() {
      this.state = State.NONE;
      this.backup = State.NONE;
      this.isRotating = false;
      this.xpbuystate = XpBuyState.NONE;
      this.xpwaitcounter = 0;
      this.pearlbuystate = PearlBuyState.NONE;
      this.pearlwaitcounter = 0;
      this.obibuystate = ObiBuyState.NONE;
      this.obiwaitcounter = 0;
      this.carrotbuystate = CarrotBuyState.NONE;
      this.carrotwaitcounter = 0;
      this.mendStage = MendStage.ENSURE;
      this.stuckTicks = 0;
      this.chests = 0;
      this.hoppers = 0;
      this.dispensers = 0;
      this.enderChests = 0;
      this.shulkers = 0;
      this.goddamnihateniggers = true;
      this.yRecoveryRotationDone = false;
      this.yRecoveryBasePos = null;
      this.shouldCloseInventory = false;
      this.resetMiningTick = 0;
      this.resetUseTick = 0;
      this.wasScreenOpen = false;
      this.isBackup = false;
      this.mendingGraceTicks = 0;
      this.phase = Phase.DIG;
      this.phaseStartTime = 0L;
      this.towerRotationDone = false;
      this.towerBasePos = null;
   }

   public void onEnable() {
      this.state = State.NONE;
      this.backup = State.NONE;
      this.isRotating = false;
      this.phase = Phase.DIG;
      this.phaseStartTime = 0L;
      this.towerRotationDone = false;
      this.mendingGraceTicks = 0;
      this.stuckTicks = 0;
      this.lastCoords = null;
      this.towerBasePos = null;
      this.yRecoveryRotationDone = false;
      this.yRecoveryBasePos = null;
      this.mendStage = MendStage.ENSURE;
      this.xpbuystate = XpBuyState.NONE;
      this.xpwaitcounter = 0;
      this.pearlbuystate = PearlBuyState.NONE;
      this.pearlwaitcounter = 0;
      this.obibuystate = ObiBuyState.NONE;
      this.obiwaitcounter = 0;
      this.carrotbuystate = CarrotBuyState.NONE;
      this.carrotwaitcounter = 0;
      this.isBackup = false;
      this.goddamnihateniggers = true;
      if (mc.player != null) {
         this.rotateCharacter = new RotateCharacter(mc);
         this.centerCharacter = new CenterCharacter(mc);
         if (!this.isHoldingPickaxe()) {
            int pickaxeSlot = this.findPickaxeInHotbar();
            if (pickaxeSlot == -1) {
               this.disconnect("YOU DON'T HAVE PICKAXE");
               return;
            }

            mc.player.getInventory().selectedSlot = pickaxeSlot;
         }

         if (!this.hasTotemInOffhand()) {
            if (this.findTotemInInventory() == -1) {
               this.disconnect("YOU DON'T HAVE TOTEM");
               return;
            }

            this.offhandTotem(this.findTotemInInventory());
         }

         this.currentDirection = this.getDir();
         Vec2f firstTurn = this.getValues(this.currentDirection);
         if (!this.isRotating) {
            this.rotateCharacter.rotate(firstTurn.x, firstTurn.y, () -> {
               this.centerCharacter.initiate();
               this.state = State.MINING;
            });
         }

         super.onEnable();
      }
   }

   public void onDisable() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         this.updateMining(false);
         this.updateUsage(false);
         mc.options.jumpKey.setPressed(false);
      }

      this.state = State.NONE;
      this.backup = State.NONE;
      this.isRotating = false;
      this.phase = Phase.DIG;
      this.phaseStartTime = 0L;
      this.towerRotationDone = false;
      this.towerBasePos = null;
      this.currentDirection = null;
      this.yRecoveryRotationDone = false;
      this.yRecoveryBasePos = null;
      this.mendingGraceTicks = 0;
      this.wasScreenOpen = false;
      this.resetMiningTick = 0;
      this.resetUseTick = 0;
      super.onDisable();
   }

   private static int convertSlotIndex(int slotIndex) {
      return slotIndex < 9 ? 36 + slotIndex : slotIndex;
   }

   private boolean isHoldingPickaxe() {
      return mc.player == null ? false : this.isPickaxe(mc.player.getMainHandStack().getItem());
   }

   private boolean isPickaxe(Item item) {
      return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE;
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && mc.player != null) {
         if (this.rotateCharacter != null && this.rotateCharacter.isActive()) {
            this.isRotating = true;
            this.rotateCharacter.update(true, false);
         } else {
            this.isRotating = false;
         }

      }
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null && mc.world != null) {
         if (this.jumped) {
            this.jumped = false;
            mc.options.jumpKey.setPressed(false);
         }

         mc.execute(() -> {
            if (this.shouldCloseInventory) {
               if (mc.currentScreen instanceof InventoryScreen) {
                  mc.currentScreen.close();
               }

               this.shouldCloseInventory = false;
            }

            this.scanForBase();
            if (this.mendingGraceTicks > 0) {
               --this.mendingGraceTicks;
            }

            if (this.state != State.AUTOMEND && this.state != State.AUTOEAT && this.state != State.BUYXP && this.state != State.BUYPEARL && this.state != State.BUYCARROT && this.state != State.BUYOBI) {
               if (this.mendingGraceTicks <= 0 && !this.hasTotemInOffhand()) {
                  this.disconnect("YOUR TOTEM POPPED");
               }

               if (this.isPickaxeLowDurability() && this.state != State.BUYXP) {
                  this.backup = this.state;
                  this.state = State.AUTOMEND;
               }

               if (mc.player.getHungerManager().getFoodLevel() <= 6 && this.state != State.BUYCARROT) {
                  this.backup = this.state;
                  this.state = State.AUTOEAT;
               }
            }

            switch(this.state) {
            case NONE:
            default:
               break;
            case MINING:
               this.handleMining();
               break;
            case GOABOVEHAZARD:
               this.handleGoAbove();
               break;
            case YRECOVERY:
               this.handleYRecovery();
               break;
            case PEARL:
               this.handlePearl();
               break;
            case AUTOMEND:
               this.handleMend();
               break;
            case AUTOEAT:
               this.handleEating();
               break;
            case BUYXP:
               this.handleXPBuy();
               break;
            case BUYPEARL:
               this.handlePearlBuy();
               break;
            case BUYCARROT:
               this.handleCarrotBuy();
               break;
            case BUYOBI:
               this.handleObiBuy();
            }

            if (this.centerCharacter.isCentering()) {
               this.centerCharacter.update();
            }

            this.wasScreenOpen = mc.currentScreen != null;
         });
      }
   }

   private void scanForBase() {
      if (mc.player != null && mc.world != null) {
         this.chests = 0;
         this.hoppers = 0;
         this.dispensers = 0;
         this.enderChests = 0;
         this.shulkers = 0;
         this.movingPiston = 0;
         this.foundSpawner = false;
         int chunkX = mc.player.getChunkPos().x;
         int chunkZ = mc.player.getChunkPos().z;
         int radius = 2;

         for(int x = chunkX - radius; x <= chunkX + radius; ++x) {
            for(int z = chunkZ - radius; z <= chunkZ + radius; ++z) {
               if (mc.world.isChunkLoaded(x, z)) {
                  WorldChunk chunk = mc.world.getChunk(x, z);
                  Iterator var7 = chunk.getBlockEntityPositions().iterator();

                  while(var7.hasNext()) {
                     BlockPos pos = (BlockPos)var7.next();
                     BlockEntity be = mc.world.getBlockEntity(pos);
                     if (be != null) {
                        if (be instanceof MobSpawnerBlockEntity) {
                           this.foundSpawner = true;
                        }

                        if (be.getPos().getY() <= 0) {
                           if (be instanceof ChestBlockEntity) {
                              ++this.chests;
                           } else if (be instanceof ShulkerBoxBlockEntity) {
                              ++this.shulkers;
                           } else if (be instanceof PistonBlockEntity) {
                              ++this.movingPiston;
                           }
                        }
                     }
                  }
               }
            }
         }

         boolean foundBase = false;
         String reason = "";
         if (this.chests >= 35) {
            foundBase = true;
            reason = "BASE";
         } else if (this.shulkers >= 35) {
            foundBase = true;
            reason = "BASE";
         } else if (this.movingPiston >= 10) {
            reason = "BASE";
         } else if (this.foundSpawner && (Boolean)this.spawnercritical.getValue()) {
            foundBase = true;
            reason = "SPAWNER";
         }

         if (foundBase) {
            this.disconnect("YOU FOUND A " + reason);
         }

      }
   }

   private int findPickaxeInHotbar() {
      if (mc.player == null) {
         return -1;
      } else {
         for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (this.isPickaxe(stack.getItem())) {
               return i;
            }
         }

         return -1;
      }
   }

   public boolean ensureXpInHotbarSlot() {
      if (mc.player != null && mc.interactionManager != null) {
         if (this.hasXPInOffhand()) {
            return true;
         } else if (!this.isPickaxeLowDurability()) {
            return false;
         } else {
            int targetHotbarSlot = ((Double)this.XPSlot.getValue()).intValue() - 1;
            ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);
            if (hotbarStack.getItem() == Items.EXPERIENCE_BOTTLE) {
               return true;
            } else {
               int xpSlot = -1;

               for(int i = 0; i < 36; ++i) {
                  ItemStack stack = mc.player.getInventory().getStack(i);
                  if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
                     xpSlot = i;
                     break;
                  }
               }

               if (xpSlot == -1) {
                  this.state = State.BUYXP;
                  return false;
               } else if (!(mc.currentScreen instanceof InventoryScreen)) {
                  mc.execute(() -> {
                     mc.setScreen(new InventoryScreen(mc.player));
                  });
                  return false;
               } else {
                  PlayerScreenHandler handler = mc.player.playerScreenHandler;
                  int syncId = handler.syncId;
                  int fromSlot = convertSlotIndex(xpSlot);
                  int toSlot = convertSlotIndex(targetHotbarSlot);
                  mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
                  mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
                  this.shouldCloseInventory = true;
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public boolean ensurePearlInHotbarSlot() {
      if (mc.player != null && mc.interactionManager != null) {
         int targetHotbarSlot = ((Double)this.PearlSlot.getValue()).intValue() - 1;
         ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);
         if (hotbarStack.getItem() == Items.ENDER_PEARL) {
            return true;
         } else {
            int pearlSlot = -1;

            for(int i = 0; i < 36; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() == Items.ENDER_PEARL) {
                  pearlSlot = i;
                  break;
               }
            }

            if (pearlSlot == -1) {
               this.state = State.BUYPEARL;
               return false;
            } else if (!(mc.currentScreen instanceof InventoryScreen)) {
               mc.execute(() -> {
                  mc.setScreen(new InventoryScreen(mc.player));
               });
               return false;
            } else {
               PlayerScreenHandler handler = mc.player.playerScreenHandler;
               int syncId = handler.syncId;
               int fromSlot = convertSlotIndex(pearlSlot);
               int toSlot = convertSlotIndex(targetHotbarSlot);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               this.shouldCloseInventory = true;
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean ensureGoldenCarrotInHotbarSlot() {
      if (mc.player != null && mc.interactionManager != null) {
         int targetHotbarSlot = ((Double)this.CarrotSlot.getValue()).intValue() - 1;
         ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);
         if (hotbarStack.getItem() == Items.GOLDEN_CARROT) {
            return true;
         } else {
            int carrotSlot = -1;

            for(int i = 0; i < 36; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() == Items.GOLDEN_CARROT) {
                  carrotSlot = i;
                  break;
               }
            }

            if (carrotSlot == -1) {
               this.state = State.BUYCARROT;
               return false;
            } else if (!(mc.currentScreen instanceof InventoryScreen)) {
               mc.execute(() -> {
                  mc.setScreen(new InventoryScreen(mc.player));
               });
               return false;
            } else {
               PlayerScreenHandler handler = mc.player.playerScreenHandler;
               int syncId = handler.syncId;
               int fromSlot = convertSlotIndex(carrotSlot);
               int toSlot = convertSlotIndex(targetHotbarSlot);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               this.shouldCloseInventory = true;
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public boolean ensureObsidianInHotbarSlot() {
      if (mc.player != null && mc.interactionManager != null) {
         int targetHotbarSlot = ((Double)this.BlockSlot.getValue()).intValue() - 1;
         ItemStack hotbarStack = mc.player.getInventory().getStack(targetHotbarSlot);
         if (hotbarStack.getItem() == Items.OBSIDIAN) {
            return true;
         } else {
            int obsidianSlot = -1;

            for(int i = 0; i < 36; ++i) {
               ItemStack stack = mc.player.getInventory().getStack(i);
               if (stack.getItem() == Items.OBSIDIAN) {
                  obsidianSlot = i;
                  break;
               }
            }

            if (obsidianSlot == -1) {
               this.state = State.BUYOBI;
               return false;
            } else if (!(mc.currentScreen instanceof InventoryScreen)) {
               mc.execute(() -> {
                  mc.setScreen(new InventoryScreen(mc.player));
               });
               return false;
            } else {
               PlayerScreenHandler handler = mc.player.playerScreenHandler;
               int syncId = handler.syncId;
               int fromSlot = convertSlotIndex(obsidianSlot);
               int toSlot = convertSlotIndex(targetHotbarSlot);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, toSlot, 0, SlotActionType.PICKUP, mc.player);
               mc.interactionManager.clickSlot(syncId, fromSlot, 0, SlotActionType.PICKUP, mc.player);
               this.shouldCloseInventory = true;
               return false;
            }
         }
      } else {
         return false;
      }
   }

   public void handleMend() {
      switch(this.mendStage) {
      case ENSURE:
         if (!this.ensureXpInHotbarSlot()) {
            return;
         }

         this.mendStage = MendStage.ROTATE_DOWN;
         break;
      case ROTATE_DOWN:
         if ((double)Math.abs(mc.player.getPitch() - 90.0F) > 0.05D) {
            if (!this.isRotating) {
               this.rotateCharacter.rotate(mc.player.getYaw(), 90.0F, () -> {
                  mc.player.getInventory().selectedSlot = ((Double)this.XPSlot.getValue()).intValue() - 1;
                  this.mendStage = MendStage.OFFHAND_XP;
               });
            }
         } else {
            this.mendStage = MendStage.OFFHAND_XP;
         }
         break;
      case OFFHAND_XP:
         if (this.hasXPInOffhand()) {
            this.mendStage = MendStage.THROW_XP;
            return;
         }

         PlayerScreenHandler handler = mc.player.playerScreenHandler;
         int sync = handler.syncId;
         int offHandSlot = 45;
         this.mendingGraceTicks = 40;
         mc.interactionManager.clickSlot(sync, convertSlotIndex(((Double)this.XPSlot.getValue()).intValue() - 1), 0, SlotActionType.PICKUP, mc.player);
         mc.interactionManager.clickSlot(sync, offHandSlot, 0, SlotActionType.PICKUP, mc.player);
         if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.TOTEM_OF_UNDYING) {
            mc.interactionManager.clickSlot(sync, convertSlotIndex(((Double)this.XPSlot.getValue()).intValue() - 1), 0, SlotActionType.PICKUP, mc.player);
         }

         this.mendStage = MendStage.THROW_XP;
         break;
      case THROW_XP:
         if (!this.hasXPInOffhand() || this.isOffhandEmpty()) {
            this.mendStage = MendStage.REOFFHAND_TOTEM;
            return;
         }

         this.mendingGraceTicks = 40;
         this.updateUsage(true);
         mc.player.getInventory().selectedSlot = this.findPickaxeInHotbar();
         break;
      case REOFFHAND_TOTEM:
         this.updateUsage(false);
         if (!this.hasTotemInOffhand()) {
            this.offhandTotem(this.findTotemInInventory());
            this.mendingGraceTicks = 40;
            return;
         }

         this.mendStage = MendStage.ROTATE_BACK;
         break;
      case ROTATE_BACK:
         Vec2f values = this.getValues(this.currentDirection);
         if (!this.isRotating) {
            this.rotateCharacter.rotate(values.x, values.y, () -> {
               mc.player.getInventory().selectedSlot = this.findPickaxeInHotbar();
               this.mendStage = MendStage.RESET;
            });
         }
         break;
      case RESET:
         this.mendStage = MendStage.ENSURE;
         this.state = this.backup;
         this.backup = State.NONE;
      }

   }

   private void disconnect(String text) {
      if (mc.player != null) {
         this.toggle();
         mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("TunnelBaseFinder | " + text)));
      }

   }

   public Direction getDir() {
      PlayerEntity player = mc.player;
      if (player == null) {
         return Direction.NORTH;
      } else {
         float yaw = player.getYaw();
         float pitch = player.getPitch();
         if (pitch > 60.0F) {
            return Direction.DOWN;
         } else if (pitch < -60.0F) {
            return Direction.UP;
         } else {
            float wrappedYaw = yaw % 360.0F;
            if (wrappedYaw < 0.0F) {
               wrappedYaw += 360.0F;
            }

            if (wrappedYaw >= 45.0F && wrappedYaw < 135.0F) {
               return Direction.WEST;
            } else if (wrappedYaw >= 135.0F && wrappedYaw < 225.0F) {
               return Direction.NORTH;
            } else {
               return wrappedYaw >= 225.0F && wrappedYaw < 315.0F ? Direction.EAST : Direction.SOUTH;
            }
         }
      }
   }

   public Vec2f getValues(Direction direction) {
      float yaw = 0.0F;
      float pitch = ((String)this.mode.getValue()).equals("Standing") ? 45.0F : 0.0F;
      switch(direction) {
      case NORTH:
         yaw = 180.0F;
         break;
      case SOUTH:
         yaw = 0.0F;
         break;
      case WEST:
         yaw = 90.0F;
         break;
      case EAST:
         yaw = 270.0F;
         break;
      case UP:
         yaw = 0.0F;
         pitch = -90.0F;
         break;
      case DOWN:
         yaw = 0.0F;
         pitch = 90.0F;
      }

      return new Vec2f(yaw, pitch);
   }

   public void handleXPBuy() {
      switch(this.xpbuystate) {
      case NONE:
         this.xpbuystate = XpBuyState.OPENSHOP;
         break;
      case OPENSHOP:
         mc.getNetworkHandler().sendChatCommand("shop");
         this.xpbuystate = XpBuyState.WAIT1;
         this.xpwaitcounter = 0;
         break;
      case WAIT1:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.CLICKGEAR;
         }
         break;
      case CLICKGEAR:
         this.clickShopItem(11, Items.END_STONE, 13, XpBuyState.CLICKXP, XpBuyState.WAIT2);
         break;
      case WAIT2:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.CLICKXP;
         }
         break;
      case CLICKXP:
         this.clickShopItem(16, Items.EXPERIENCE_BOTTLE, 16, XpBuyState.CLICKSTACK, XpBuyState.WAIT3);
         break;
      case WAIT3:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.CLICKSTACK;
         }
         break;
      case CLICKSTACK:
         this.clickShopItem(17, Items.LIME_STAINED_GLASS_PANE, 17, XpBuyState.DROPITEMS, XpBuyState.WAIT4);
         break;
      case WAIT4:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.DROPITEMS;
         }
         break;
      case DROPITEMS:
         if (this.isInventoryFull()) {
            this.dropJunkStack();
         }

         this.xpbuystate = XpBuyState.WAIT5;
         this.xpwaitcounter = 0;
         break;
      case WAIT5:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.BUY;
         }
         break;
      case BUY:
         Screen var2 = mc.currentScreen;
         if (var2 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)var2;
            if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(23).hasStack()) {
               mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, 23, 0, SlotActionType.PICKUP, mc.player);
            }
         }

         this.xpbuystate = XpBuyState.WAIT6;
         this.xpwaitcounter = 0;
         break;
      case WAIT6:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.CLOSE;
         }
         break;
      case CLOSE:
         if (mc.currentScreen != null) {
            mc.execute(() -> {
               mc.currentScreen.close();
            });
         }

         this.xpbuystate = XpBuyState.WAIT7;
         this.xpwaitcounter = 0;
         break;
      case WAIT7:
         if (this.xpwaitcounter++ >= 7) {
            this.xpbuystate = XpBuyState.RESET;
         }
         break;
      case RESET:
         this.xpbuystate = XpBuyState.NONE;
         this.state = State.AUTOMEND;
      }

   }

   private void clickShopItem(int checkSlot, Item checkItem, int clickSlot, Object nextState, Object waitState) {
      Screen var7 = mc.currentScreen;
      if (var7 instanceof GenericContainerScreen) {
         GenericContainerScreen screen = (GenericContainerScreen)var7;
         if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(checkSlot).getStack().isOf(checkItem)) {
            mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, clickSlot, 0, SlotActionType.PICKUP, mc.player);
            if (nextState instanceof XpBuyState) {
               XpBuyState s = (XpBuyState)nextState;
               this.xpbuystate = s;
            }
         } else if (nextState instanceof XpBuyState) {
            this.xpbuystate = XpBuyState.NONE;
         }
      } else if (nextState instanceof XpBuyState) {
         this.xpbuystate = XpBuyState.NONE;
      }

      if (waitState instanceof XpBuyState) {
         XpBuyState s = (XpBuyState)waitState;
         this.xpbuystate = s;
      }

      this.xpwaitcounter = 0;
   }

   private boolean hasTotemInOffhand() {
      return mc.player != null && mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING;
   }

   private boolean isOffhandEmpty() {
      return mc.player != null && mc.player.getOffHandStack().isEmpty();
   }

   private boolean hasXPInOffhand() {
      return mc.player != null && mc.player.getOffHandStack().getItem() == Items.EXPERIENCE_BOTTLE;
   }

   private int findTotemInInventory() {
      if (mc.player == null) {
         return -1;
      } else {
         for(int i = 0; i < mc.player.getInventory().size(); ++i) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.TOTEM_OF_UNDYING) {
               return i;
            }
         }

         return -1;
      }
   }

   private void offhandTotem(int slot) {
      if (mc.player != null && mc.interactionManager != null && slot != -1) {
         if (!(mc.currentScreen instanceof InventoryScreen)) {
            mc.execute(() -> {
               mc.setScreen(new InventoryScreen(mc.player));
            });
         } else {
            PlayerScreenHandler handler = mc.player.playerScreenHandler;
            int syncId = handler.syncId;
            int offHandSlot = 45;
            mc.interactionManager.clickSlot(syncId, convertSlotIndex(slot), 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, offHandSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(syncId, convertSlotIndex(slot), 0, SlotActionType.PICKUP, mc.player);
            mc.execute(() -> {
               if (mc.currentScreen != null) {
                  mc.currentScreen.close();
               }

            });
         }
      }
   }

   private boolean isPickaxeLowDurability() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack mainHand = mc.player.getMainHandStack();
         if (!this.isPickaxe(mainHand.getItem())) {
            return false;
         } else {
            return mainHand.getDamage() >= mainHand.getMaxDamage() - 10;
         }
      }
   }

   private boolean isInventoryFull() {
      if (mc.player == null) {
         return false;
      } else {
         for(int i = 0; i < 36; ++i) {
            if (mc.player.getInventory().getStack(i).isEmpty()) {
               return false;
            }
         }

         return true;
      }
   }

   private void dropJunkStack() {
      if (mc.player != null) {
         for(int i = 0; i < 36; ++i) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && this.isJunkItem(stack.getItem())) {
               mc.player.getInventory().removeStack(i);
               return;
            }
         }

      }
   }

   private boolean isJunkItem(Item item) {
      Item[] var2 = JUNK_ITEMS;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Item junk = var2[var4];
         if (item == junk) {
            return true;
         }
      }

      return false;
   }

   private void updateMining(boolean mining) {
      if (this.resetMiningTick > 0) {
         --this.resetMiningTick;
      } else {
         mc.options.attackKey.setPressed(mining);
         if (!mining) {
            this.resetMiningTick = 2;
         }

      }
   }

   private void updateUsage(boolean using) {
      if (this.resetUseTick > 0) {
         --this.resetUseTick;
      } else {
         mc.options.useKey.setPressed(using);
         if (!using) {
            this.resetUseTick = 2;
         }

      }
   }

   public void handleEating() {
      if (this.ensureGoldenCarrotInHotbarSlot()) {
         int carrotSlot = ((Double)this.CarrotSlot.getValue()).intValue() - 1;
         mc.player.getInventory().selectedSlot = carrotSlot;
         this.updateUsage(true);
         if (mc.player.getHungerManager().getFoodLevel() >= 18) {
            this.updateUsage(false);
            mc.player.getInventory().selectedSlot = this.findPickaxeInHotbar();
            this.state = this.backup;
            this.backup = State.NONE;
         }

      }
   }

   public void handlePearlBuy() {
      switch(this.pearlbuystate) {
      case NONE:
         this.pearlbuystate = PearlBuyState.OPENSHOP;
         break;
      case OPENSHOP:
         mc.getNetworkHandler().sendChatCommand("shop");
         this.pearlbuystate = PearlBuyState.WAIT1;
         this.pearlwaitcounter = 0;
         break;
      case WAIT1:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.CLICKGEAR;
         }
         break;
      case CLICKGEAR:
         this.clickPearlShopItem(11, Items.END_STONE, 13);
         this.pearlbuystate = PearlBuyState.WAIT2;
         this.pearlwaitcounter = 0;
         break;
      case WAIT2:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.CLICKPEARL;
         }
         break;
      case CLICKPEARL:
         this.clickPearlShopItem(13, Items.ENDER_PEARL, 13);
         this.pearlbuystate = PearlBuyState.WAIT3;
         this.pearlwaitcounter = 0;
         break;
      case WAIT3:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.CLICKSTACK;
         }
         break;
      case CLICKSTACK:
         this.clickPearlShopItem(17, Items.LIME_STAINED_GLASS_PANE, 17);
         this.pearlbuystate = PearlBuyState.WAIT4;
         this.pearlwaitcounter = 0;
         break;
      case WAIT4:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.DROPITEMS;
         }
         break;
      case DROPITEMS:
         if (this.isInventoryFull()) {
            this.dropJunkStack();
         }

         this.pearlbuystate = PearlBuyState.WAIT5;
         this.pearlwaitcounter = 0;
         break;
      case WAIT5:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.BUY;
         }
         break;
      case BUY:
         Screen var2 = mc.currentScreen;
         if (var2 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)var2;
            if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(23).hasStack()) {
               mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, 23, 0, SlotActionType.PICKUP, mc.player);
            }
         }

         this.pearlbuystate = PearlBuyState.WAIT6;
         this.pearlwaitcounter = 0;
         break;
      case WAIT6:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.CLOSE;
         }
         break;
      case CLOSE:
         if (mc.currentScreen != null) {
            mc.execute(() -> {
               mc.currentScreen.close();
            });
         }

         this.pearlbuystate = PearlBuyState.WAIT7;
         this.pearlwaitcounter = 0;
         break;
      case WAIT7:
         if (this.pearlwaitcounter++ >= 7) {
            this.pearlbuystate = PearlBuyState.RESET;
         }
         break;
      case RESET:
         this.pearlbuystate = PearlBuyState.NONE;
         this.state = State.PEARL;
      }

   }

   private void clickPearlShopItem(int checkSlot, Item checkItem, int clickSlot) {
      Screen var5 = mc.currentScreen;
      if (var5 instanceof GenericContainerScreen) {
         GenericContainerScreen screen = (GenericContainerScreen)var5;
         if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(checkSlot).getStack().isOf(checkItem)) {
            mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, clickSlot, 0, SlotActionType.PICKUP, mc.player);
         }
      }

   }

   public void handleObiBuy() {
      switch(this.obibuystate) {
      case NONE:
         this.obibuystate = ObiBuyState.OPENSHOP;
         break;
      case OPENSHOP:
         mc.getNetworkHandler().sendChatCommand("shop");
         this.obibuystate = ObiBuyState.WAIT1;
         this.obiwaitcounter = 0;
         break;
      case WAIT1:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.CLICKGEAR;
         }
         break;
      case CLICKGEAR:
         this.clickObiShopItem(11, Items.BRICKS, 11);
         this.obibuystate = ObiBuyState.WAIT2;
         this.obiwaitcounter = 0;
         break;
      case WAIT2:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.CLICKOBI;
         }
         break;
      case CLICKOBI:
         this.clickObiShopItem(11, Items.OBSIDIAN, 11);
         this.obibuystate = ObiBuyState.WAIT3;
         this.obiwaitcounter = 0;
         break;
      case WAIT3:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.CLICKSTACK;
         }
         break;
      case CLICKSTACK:
         this.clickObiShopItem(17, Items.LIME_STAINED_GLASS_PANE, 17);
         this.obibuystate = ObiBuyState.WAIT4;
         this.obiwaitcounter = 0;
         break;
      case WAIT4:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.DROPITEMS;
         }
         break;
      case DROPITEMS:
         if (this.isInventoryFull()) {
            this.dropJunkStack();
         }

         this.obibuystate = ObiBuyState.WAIT5;
         this.obiwaitcounter = 0;
         break;
      case WAIT5:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.BUY;
         }
         break;
      case BUY:
         Screen var2 = mc.currentScreen;
         if (var2 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)var2;
            if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(23).hasStack()) {
               mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, 23, 0, SlotActionType.PICKUP, mc.player);
            }
         }

         this.obibuystate = ObiBuyState.WAIT6;
         this.obiwaitcounter = 0;
         break;
      case WAIT6:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.CLOSE;
         }
         break;
      case CLOSE:
         if (mc.currentScreen != null) {
            mc.execute(() -> {
               mc.currentScreen.close();
            });
         }

         this.obibuystate = ObiBuyState.WAIT7;
         this.obiwaitcounter = 0;
         break;
      case WAIT7:
         if (this.obiwaitcounter++ >= 7) {
            this.obibuystate = ObiBuyState.RESET;
         }
         break;
      case RESET:
         this.obibuystate = ObiBuyState.NONE;
         this.state = this.backup;
         this.backup = State.NONE;
      }

   }

   private void clickObiShopItem(int checkSlot, Item checkItem, int clickSlot) {
      Screen var5 = mc.currentScreen;
      if (var5 instanceof GenericContainerScreen) {
         GenericContainerScreen screen = (GenericContainerScreen)var5;
         if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(checkSlot).getStack().isOf(checkItem)) {
            mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, clickSlot, 0, SlotActionType.PICKUP, mc.player);
         }
      }

   }

   public void handleCarrotBuy() {
      switch(this.carrotbuystate) {
      case NONE:
         this.carrotbuystate = CarrotBuyState.OPENSHOP;
         break;
      case OPENSHOP:
         mc.getNetworkHandler().sendChatCommand("shop");
         this.carrotbuystate = CarrotBuyState.WAIT1;
         this.carrotwaitcounter = 0;
         break;
      case WAIT1:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.CLICKGEAR;
         }
         break;
      case CLICKGEAR:
         this.clickCarrotShopItem(11, Items.COOKED_BEEF, 12);
         this.carrotbuystate = CarrotBuyState.WAIT2;
         this.carrotwaitcounter = 0;
         break;
      case WAIT2:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.CLICKCARROT;
         }
         break;
      case CLICKCARROT:
         this.clickCarrotShopItem(13, Items.GOLDEN_CARROT, 13);
         this.carrotbuystate = CarrotBuyState.WAIT3;
         this.carrotwaitcounter = 0;
         break;
      case WAIT3:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.CLICKSTACK;
         }
         break;
      case CLICKSTACK:
         this.clickCarrotShopItem(17, Items.LIME_STAINED_GLASS_PANE, 17);
         this.carrotbuystate = CarrotBuyState.WAIT4;
         this.carrotwaitcounter = 0;
         break;
      case WAIT4:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.DROPITEMS;
         }
         break;
      case DROPITEMS:
         if (this.isInventoryFull()) {
            this.dropJunkStack();
         }

         this.carrotbuystate = CarrotBuyState.WAIT5;
         this.carrotwaitcounter = 0;
         break;
      case WAIT5:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.BUY;
         }
         break;
      case BUY:
         Screen var2 = mc.currentScreen;
         if (var2 instanceof GenericContainerScreen) {
            GenericContainerScreen screen = (GenericContainerScreen)var2;
            if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(23).hasStack()) {
               mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, 23, 0, SlotActionType.PICKUP, mc.player);
            }
         }

         this.carrotbuystate = CarrotBuyState.WAIT6;
         this.carrotwaitcounter = 0;
         break;
      case WAIT6:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.CLOSE;
         }
         break;
      case CLOSE:
         if (mc.currentScreen != null) {
            mc.execute(() -> {
               mc.currentScreen.close();
            });
         }

         this.carrotbuystate = CarrotBuyState.WAIT7;
         this.carrotwaitcounter = 0;
         break;
      case WAIT7:
         if (this.carrotwaitcounter++ >= 7) {
            this.carrotbuystate = CarrotBuyState.RESET;
         }
         break;
      case RESET:
         this.carrotbuystate = CarrotBuyState.NONE;
         this.state = State.AUTOEAT;
      }

   }

   private void clickCarrotShopItem(int checkSlot, Item checkItem, int clickSlot) {
      Screen var5 = mc.currentScreen;
      if (var5 instanceof GenericContainerScreen) {
         GenericContainerScreen screen = (GenericContainerScreen)var5;
         if (((GenericContainerScreenHandler)screen.getScreenHandler()).getSlot(checkSlot).getStack().isOf(checkItem)) {
            mc.interactionManager.clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, clickSlot, 0, SlotActionType.PICKUP, mc.player);
         }
      }

   }

   public void handleGoAbove() {
      this.state = State.MINING;
   }

   public void handleYRecovery() {
      this.state = State.MINING;
   }

   public void handlePearl() {
      if (this.ensurePearlInHotbarSlot()) {
         int pearlSlot = ((Double)this.PearlSlot.getValue()).intValue() - 1;
         mc.player.getInventory().selectedSlot = pearlSlot;
         this.updateUsage(true);
         this.state = State.MINING;
      }
   }

   public void handleMining() {
      if (mc.player != null && mc.world != null) {
         if (!this.isHoldingPickaxe()) {
            int pickaxeSlot = this.findPickaxeInHotbar();
            if (pickaxeSlot == -1) {
               this.disconnect("NO PICKAXE FOUND");
               return;
            }

            mc.player.getInventory().selectedSlot = pickaxeSlot;
         }

         if (((String)this.mode.getValue()).equals("Crawl")) {
            mc.player.setPose(EntityPose.SWIMMING);
         }

         mc.options.forwardKey.setPressed(true);
         this.updateMining(true);
         BlockPos currentPos = mc.player.getBlockPos();
         if (this.lastCoords != null && this.lastCoords.equals(currentPos)) {
            ++this.stuckTicks;
            if (this.stuckTicks > 100) {
               this.stuckTicks = 0;
            }
         } else {
            this.stuckTicks = 0;
         }

         this.lastCoords = currentPos;
      }
   }

   static {
      JUNK_ITEMS = new Item[]{Items.STONE, Items.COBBLESTONE, Items.DEEPSLATE, Items.COBBLED_DEEPSLATE, Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.TUFF, Items.CALCITE, Items.DRIPSTONE_BLOCK, Items.POINTED_DRIPSTONE, Items.GRAVEL, Items.FLINT, Items.DIRT, Items.GRASS_BLOCK, Items.COARSE_DIRT, Items.ROOTED_DIRT, Items.NETHERRACK, Items.BLACKSTONE, Items.BASALT, Items.SMOOTH_BASALT, Items.END_STONE, Items.COAL, Items.RAW_IRON, Items.RAW_COPPER, Items.COBWEB, Items.STRING, Items.CLAY, Items.TOTEM_OF_UNDYING};
   }
}
