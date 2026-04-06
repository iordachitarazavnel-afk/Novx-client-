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
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

@ModuleInfo(
   name = "AutoCrystal",
   category = Category.COMBAT,
   desc = "Automatically places and breaks end crystals"
)
public class AutoCrystal extends Function {
   private final BindSetting activateKey = new BindSetting("Activate Key", this, -1);
   private final NumberSetting placeDelay = new NumberSetting("Place Delay", this, 2.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting breakDelay = new NumberSetting("Break Delay", this, 2.0D, 0.0D, 20.0D, 1.0D);
   private final NumberSetting placeChance = new NumberSetting("Place Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final NumberSetting breakChance = new NumberSetting("Break Chance", this, 100.0D, 0.0D, 100.0D, 1.0D);
   private final BooleanSetting lootProtect = new BooleanSetting("Loot Protect", false);
   private final BooleanSetting fakePunch = new BooleanSetting("Fake Punch", false);
   private final BooleanSetting damageTick = new BooleanSetting("Damage Tick", false);
   private final BooleanSetting antiWeakness = new BooleanSetting("Anti-Weakness", false);
   private final NumberSetting particleChance = new NumberSetting("Particle Chance", this, 20.0D, 0.0D, 100.0D, 1.0D);
   private final Random random = new Random();
   public boolean crystalling;
   private int placeClock;
   private int breakClock;

   public AutoCrystal() {
      this.addSettings(new Setting[]{this.activateKey, this.placeDelay, this.breakDelay, this.placeChance, this.breakChance, this.lootProtect, this.fakePunch, this.damageTick, this.antiWeakness, this.particleChance});
   }

   public void onEnable() {
      super.onEnable();
      this.placeClock = 0;
      this.breakClock = 0;
      this.crystalling = false;
   }

   public void onDisable() {
      super.onDisable();
      this.placeClock = 0;
      this.breakClock = 0;
      this.crystalling = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck() && mc.currentScreen == null) {
         boolean dontPlace = this.placeClock != 0;
         boolean dontBreak = this.breakClock != 0;
         if (!(Boolean)this.lootProtect.getValue() || !this.isValuableLootNearby()) {
            int randomInt = this.random.nextInt(100) + 1;
            if (dontPlace) {
               --this.placeClock;
            }

            if (dontBreak) {
               --this.breakClock;
            }

            if (!mc.player.isDead() && (!(Boolean)this.damageTick.getValue() || !this.damageTickCheck())) {
               if (!this.activateKey.isPressed()) {
                  this.placeClock = 0;
                  this.breakClock = 0;
                  this.crystalling = false;
               } else {
                  this.crystalling = true;
                  if (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL) {
                     HitResult target = mc.crosshairTarget;
                     boolean isObsidian;
                     if (target instanceof BlockHitResult) {
                        BlockHitResult hit = (BlockHitResult)target;
                        if (hit.getType() == Type.BLOCK) {
                           BlockPos pos = hit.getBlockPos();
                           isObsidian = mc.world.getBlockState(pos).isOf(Blocks.OBSIDIAN);
                           boolean isBedrock = mc.world.getBlockState(pos).isOf(Blocks.BEDROCK);
                           boolean canPlace = (isObsidian || isBedrock) && this.canPlaceCrystal(pos);
                           if (!dontPlace && (float)randomInt <= this.placeChance.getValueFloat()) {
                              if (canPlace) {
                                 mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                                 this.placeClock = (int)this.placeDelay.getValueFloat();
                              }

                              if ((Boolean)this.fakePunch.getValue() && !dontBreak && (float)randomInt <= this.breakChance.getValueFloat()) {
                                 if (isObsidian || isBedrock) {
                                    return;
                                 }

                                 mc.interactionManager.attackBlock(pos, hit.getSide());
                                 mc.player.swingHand(Hand.MAIN_HAND);
                                 this.breakClock = (int)this.breakDelay.getValueFloat();
                              }
                           }
                        }
                     }

                     randomInt = this.random.nextInt(100) + 1;
                     if (target instanceof EntityHitResult) {
                        EntityHitResult hit = (EntityHitResult)target;
                        if (!dontBreak && (float)randomInt <= this.breakChance.getValueFloat()) {
                           Entity entity = hit.getEntity();
                           isObsidian = entity instanceof EndCrystalEntity || entity instanceof SlimeEntity;
                           if (!(Boolean)this.fakePunch.getValue() && !isObsidian) {
                              return;
                           }

                           if (isObsidian) {
                              int previousSlot = mc.player.getInventory().selectedSlot;
                              if ((Boolean)this.antiWeakness.getValue() && this.cantBreakCrystal()) {
                                 this.swapToWeapon();
                              }

                              mc.interactionManager.attackEntity(mc.player, entity);
                              mc.player.swingHand(Hand.MAIN_HAND);
                              this.breakClock = (int)this.breakDelay.getValueFloat();
                              if ((Boolean)this.antiWeakness.getValue()) {
                                 mc.player.getInventory().selectedSlot = previousSlot;
                              }
                           }
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private boolean canPlaceCrystal(BlockPos pos) {
      BlockPos above = pos.up();
      BlockPos above2 = pos.up(2);
      return mc.world.getBlockState(above).isAir() && mc.world.getBlockState(above2).isAir() && mc.world.getOtherEntities((Entity)null, new Box(above)).isEmpty();
   }

   private boolean cantBreakCrystal() {
      if (mc.player == null) {
         return false;
      } else {
         boolean hasWeakness = mc.player.hasStatusEffect(StatusEffects.WEAKNESS);
         boolean hasStrength = mc.player.hasStatusEffect(StatusEffects.STRENGTH);
         if (!hasWeakness) {
            return false;
         } else {
            int weaknessLvl = mc.player.getStatusEffect(StatusEffects.WEAKNESS).getAmplifier();
            int strengthLvl = hasStrength ? mc.player.getStatusEffect(StatusEffects.STRENGTH).getAmplifier() : -1;
            return strengthLvl <= weaknessLvl && !this.isTool(mc.player.getMainHandStack());
         }
      }
   }

   private boolean isTool(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         Item item = stack.getItem();
         return item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE || item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE || item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL;
      }
   }

   private void swapToWeapon() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.DIAMOND_SWORD || stack.getItem() == Items.NETHERITE_SWORD) {
            mc.player.getInventory().selectedSlot = i;
            return;
         }
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
         } while(item != Items.TOTEM_OF_UNDYING && item != Items.NETHERITE_INGOT && item != Items.DIAMOND && item != Items.ENCHANTED_GOLDEN_APPLE);

         return true;
      }
   }

   private boolean damageTickCheck() {
      return false;
   }
}
