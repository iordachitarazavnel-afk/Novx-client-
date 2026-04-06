package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.misc.AutoReconnect;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import foure.dev.util.WebhookUtils;
import foure.dev.util.character.RotateCharacter;
import foure.dev.util.others.Friends;
import foure.dev.util.render.utils.ChatUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "SpawnerProtect",
   category = Category.DONUT,
   desc = "Breaks all spawners around you when players are nearby and dumps your inventory in an e-chest"
)
public class SpawnerProtect extends Function {
   private final BooleanSetting fastMode = new BooleanSetting("Fast Mode", this, true);
   private final NumberSetting emergencyDistance = new NumberSetting("Emergency Distance", this, 5.0D, 1.0D, 50.0D, 0.5D);
   private final BooleanSetting webhookEnabled = new BooleanSetting("Webhook", this, false);
   private final StringSetting webhookUrl = new StringSetting("Webhook URL", this, "");
   private final BooleanSetting selfPing = new BooleanSetting("Self Ping", this, false);
   private final StringSetting discordId = new StringSetting("Discord ID", this, "");
   SpawnerProtect.ProtectState state;
   boolean foundPlayer;
   List<BlockPos> spawnerPositions;
   int searchRadius;
   int currentSpawnerIndex;
   int miningTicks;
   boolean isMining;
   int previousSlot;
   BlockPos enderChestPos;
   int dumpSlot;
   int dumpDelay;
   boolean hasOpenedChest;
   int verificationTicks;
   boolean isVerifying;
   RotateCharacter rotateChar;
   private BlockPos lastPosition;
   private boolean isCoordChangeProtected;
   private int coordChangeCooldown;

   public SpawnerProtect() {
      this.state = SpawnerProtect.ProtectState.CHECKING;
      this.foundPlayer = false;
      this.spawnerPositions = new ArrayList();
      this.searchRadius = 10;
      this.currentSpawnerIndex = 0;
      this.miningTicks = 0;
      this.isMining = false;
      this.previousSlot = -1;
      this.enderChestPos = null;
      this.dumpSlot = 0;
      this.dumpDelay = 0;
      this.hasOpenedChest = false;
      this.verificationTicks = 0;
      this.isVerifying = false;
      this.lastPosition = null;
      this.isCoordChangeProtected = false;
      this.coordChangeCooldown = 0;
   }

   public void onEnable() {
      if (mc.player != null) {
         if (this.findPickaxeSlot() == -1) {
            ChatUtils.sendMessage("Please Get A Silk Touch Pickaxe!");
            this.toggle();
         } else {
            if (mc.currentScreen != null) {
               mc.execute(() -> {
                  mc.currentScreen.close();
               });
            }

            int pickaxeSlot = this.findPickaxeSlot();
            if (pickaxeSlot != -1) {
               this.previousSlot = mc.player.getInventory().selectedSlot;
               mc.player.getInventory().selectedSlot = pickaxeSlot;
            }

            this.foundPlayer = false;
            this.spawnerPositions.clear();
            this.state = SpawnerProtect.ProtectState.CHECKING;
            this.rotateChar = new RotateCharacter(mc);
            this.currentSpawnerIndex = 0;
            this.miningTicks = 0;
            this.isMining = false;
            this.enderChestPos = null;
            this.dumpSlot = 0;
            this.dumpDelay = 0;
            this.hasOpenedChest = false;
            this.verificationTicks = 0;
            this.isVerifying = false;
            this.lastPosition = mc.player.getBlockPos();
            this.isCoordChangeProtected = false;
            this.coordChangeCooldown = 0;
            super.onEnable();
         }
      }
   }

   public void onDisable() {
      if (this.previousSlot != -1 && mc.player != null) {
         mc.player.getInventory().selectedSlot = this.previousSlot;
      }

      if (mc.player != null) {
         mc.player.setSneaking(false);
         mc.options.sneakKey.setPressed(false);
      }

      super.onDisable();
   }

   @Subscribe
   public void onRender3D(Render3DEvent event) {
      if (mc.world != null && mc.player != null) {
         if ((this.state == SpawnerProtect.ProtectState.MINING || this.state == SpawnerProtect.ProtectState.OPENENDERCHEST) && this.rotateChar != null) {
            this.rotateChar.update(true, (Boolean)this.fastMode.getValue());
         }

      }
   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      if (mc.world != null && mc.player != null) {
         this.checkForCoordinateChange();
         if (this.state == SpawnerProtect.ProtectState.CHECKING || this.state == SpawnerProtect.ProtectState.FINDSPAWNER || this.state == SpawnerProtect.ProtectState.MINING) {
            mc.player.setSneaking(true);
            mc.options.sneakKey.setPressed(true);
         }

         switch(this.state.ordinal()) {
         case 0:
            this.checkPlayer();
            break;
         case 1:
            this.findSpawners();
            break;
         case 2:
            this.mineSpawners();
            break;
         case 3:
            this.findEnderChest();
            break;
         case 4:
            this.openEnderChest();
            break;
         case 5:
            this.dumpInventory();
         }

      }
   }

