package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.util.character.RotateCharacter;
import java.util.Iterator;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

@ModuleInfo(
   name = "RTPBaseFinder",
   category = Category.DONUT,
   desc = "Finds bases by digging down"
)
public class RTPBaseFinder extends Function {
   private final ModeSetting RTPLocation = new ModeSetting("Region", this, "Random", new String[]{"Random", "NA East", "NA West", "EU West", "EU Central", "Asia", "Oceania"});
   int chests = 0;
   int hoppers = 0;
   int dispensers = 0;
   int enderChests = 0;
   int shulkers = 0;
   boolean foundSpawner;
   boolean isRotateDown = false;
   RotateCharacter rotator;
   boolean sentRTP = false;
   private long worldNotNullSince = -1L;
   private boolean wasWorldNull = true;
   private boolean pendingRotation = false;
   private boolean firstRotationAfterEnable = true;

   public void onEnable() {
      if (mc.currentScreen != null) {
         mc.execute(() -> {
            mc.currentScreen.close();
         });
      }

      this.worldNotNullSince = -1L;
      this.wasWorldNull = true;
      this.pendingRotation = false;
      this.firstRotationAfterEnable = true;
      super.onEnable();
   }

   public void onDisable() {
      super.onDisable();
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (this.rotator != null) {
         if (this.isRotateDown && !this.rotator.isActive()) {
            this.rotator.rotate(mc.player.getYaw(), 85.45357F, () -> {
               this.isRotateDown = false;
            });
         }

         if (this.isRotateDown && this.rotator.isActive()) {
            this.rotator.update(true, false);
         }
      }

   }

