package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BindSetting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.Iterator;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "SafeAnchorMacro",
   category = Category.COMBAT,
   desc = "Places anchor, charges it, protects you with glowstone, and explodes"
)
public class SafeAnchorMacro extends Function {
   private final BindSetting triggerKey = new BindSetting("Trigger Key", 71);
   private final NumberSetting switchDelay = new NumberSetting("Switch Delay", this, 2.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting totemSlot = new NumberSetting("Totem Slot", this, 9.0D, 1.0D, 9.0D, 1.0D);
   private final NumberSetting range = new NumberSetting("Range", this, 4.5D, 3.0D, 6.0D, 0.10000000149011612D);
   private final BooleanSetting smoothRotations = new BooleanSetting("Smooth Rotations", true);
   private int delayCounter = 0;
   private int step = 0;
   private boolean isActive = false;
   private BlockPos targetAnchorPos = null;
   private BlockPos protectionBlockPos = null;

   public SafeAnchorMacro() {
      this.addSettings(new Setting[]{this.triggerKey, this.switchDelay, this.totemSlot, this.range, this.smoothRotations});
   }

   public void onDisable() {
      super.onDisable();
      this.reset();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (mc.currentScreen == null && mc.player != null) {
         if (!this.hasRequiredItems()) {
            if (this.isActive) {
               this.reset();
            }

         } else if (this.isActive || this.checkTriggerKey()) {
            if (this.delayCounter < (int)this.switchDelay.getValueFloat()) {
               ++this.delayCounter;
            } else {
               this.delayCounter = 0;
               switch(this.step) {
               case 0:
                  if (!this.findTargetPosition()) {
                     this.reset();
                     return;
                  }

                  ++this.step;
                  break;
               case 1:
                  if (!this.placeBlock(this.targetAnchorPos, Items.RESPAWN_ANCHOR)) {
                     this.reset();
                     return;
                  }

                  ++this.step;
                  break;
               case 2:
                  if (!this.chargeAnchor(this.targetAnchorPos)) {
                     this.reset();
                     return;
                  }

                  ++this.step;
                  break;
               case 3:
                  if (!this.isReplaceable(this.protectionBlockPos)) {
                     ++this.step;
                  } else {
                     BlockPos below = this.protectionBlockPos.down();
                     if (this.isReplaceable(below)) {
                        ++this.step;
                     } else {
                        this.placeBlock(this.protectionBlockPos, Items.GLOWSTONE);
                        ++this.step;
                     }
                  }
                  break;
               case 4:
                  this.swapToSlot((int)this.totemSlot.getValueFloat() - 1);
                  ++this.step;
                  break;
               case 5:
                  if (this.hasLootNearby(this.targetAnchorPos)) {
                     this.reset();
                     return;
                  }

                  if (!this.interactWithBlock(this.targetAnchorPos)) {
                     this.reset();
                     return;
                  }

                  ++this.step;
                  break;
               case 6:
                  this.reset();
               }

            }
         }
      }
   }

   private boolean findTargetPosition() {
      HitResult var2 = mc.crosshairTarget;
      if (var2 instanceof BlockHitResult) {
         BlockHitResult hitResult = (BlockHitResult)var2;
         BlockPos hitPos = hitResult.getBlockPos();
         if (this.isReplaceable(hitPos)) {
            this.targetAnchorPos = hitPos;
         } else {
            Direction side = hitResult.getSide();
            this.targetAnchorPos = hitPos.offset(side);
         }

         double distance = mc.player.getBlockPos().toCenterPos().distanceTo(Vec3d.ofCenter(this.targetAnchorPos));
         if (distance > (double)this.range.getValueFloat()) {
            return false;
         } else {
            Vec3d playerPos = mc.player.getBlockPos().toCenterPos();
            Vec3d anchorPos = Vec3d.ofCenter(this.targetAnchorPos);
            Vec3d midpoint = playerPos.add(anchorPos).multiply(0.5D);
            BlockPos protectionPos = new BlockPos((int)Math.floor(midpoint.x), mc.player.getBlockY(), (int)Math.floor(midpoint.z));
            if (protectionPos.equals(this.targetAnchorPos) || protectionPos.equals(mc.player.getBlockPos())) {
               float yaw = mc.player.getYaw() % 360.0F;
               if (yaw < 0.0F) {
                  yaw += 360.0F;
               }

               Direction facing;
               if (!(yaw >= 315.0F) && !(yaw < 45.0F)) {
                  if (yaw >= 45.0F && yaw < 135.0F) {
                     facing = Direction.WEST;
                  } else if (yaw >= 135.0F && yaw < 225.0F) {
                     facing = Direction.NORTH;
                  } else {
                     facing = Direction.EAST;
                  }
               } else {
                  facing = Direction.SOUTH;
               }

               protectionPos = mc.player.getBlockPos().offset(facing);
               if (protectionPos.equals(this.targetAnchorPos)) {
                  protectionPos = mc.player.getBlockPos();
               }
            }

            this.protectionBlockPos = protectionPos;
            return true;
         }
      } else {
         return false;
      }
   }

   private boolean isReplaceable(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState blockState = mc.world.getBlockState(pos);
         Block block = blockState.getBlock();
         if (blockState.isAir()) {
            return true;
         } else {
            return block == Blocks.SHORT_GRASS || block == Blocks.TALL_GRASS || block == Blocks.FERN || block == Blocks.WATER || block == Blocks.LAVA || blockState.isReplaceable();
         }
      }
   }