   void checkForCoordinateChange() {
      BlockPos currentPosition = mc.player.getBlockPos();
      if (this.lastPosition != null && !currentPosition.equals(this.lastPosition)) {
         this.isCoordChangeProtected = !this.isCoordChangeProtected;
         if (this.isCoordChangeProtected) {
            ChatUtils.sendMessage("Restart Detected");
            this.state = SpawnerProtect.ProtectState.CHECKING;
            this.foundPlayer = false;
            this.spawnerPositions.clear();
            this.currentSpawnerIndex = 0;
            this.isMining = false;
            if (this.previousSlot != -1) {
               mc.player.getInventory().selectedSlot = this.previousSlot;
               this.previousSlot = -1;
            }

            mc.options.attackKey.setPressed(false);
         } else {
            ChatUtils.sendMessage("Resuming");
         }

         this.coordChangeCooldown = 40;
      }

      this.lastPosition = currentPosition;
      if (this.coordChangeCooldown > 0) {
         --this.coordChangeCooldown;
      }

   }

   void checkPlayer() {
      if (!this.isCoordChangeProtected) {
         String selfName = mc.player.getName().getString();
         AutoReconnect autoReconnect = (AutoReconnect)FourEClient.getInstance().getFunctionManager().getModule(AutoReconnect.class);
         Iterator var3 = mc.world.getPlayers().iterator();

         while(var3.hasNext()) {
            PlayerEntity player = (PlayerEntity)var3.next();
            if (player != mc.player) {
               String name = player.getName().getString();
               if (!name.equalsIgnoreCase(selfName) && !name.equalsIgnoreCase("venom") && !player.isSpectator() && !Friends.isFriend(name)) {
                  if (autoReconnect != null && autoReconnect.isToggled()) {
                     autoReconnect.toggle();
                  }

                  double distance = (double)mc.player.distanceTo(player);
                  if ((Double)this.emergencyDistance.getValue() > 0.0D && distance <= (Double)this.emergencyDistance.getValue()) {
                     this.sendWebhook(name, distance);
                     this.disconnect("Emergency Distance Triggered! Player: " + name + " (" + String.format("%.1f", distance) + " blocks)");
                     return;
                  }

                  ChatUtils.sendMessage("Player Detected: " + name);
                  this.sendWebhook(name, distance);
                  this.foundPlayer = true;
                  this.state = SpawnerProtect.ProtectState.FINDSPAWNER;
                  return;
               }
            }
         }

      }
   }

   void findSpawners() {
      this.spawnerPositions.clear();
      BlockPos playerPos = mc.player.getBlockPos();

      for(int x = -this.searchRadius; x <= this.searchRadius; ++x) {
         for(int y = -this.searchRadius; y <= this.searchRadius; ++y) {
            for(int z = -this.searchRadius; z <= this.searchRadius; ++z) {
               BlockPos pos = playerPos.add(x, y, z);
               Block block = mc.world.getBlockState(pos).getBlock();
               if (block == Blocks.SPAWNER) {
                  this.spawnerPositions.add(pos);
               }
            }
         }
      }

      if (this.spawnerPositions.isEmpty()) {
         ChatUtils.sendMessage("No Spawners Found");
         this.disconnect("No Spawners Found");
         this.state = SpawnerProtect.ProtectState.CHECKING;
      } else {
         ChatUtils.sendMessage("Found " + this.spawnerPositions.size() + " Spawners");
         this.currentSpawnerIndex = 0;
         this.state = SpawnerProtect.ProtectState.MINING;
      }

   }

