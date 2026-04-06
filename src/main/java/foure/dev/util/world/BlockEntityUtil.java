package foure.dev.util.world;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;

public class BlockEntityUtil {
   public static Box getBox(BlockEntity be) {
      return be.getWorld() == null ? new Box(be.getPos()) : be.getCachedState().getOutlineShape(be.getWorld(), be.getPos()).getBoundingBox().offset(be.getPos());
   }

   public static void findSurroundingBlockEntities(BlockEntity blockEntity, ArrayList<BlockEntity> surroundingBlockEntities) {
      World world = blockEntity.getWorld();
      if (world != null) {
         List<Vec3i> surroundPositions = Arrays.asList(new Vec3i(1, 0, 0), new Vec3i(0, 0, 1), new Vec3i(-1, 0, 0), new Vec3i(0, 0, -1), new Vec3i(0, 1, 0), new Vec3i(0, -1, 0));
         Iterator var4 = surroundPositions.iterator();

         while(var4.hasNext()) {
            Vec3i surroundPosition = (Vec3i)var4.next();
            BlockPos blockPos = blockEntity.getPos().add(surroundPosition);
            BlockEntity surroundingBlockEntity = world.getBlockEntity(blockPos);
            if (surroundingBlockEntity != null && surroundingBlockEntity.getType() == blockEntity.getType() && !surroundingBlockEntities.contains(surroundingBlockEntity)) {
               surroundingBlockEntities.add(surroundingBlockEntity);
               findSurroundingBlockEntities(surroundingBlockEntity, surroundingBlockEntities);
            }
         }

      }
   }

   public static Box getSurroundingBlockEntitiesBoundingBox(Box initBox, ArrayList<BlockEntity> surroundingBlockEntities) {
      double minX = initBox.minX;
      double minY = initBox.minY;
      double minZ = initBox.minZ;
      double maxX = initBox.maxX;
      double maxY = initBox.maxY;
      double maxZ = initBox.maxZ;

      Box boundingBoxSurrounded;
      for(Iterator var14 = surroundingBlockEntities.iterator(); var14.hasNext(); minZ = Math.min(minZ, boundingBoxSurrounded.minZ)) {
         BlockEntity surroundingBlockEntity = (BlockEntity)var14.next();
         boundingBoxSurrounded = getBox(surroundingBlockEntity);
         maxX = Math.max(maxX, boundingBoxSurrounded.maxX);
         maxY = Math.max(maxY, boundingBoxSurrounded.maxY);
         maxZ = Math.max(maxZ, boundingBoxSurrounded.maxZ);
         minX = Math.min(minX, boundingBoxSurrounded.minX);
         minY = Math.min(minY, boundingBoxSurrounded.minY);
      }

      return new Box(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
