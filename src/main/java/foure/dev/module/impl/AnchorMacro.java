package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
   name = "AnchorMacro",
   category = Category.COMBAT,
   desc = "Advanced respawn anchor automation with randomization"
)
public class AnchorMacro extends Function {
   private final BooleanSetting whileUse = new BooleanSetting("While Use", false);
   private final BooleanSetting lootProtect = new BooleanSetting("Loot Protect", false);
   private final NumberSetting switchDelay = new NumberSetting("Switch Delay", this, 1.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting switchChance = new NumberSetting("Switch Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final NumberSetting placeChance = new NumberSetting("Place Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final NumberSetting glowstoneDelay = new NumberSetting("Glowstone Delay", this, 0.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting glowstoneChance = new NumberSetting("Glowstone Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final NumberSetting explodeDelay = new NumberSetting("Explode Delay", this, 1.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting explodeChance = new NumberSetting("Explode Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final NumberSetting explodeSlot = new NumberSetting("Explode Slot", this, 9.0D, 1.0D, 9.0D, 1.0D);
   private final BooleanSetting onlyOwn = new BooleanSetting("Only Own", false);
   private final BooleanSetting onlyCharge = new BooleanSetting("Only Charge", false);
   private final Set<BlockPos> ownedAnchors = new HashSet();
   private final Random random = new Random();
   private int switchClock = 0;
   private int glowstoneClock = 0;
   private int explodeClock = 0;

   public AnchorMacro() {
      this.addSettings(new Setting[]{this.whileUse, this.lootProtect, this.placeChance, this.switchDelay, this.switchChance, this.glowstoneDelay, this.glowstoneChance, this.explodeDelay, this.explodeChance, this.explodeSlot, this.onlyOwn, this.onlyCharge});
   }

   public void onEnable() {
      super.onEnable();
      this.switchClock = 0;
      this.glowstoneClock = 0;
      this.explodeClock = 0;
      this.ownedAnchors.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.ownedAnchors.clear();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck() && mc.currentScreen == null) {
         if ((Boolean)this.whileUse.getValue() || !mc.player.isUsingItem() && !this.isTool(mc.player.getOffHandStack())) {
            if (!(Boolean)this.lootProtect.getValue() || !this.isValuableLootNearby()) {
               if (this.isKeyPressed(1)) {
                  HitResult hitResult = mc.crosshairTarget;
                  if (hitResult instanceof BlockHitResult) {
                     BlockHitResult hit = (BlockHitResult)hitResult;
                     BlockPos pos = hit.getBlockPos();
                     if (this.isBlockAtPosition(pos, Blocks.RESPAWN_ANCHOR)) {
                        if (!(Boolean)this.onlyOwn.getValue() || this.ownedAnchors.contains(pos)) {
                           mc.options.useKey.setPressed(false);
                           if (this.isAnchorNotCharged(pos)) {
                              this.handleUnchargedAnchor(hit, pos);
                           }

                           if (this.isAnchorCharged(pos)) {
                              this.handleChargedAnchor(hit, pos);
                           }

                           this.trackAnchorPlacement(hit);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void handleUnchargedAnchor(BlockHitResult hit, BlockPos pos) {
      if (!((float)this.randomInt(1, 100) > this.placeChance.getValueFloat())) {
         if (mc.player.getMainHandStack().getItem() != Items.GLOWSTONE) {
            if ((float)this.switchClock < this.switchDelay.getValueFloat()) {
               ++this.switchClock;
            } else {
               if ((float)this.randomInt(1, 100) <= this.switchChance.getValueFloat()) {
                  this.switchClock = 0;
                  this.swapToItem(Items.GLOWSTONE);
               }

            }
         } else {
            if (mc.player.getMainHandStack().getItem() == Items.GLOWSTONE) {
               if ((float)this.glowstoneClock < this.glowstoneDelay.getValueFloat()) {
                  ++this.glowstoneClock;
                  return;
               }

               if ((float)this.randomInt(1, 100) <= this.glowstoneChance.getValueFloat()) {
                  this.glowstoneClock = 0;
                  mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(), hit);
               }
            }

         }
      }
   }

   private void handleChargedAnchor(BlockHitResult hit, BlockPos pos) {
      int slot = (int)this.explodeSlot.getValueFloat() - 1;
      if (mc.player.getInventory().selectedSlot != slot) {
         if ((float)this.switchClock < this.switchDelay.getValueFloat()) {
            ++this.switchClock;
         } else {
            if ((float)this.randomInt(1, 100) <= this.switchChance.getValueFloat()) {
               this.switchClock = 0;
               mc.player.getInventory().selectedSlot = slot;
            }

         }
      } else {
         if (mc.player.getInventory().selectedSlot == slot) {
            if ((float)this.explodeClock < this.explodeDelay.getValueFloat()) {
               ++this.explodeClock;
               return;
            }

            if ((float)this.randomInt(1, 100) <= this.explodeChance.getValueFloat()) {
               this.explodeClock = 0;
               if (!(Boolean)this.onlyCharge.getValue()) {
                  mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(), hit);
                  this.ownedAnchors.remove(pos);
               }
            }
         }

      }
   }

   private void trackAnchorPlacement(BlockHitResult hit) {
      if (hit.getType() == Type.BLOCK) {
         if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
            Direction dir = hit.getSide();
            BlockPos pos = hit.getBlockPos();
            if (!mc.world.getBlockState(pos).isReplaceable()) {
               this.ownedAnchors.add(pos.offset(dir));
            } else {
               this.ownedAnchors.add(pos);
            }
         }

         BlockPos bp = hit.getBlockPos();
         if (this.isAnchorCharged(bp)) {
            this.ownedAnchors.remove(bp);
         }
      }

   }

   private boolean isTool(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         Item item = stack.getItem();
         return item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL;
      }
   }

   private boolean isValuableLootNearby() {
      if (mc.world == null) {
         return false;
      } else {
         double searchRadius = 10.0D;
         Box searchBox = new Box(mc.player.getX() - searchRadius, mc.player.getY() - searchRadius, mc.player.getZ() - searchRadius, mc.player.getX() + searchRadius, mc.player.getY() + searchRadius, mc.player.getZ() + searchRadius);
         Iterator var4 = mc.world.getOtherEntities((Entity)null, searchBox).iterator();

         Item item;
         do {
            ItemStack stack;
            do {
               Entity entity;
               do {
                  if (!var4.hasNext()) {
                     return false;
                  }

                  entity = (Entity)var4.next();
               } while(!(entity instanceof ItemEntity));

               ItemEntity itemEntity = (ItemEntity)entity;
               stack = itemEntity.getStack();
            } while(stack.isEmpty());

            item = stack.getItem();
         } while(item != Items.TOTEM_OF_UNDYING && item != Items.NETHERITE_INGOT && item != Items.DIAMOND && item != Items.ENCHANTED_GOLDEN_APPLE && item != Items.NETHERITE_HELMET && item != Items.NETHERITE_CHESTPLATE && item != Items.NETHERITE_LEGGINGS && item != Items.NETHERITE_BOOTS && item != Items.NETHERITE_SWORD && item != Items.NETHERITE_AXE);

         return true;
      }
   }

   private boolean isKeyPressed(int key) {
      if (key <= 8) {
         return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key) == 1;
      } else {
         return GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == 1;
      }
   }

   private boolean isBlockAtPosition(BlockPos blockPos, Block block) {
      return mc.world.getBlockState(blockPos).getBlock() == block;
   }

   private boolean isAnchorCharged(BlockPos blockPos) {
      return this.isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) && (Integer)mc.world.getBlockState(blockPos).get(RespawnAnchorBlock.CHARGES) != 0;
   }

   private boolean isAnchorNotCharged(BlockPos blockPos) {
      return this.isBlockAtPosition(blockPos, Blocks.RESPAWN_ANCHOR) && (Integer)mc.world.getBlockState(blockPos).get(RespawnAnchorBlock.CHARGES) == 0;
   }

   private void swapToItem(Item item) {
      for(int i = 0; i < 9; ++i) {
         if (mc.player.getInventory().getStack(i).isOf(item)) {
            mc.player.getInventory().selectedSlot = i;
            return;
         }
      }

   }

   private int randomInt(int min, int max) {
      return this.random.nextInt(max - min + 1) + min;
   }
}