   int findPickaxeSlot() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (this.isPickaxe(stack.getItem())) {
            RegistryEntry<Enchantment> silkTouch = (RegistryEntry)mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getEntry(Identifier.of("minecraft", "silk_touch")).orElse((RegistryEntry.Reference<Enchantment>) null);
            if (silkTouch != null) {
               Iterator var4 = stack.getEnchantments().getEnchantments().iterator();

               while(var4.hasNext()) {
                  RegistryEntry<Enchantment> entry = (RegistryEntry)var4.next();
                  if (entry.matchesKey(Enchantments.SILK_TOUCH)) {
                     return i;
                  }
               }
            }
         }
      }

      return -1;
   }

   private boolean isPickaxe(Item item) {
      return item == Items.WOODEN_PICKAXE || item == Items.STONE_PICKAXE || item == Items.IRON_PICKAXE || item == Items.GOLDEN_PICKAXE || item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE;
   }

   void mineSpawners() {
      if (this.currentSpawnerIndex >= this.spawnerPositions.size()) {
         ChatUtils.sendMessage("Mined Spawners");
         if (this.previousSlot != -1) {
            mc.player.getInventory().selectedSlot = this.previousSlot;
            this.previousSlot = -1;
         }

         mc.options.attackKey.setPressed(false);
         this.state = SpawnerProtect.ProtectState.FINDENDERCHEST;
         this.spawnerPositions.clear();
         this.currentSpawnerIndex = 0;
      } else {
         BlockPos targetPos = (BlockPos)this.spawnerPositions.get(this.currentSpawnerIndex);
         Block block = mc.world.getBlockState(targetPos).getBlock();
         if (block != Blocks.SPAWNER && !this.isVerifying) {
            this.isVerifying = true;
            this.verificationTicks = 0;
            mc.options.attackKey.setPressed(false);
         } else {
            if (this.isVerifying) {
               ++this.verificationTicks;
               if (this.verificationTicks < 10) {
                  return;
               }

               Block verifyBlock = mc.world.getBlockState(targetPos).getBlock();
               if (verifyBlock != Blocks.SPAWNER) {
                  ChatUtils.sendMessage("Spawner " + (this.currentSpawnerIndex + 1) + " Already Broken, Moving To Next");
                  ++this.currentSpawnerIndex;
                  this.isMining = false;
                  this.miningTicks = 0;
                  this.isVerifying = false;
                  this.verificationTicks = 0;
                  return;
               }

               ChatUtils.sendMessage("Spawner " + (this.currentSpawnerIndex + 1) + " Detected After Verification");
               this.isVerifying = false;
               this.verificationTicks = 0;
               this.isMining = false;
            }

            if (!this.isMining && this.previousSlot == -1) {
               int pickaxeSlot = this.findPickaxeSlot();
               if (pickaxeSlot == -1) {
                  ChatUtils.sendMessage("No Silk Touch Pickaxe Found In Hotbar!");
                  this.disconnect("No Silk Touch Pickaxe Found In Hotbar!");
                  this.state = SpawnerProtect.ProtectState.CHECKING;
                  return;
               }

               this.previousSlot = mc.player.getInventory().selectedSlot;
               mc.player.getInventory().selectedSlot = pickaxeSlot;
            }

            Vec3d targetVec = Vec3d.ofCenter(targetPos);
            Vec3d playerVec = mc.player.getEyePos();
            Vec3d direction = targetVec.subtract(playerVec).normalize();
            float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
            float pitch = (float)(-Math.toDegrees(Math.asin(direction.y)));
            if (!this.isMining) {
               mc.options.attackKey.setPressed(false);
               if (!this.rotateChar.isActive()) {
                  this.rotateChar.rotate(yaw, pitch, () -> {
                     this.isMining = true;
                     this.miningTicks = 0;
                  });
               }
            } else {
               ++this.miningTicks;
               mc.options.attackKey.setPressed(true);
               if (this.miningTicks % 5 == 0) {
                  Block currentBlock = mc.world.getBlockState(targetPos).getBlock();
                  if (currentBlock != Blocks.SPAWNER) {
                     int var10000 = this.currentSpawnerIndex + 1;
                     ChatUtils.sendMessage("Mined Spawner " + var10000 + "/" + this.spawnerPositions.size());
                     mc.options.attackKey.setPressed(false);
                     ++this.currentSpawnerIndex;
                     this.isMining = false;
                     this.miningTicks = 0;
                  }
               }
            }

         }
      }
   }

   void findEnderChest() {
      BlockPos playerPos = mc.player.getBlockPos();
      BlockPos nearest = null;
      double nearestDist = Double.MAX_VALUE;

      for(int x = -this.searchRadius; x <= this.searchRadius; ++x) {
         for(int y = -this.searchRadius; y <= this.searchRadius; ++y) {
            for(int z = -this.searchRadius; z <= this.searchRadius; ++z) {
               BlockPos pos = playerPos.add(x, y, z);
               Block block = mc.world.getBlockState(pos).getBlock();
               if (block == Blocks.ENDER_CHEST) {
                  double dist = playerPos.getSquaredDistance(pos);
                  if (dist < nearestDist) {
                     nearestDist = dist;
                     nearest = pos;
                  }
               }
            }
         }
      }

      if (nearest == null) {
         this.disconnect("No E-Chest Found!");
         this.state = SpawnerProtect.ProtectState.CHECKING;
      } else {
         this.enderChestPos = nearest;
         ChatUtils.sendMessage("Found E-Chest at " + nearest.toShortString());
         mc.player.setSneaking(false);
         mc.options.sneakKey.setPressed(false);
         this.state = SpawnerProtect.ProtectState.OPENENDERCHEST;
      }

   }

   void openEnderChest() {
      if (this.enderChestPos == null) {
         this.state = SpawnerProtect.ProtectState.CHECKING;
      } else if (mc.world.getBlockState(this.enderChestPos).getBlock() != Blocks.ENDER_CHEST) {
         this.disconnect("E-Chest vanished!");
         this.state = SpawnerProtect.ProtectState.CHECKING;
      } else {
         Vec3d targetVec = Vec3d.ofCenter(this.enderChestPos);
         Vec3d playerVec = mc.player.getEyePos();
         Vec3d direction = targetVec.subtract(playerVec).normalize();
         float yaw = (float)Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0F;
         float pitch = (float)(-Math.toDegrees(Math.asin(direction.y)));
         if (!this.rotateChar.isActive() && !this.hasOpenedChest) {
            this.rotateChar.rotate(yaw, pitch, () -> {
               BlockHitResult hitResult = new BlockHitResult(Vec3d.ofCenter(this.enderChestPos), Direction.UP, this.enderChestPos, false);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hitResult);
               this.hasOpenedChest = true;
               this.dumpDelay = 10;
               ChatUtils.sendMessage("Opening E-Chest");
            });
         }

         if (this.hasOpenedChest && this.dumpDelay > 0) {
            --this.dumpDelay;
         } else if (this.hasOpenedChest && this.dumpDelay == 0) {
            this.state = SpawnerProtect.ProtectState.DUMPINVENTORY;
            this.dumpSlot = 0;
         }

      }
   }

   void dumpInventory() {
      ScreenHandler handler = mc.player.currentScreenHandler;
      if (handler != null && handler != mc.player.playerScreenHandler) {
         if (this.dumpSlot < 36) {
            int containerSlots = handler.slots.size() - 36;
            int screenSlot = containerSlots + this.dumpSlot;
            mc.interactionManager.clickSlot(handler.syncId, screenSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
            ++this.dumpSlot;
         } else {
            ChatUtils.sendMessage("Dumped Inventory");
            mc.execute(() -> {
               if (mc.player != null) {
                  mc.player.closeHandledScreen();
               }

            });
            this.enderChestPos = null;
            this.hasOpenedChest = false;
            this.dumpSlot = 0;
            this.state = SpawnerProtect.ProtectState.CHECKING;
            this.disconnect("Spawners Saved!");
         }

      } else {
         ChatUtils.sendMessage("E-Chest Not Opened");
         this.hasOpenedChest = false;
         this.state = SpawnerProtect.ProtectState.OPENENDERCHEST;
      }
   }

   private void sendWebhook(String playerName, double distance) {
      if ((Boolean)this.webhookEnabled.getValue() && !((String)this.webhookUrl.getValue()).trim().isEmpty()) {
         String selfPingId = "";
         if ((Boolean)this.selfPing.getValue() && !((String)this.discordId.getValue()).trim().isEmpty()) {
            selfPingId = ((String)this.discordId.getValue()).trim();
         }

         BlockPos pos = mc.player.getBlockPos();
         (new WebhookUtils((String)this.webhookUrl.getValue())).setTitle("Player Detected!").setDescription("A player was detected near spawners").setSelfPing(selfPingId).addUsername(playerName).addCoords(pos).addField("Distance", String.format("%.2f blocks", distance), true).addServer().addTime().send();
      }
   }

   private void disconnect(String text) {
      this.toggle();
      if (mc.player != null && mc.player.networkHandler != null) {
         mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("SpawnerProtect | " + text)));
      }

   }

   static enum ProtectState {
      CHECKING,
      FINDSPAWNER,
      MINING,
      FINDENDERCHEST,
      OPENENDERCHEST,
      DUMPINVENTORY;

      // $FF: synthetic method
      private static SpawnerProtect.ProtectState[] $values() {
         return new SpawnerProtect.ProtectState[]{CHECKING, FINDSPAWNER, MINING, FINDENDERCHEST, OPENENDERCHEST, DUMPINVENTORY};
      }
   }
}
