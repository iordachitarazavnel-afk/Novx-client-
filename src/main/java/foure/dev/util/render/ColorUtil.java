package foure.dev.util.render;

import java.awt.Color;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.DropperBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.entity.TrappedChestBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.entity.vehicle.HopperMinecartEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

public class ColorUtil {
   public static final int GOLD = -22016;
   public static final int PURPLE = -10533692;
   private static final int[] rainbowList = new int[]{-39325, -85692, -131689, -6365026, -9584157, -3368503, -3178806};

   public static Color getBlockColor(BlockEntity blockEntity) {
      if (!(blockEntity instanceof ChestBlockEntity) && !(blockEntity instanceof TrappedChestBlockEntity) && !(blockEntity instanceof BarrelBlockEntity)) {
         if (blockEntity instanceof EnderChestBlockEntity) {
            return new Color(-10533692, true);
         } else if (blockEntity instanceof ShulkerBoxBlockEntity) {
            return new Color(-16711681);
         } else {
            return !(blockEntity instanceof AbstractFurnaceBlockEntity) && !(blockEntity instanceof HopperBlockEntity) && !(blockEntity instanceof DropperBlockEntity) && !(blockEntity instanceof DispenserBlockEntity) ? null : Color.GRAY;
         }
      } else {
         return new Color(-22016, true);
      }
   }

   public static Color getEntityColor(Entity entity) {
      if (entity instanceof AbstractHorseEntity) {
         return new Color(-22016, true);
      } else if (!(entity instanceof ChestBoatEntity) && !(entity instanceof ChestMinecartEntity)) {
         if (!(entity instanceof ItemFrameEntity) && !(entity instanceof ArmorStandEntity)) {
            return entity instanceof HopperMinecartEntity ? Color.GRAY : null;
         } else {
            return Color.WHITE;
         }
      } else {
         return new Color(-22016, true);
      }
   }

   public static Color changeAlpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
   }

   public static int changeAlpha(int color, int alpha) {
      return color & 16777215 | alpha << 24;
   }

   public static int lerpValue(int value, int targetValue, double timeDivisor) {
      double curTime = (double)Util.getMeasuringTimeMs() / timeDivisor;
      float lerpedAmount = MathHelper.abs(MathHelper.sin((double)((float)curTime)));
      return (int)((float)value + (float)(targetValue - value) * lerpedAmount);
   }

   public static int getDynamicFadeVal() {
      return lerpValue(30, 120, 1000.0D);
   }
}