   private boolean placeBlock(BlockPos pos, Item item) {
      if (pos != null && this.isReplaceable(pos)) {
         this.swapToItem(item);
         BlockHitResult hitResult = this.findPlacementSide(pos);
         if (hitResult == null) {
            return false;
         } else {
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
            return true;
         }
      } else {
         return false;
      }
   }

   private BlockHitResult findPlacementSide(BlockPos pos) {
      Direction[] directions = new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
      Direction[] var3 = directions;
      int var4 = directions.length;

      for(int var5 = 0; var5 < var4; ++var5) {
         Direction dir = var3[var5];
         BlockPos neighbor = pos.offset(dir);
         if (!this.isReplaceable(neighbor)) {
            return new BlockHitResult(Vec3d.ofCenter(neighbor).add((double)dir.getOpposite().getVector().getX() * 0.5D, (double)dir.getOpposite().getVector().getY() * 0.5D, (double)dir.getOpposite().getVector().getZ() * 0.5D), dir.getOpposite(), neighbor, false);
         }
      }

      return null;
   }

   private boolean interactWithBlock(BlockPos pos) {
      if (pos == null) {
         return false;
      } else {
         BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         return true;
      }
   }

   private boolean chargeAnchor(BlockPos pos) {
      if (pos != null && !this.isReplaceable(pos)) {
         this.swapToItem(Items.GLOWSTONE);
         BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
         return true;
      } else {
         return false;
      }
   }

   private void swapToItem(Item item) {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).getItem() == item) {
            mc.player.getInventory().selectedSlot = i;
            return;
         }
      }

   }

   private void swapToSlot(int slot) {
      if (slot >= 0 && slot < 9) {
         mc.player.getInventory().selectedSlot = slot;
      }

   }

   private boolean hasRequiredItems() {
      boolean hasAnchor = false;
      boolean hasGlowstone = false;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem().equals(Items.RESPAWN_ANCHOR)) {
            hasAnchor = true;
         }

         if (stack.getItem().equals(Items.GLOWSTONE)) {
            hasGlowstone = true;
         }
      }

      return hasAnchor && hasGlowstone;
   }

   private boolean checkTriggerKey() {
      if (!this.triggerKey.isPressed()) {
         return false;
      } else {
         this.isActive = true;
         return true;
      }
   }

   private void reset() {
      this.isActive = false;
      this.step = 0;
      this.delayCounter = 0;
      this.targetAnchorPos = null;
      this.protectionBlockPos = null;
   }

   private boolean hasLootNearby(BlockPos pos) {
      if (mc.world != null && pos != null) {
         double searchRadius = 10.0D;
         Box searchBox = new Box((double)pos.getX() - searchRadius, (double)pos.getY() - searchRadius, (double)pos.getZ() - searchRadius, (double)pos.getX() + searchRadius, (double)pos.getY() + searchRadius, (double)pos.getZ() + searchRadius);
         Iterator var5 = mc.world.getOtherEntities((Entity)null, searchBox).iterator();

         Item item;
         do {
            ItemStack stack;
            do {
               Entity entity;
               do {
                  if (!var5.hasNext()) {
                     return false;
                  }

                  entity = (Entity)var5.next();
               } while(!(entity instanceof ItemEntity));

               ItemEntity itemEntity = (ItemEntity)entity;
               stack = itemEntity.getStack();
            } while(stack.isEmpty());

            item = stack.getItem();
         } while(item != Items.TOTEM_OF_UNDYING && item != Items.NETHERITE_HELMET && item != Items.NETHERITE_CHESTPLATE && item != Items.NETHERITE_LEGGINGS && item != Items.NETHERITE_BOOTS && item != Items.DIAMOND_HELMET && item != Items.DIAMOND_CHESTPLATE && item != Items.DIAMOND_LEGGINGS && item != Items.DIAMOND_BOOTS && item != Items.NETHERITE_SWORD && item != Items.DIAMOND_SWORD);

         return true;
      } else {
         return false;
      }
   }
}
