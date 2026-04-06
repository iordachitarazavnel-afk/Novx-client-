package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.player.EventAttackEntity;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

@ModuleInfo(
   name = "NoInteract",
   category = Category.COMBAT,
   desc = "Prevents you from certain actions"
)
public class NoInteract extends Function {
   private final BooleanSetting doubleGlowstone = new BooleanSetting("Double Glowstone", false);
   private final BooleanSetting obiPunch = new BooleanSetting("Obi Punch", false);
   private final BooleanSetting echestClick = new BooleanSetting("E-chest click", false);
   private final BooleanSetting anvilClick = new BooleanSetting("Anvil click", false);
   private boolean shouldCancelInteraction = false;

   public NoInteract() {
      this.addSettings(new Setting[]{this.doubleGlowstone, this.obiPunch, this.echestClick, this.anvilClick});
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      HitResult var3 = mc.crosshairTarget;
      if (var3 instanceof BlockHitResult) {
         BlockHitResult hit = (BlockHitResult)var3;
         if (this.isBlockAtPosition(hit.getBlockPos(), Blocks.OBSIDIAN) && (Boolean)this.obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL)) {
            event.cancel();
         }
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         this.shouldCancelInteraction = false;
         HitResult var3 = mc.crosshairTarget;
         if (var3 instanceof BlockHitResult) {
            BlockHitResult hit = (BlockHitResult)var3;
            BlockPos pos = hit.getBlockPos();
            if (this.isAnchorCharged(pos) && (Boolean)this.doubleGlowstone.getValue() && mc.player.isHolding(Items.GLOWSTONE)) {
               this.shouldCancelInteraction = true;
               return;
            }

            if (this.isBlockAtPosition(pos, Blocks.OBSIDIAN) && (Boolean)this.obiPunch.getValue() && mc.player.isHolding(Items.END_CRYSTAL)) {
               this.shouldCancelInteraction = true;
               return;
            }

            if (this.isBlockAtPosition(pos, Blocks.ENDER_CHEST) && (Boolean)this.echestClick.getValue()) {
               this.shouldCancelInteraction = true;
               return;
            }

            if ((this.isBlockAtPosition(pos, Blocks.ANVIL) || this.isBlockAtPosition(pos, Blocks.CHIPPED_ANVIL) || this.isBlockAtPosition(pos, Blocks.DAMAGED_ANVIL)) && (Boolean)this.anvilClick.getValue()) {
               this.shouldCancelInteraction = true;
            }
         }

      }
   }

   private boolean isBlockAtPosition(BlockPos pos, Block block) {
      if (mc.world == null) {
         return false;
      } else {
         return mc.world.getBlockState(pos).getBlock() == block;
      }
   }

   private boolean isAnchorCharged(BlockPos pos) {
      if (mc.world == null) {
         return false;
      } else {
         BlockState state = mc.world.getBlockState(pos);
         if (state.getBlock() instanceof RespawnAnchorBlock) {
            return (Integer)state.get(RespawnAnchorBlock.CHARGES) > 0;
         } else {
            return false;
         }
      }
   }
}