   void sendRTP() {
      String region = (String)this.RTPLocation.getValue();
      byte var4 = -1;
      switch(region.hashCode()) {
      case -1896556950:
         if (region.equals("NA East")) {
            var4 = 0;
         }
         break;
      case -1896016868:
         if (region.equals("NA West")) {
            var4 = 1;
         }
         break;
      case -1719043611:
         if (region.equals("EU Central")) {
            var4 = 3;
         }
         break;
      case -721032385:
         if (region.equals("EU West")) {
            var4 = 2;
         }
         break;
      case 2050282:
         if (region.equals("Asia")) {
            var4 = 4;
         }
         break;
      case 28907126:
         if (region.equals("Oceania")) {
            var4 = 5;
         }
      }

      String var10000;
      switch(var4) {
      case 0:
         var10000 = "rtp east";
         break;
      case 1:
         var10000 = "rtp west";
         break;
      case 2:
         var10000 = "rtp eu west";
         break;
      case 3:
         var10000 = "rtp eu central";
         break;
      case 4:
         var10000 = "rtp asia";
         break;
      case 5:
         var10000 = "rtp oceania";
         break;
      default:
         int r = (int)(Math.random() * 6.0D);
         switch(r) {
         case 0:
            var10000 = "rtp east";
            break;
         case 1:
            var10000 = "rtp west";
            break;
         case 2:
            var10000 = "rtp eu west";
            break;
         case 3:
            var10000 = "rtp eu central";
            break;
         case 4:
            var10000 = "rtp asia";
            break;
         case 5:
            var10000 = "rtp oceania";
            break;
         default:
            var10000 = "rtp east";
         }
      }

      String command = var10000;
      mc.getNetworkHandler().sendChatCommand(command);
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.player != null) {
         if (this.rotator == null) {
            this.rotator = new RotateCharacter(mc);
         }

         boolean isWorldNull = mc.world == null;
         if (isWorldNull) {
            this.worldNotNullSince = -1L;
            this.wasWorldNull = true;
            this.pendingRotation = false;
         } else if (this.wasWorldNull) {
            this.worldNotNullSince = System.currentTimeMillis();
            this.wasWorldNull = false;
         }

         long timeSinceWorldNotNull;
         if (this.pendingRotation && this.worldNotNullSince != -1L) {
            timeSinceWorldNotNull = System.currentTimeMillis() - this.worldNotNullSince;
            if (timeSinceWorldNotNull >= 2500L) {
               this.pendingRotation = false;
               this.isRotateDown = true;
               this.firstRotationAfterEnable = false;
            }
         }

         if (!this.hasTotemInOffhand()) {
            this.disconnect("Totem Popped");
         }

         this.scanForBase();
         if ((int)mc.player.getPitch() == 85) {
            this.isRotateDown = false;
            if (mc.player.getY() > 0.0D) {
               mc.options.attackKey.setPressed(true);
               mc.options.sneakKey.setPressed(true);
            } else {
               mc.options.attackKey.setPressed(false);
               mc.options.sneakKey.setPressed(false);
               if (!this.sentRTP) {
                  this.sendRTP();
                  this.sentRTP = true;
               }
            }
         } else {
            this.sentRTP = false;
            if (!this.isRotateDown) {
               if (this.firstRotationAfterEnable) {
                  this.isRotateDown = true;
                  this.firstRotationAfterEnable = false;
                  this.pendingRotation = false;
               } else if (mc.world != null && this.worldNotNullSince != -1L) {
                  timeSinceWorldNotNull = System.currentTimeMillis() - this.worldNotNullSince;
                  if (timeSinceWorldNotNull >= 2500L) {
                     this.isRotateDown = true;
                     this.pendingRotation = false;
                  } else {
                     this.pendingRotation = true;
                  }
               } else {
                  this.pendingRotation = true;
               }
            }
         }

      }
   }

   private void scanForBase() {
      if (mc.player != null && mc.world != null) {
         this.chests = 0;
         this.hoppers = 0;
         this.dispensers = 0;
         this.enderChests = 0;
         this.shulkers = 0;
         this.foundSpawner = false;
         int radius = 2;
         int chunkX = mc.player.getChunkPos().x;
         int chunkZ = mc.player.getChunkPos().z;

         for(int x = chunkX - radius; x <= chunkX + radius; ++x) {
            for(int z = chunkZ - radius; z <= chunkZ + radius; ++z) {
               if (mc.world.isChunkLoaded(x, z)) {
                  WorldChunk chunk = mc.world.getChunk(x, z);
                  Iterator var7 = chunk.getBlockEntityPositions().iterator();

                  while(var7.hasNext()) {
                     BlockPos entry = (BlockPos)var7.next();
                     BlockEntity be = mc.world.getBlockEntity(entry);
                     if (be != null) {
                        if (be instanceof MobSpawnerBlockEntity) {
                           this.foundSpawner = true;
                        }

                        if (be.getPos().getY() <= 0) {
                           if (be instanceof ChestBlockEntity) {
                              ++this.chests;
                           } else if (be instanceof HopperBlockEntity) {
                              ++this.hoppers;
                           } else if (be instanceof DispenserBlockEntity) {
                              ++this.dispensers;
                           } else if (be instanceof EnderChestBlockEntity) {
                              ++this.enderChests;
                           } else if (be instanceof ShulkerBoxBlockEntity) {
                              ++this.shulkers;
                           }
                        }
                     }
                  }
               }
            }
         }

         boolean foundBase = false;
         String reason = "";
         if (this.chests >= 20) {
            foundBase = true;
            reason = "Chest threshold reached";
         } else if (this.shulkers >= 20) {
            foundBase = true;
            reason = "Shulker threshold reached";
         } else if (this.foundSpawner) {
            foundBase = true;
            reason = "Spawner found";
         }

         if (foundBase) {
            this.disconnect(reason);
         }

      }
   }

   private boolean hasTotemInOffhand() {
      if (mc.player == null) {
         return false;
      } else {
         ItemStack offhandStack = mc.player.getOffHandStack();
         return offhandStack.getItem() == Items.TOTEM_OF_UNDYING;
      }
   }

   private void disconnect(String text) {
      if (mc.world != null && this.worldNotNullSince != -1L) {
         long timeSinceWorldNotNull = System.currentTimeMillis() - this.worldNotNullSince;
         if (timeSinceWorldNotNull >= 2500L) {
            this.toggle();
            if (mc.player != null && mc.player.networkHandler != null) {
               mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("RTPBaseFinder | " + text)));
            }

         }
      }
   }
}
