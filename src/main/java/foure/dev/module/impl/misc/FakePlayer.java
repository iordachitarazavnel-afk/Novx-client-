package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity.RemovalReason;

@ModuleInfo(
   name = "FakePlayer",
   desc = "Spawns fake players for testing ESP and other visual modules",
   category = Category.MISC,
   visual = false
)
public class FakePlayer extends Function {
   public final NumberSetting maxFakePlayers = new NumberSetting("Max Players", this, 5.0D, 1.0D, 20.0D, 1.0D);
   public final NumberSetting spawnDistance = new NumberSetting("Spawn Distance", this, 3.0D, 1.0D, 10.0D, 0.5D);
   public final StringSetting playerName = new StringSetting("Player Name", this, "Steve");
   public final BooleanSetting copyRotation = new BooleanSetting("Copy Rotation", this, true);
   public final BooleanSetting copyPose = new BooleanSetting("Copy Pose", this, true);
   private final List<OtherClientPlayerEntity> fakePlayers = new ArrayList();
   private final String[] spawnModes = new String[]{"Front", "Behind", "Left", "Right", "Above"};
   private int spawnModeIndex = 0;

   public FakePlayer() {
      this.addSettings(new Setting[]{this.maxFakePlayers, this.spawnDistance, this.playerName, this.copyRotation, this.copyPose});
   }

   public void onEnable() {
      super.onEnable();
      if (fullNullCheck()) {
         this.toggle();
      } else {
         this.spawnFakePlayer();
      }
   }

   public void onDisable() {
      super.onDisable();
      this.clearFakePlayers();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         this.fakePlayers.removeIf((fakePlayer) -> {
            return mc.world.getEntityById(fakePlayer.getId()) == null;
         });
      }
   }

   public void spawnFakePlayer() {
      if (!fullNullCheck()) {
         if (this.fakePlayers.size() < this.maxFakePlayers.getValueInt()) {
            double dist = (Double)this.spawnDistance.getValue();
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();
            float yaw = mc.player.getYaw();
            double radYaw = Math.toRadians((double)yaw);
            double sinYaw = Math.sin(radYaw);
            double cosYaw = Math.cos(radYaw);
            String mode = this.spawnModes[this.spawnModeIndex];
            this.spawnModeIndex = (this.spawnModeIndex + 1) % this.spawnModes.length;
            byte var18 = -1;
            switch(mode.hashCode()) {
            case 2364455:
               if (mode.equals("Left")) {
                  var18 = 2;
               }
               break;
            case 63058813:
               if (mode.equals("Above")) {
                  var18 = 4;
               }
               break;
            case 68152841:
               if (mode.equals("Front")) {
                  var18 = 0;
               }
               break;
            case 78959100:
               if (mode.equals("Right")) {
                  var18 = 3;
               }
               break;
            case 1986002266:
               if (mode.equals("Behind")) {
                  var18 = 1;
               }
            }

            switch(var18) {
            case 0:
               x += -sinYaw * dist;
               z += cosYaw * dist;
               break;
            case 1:
               x += sinYaw * dist;
               z += -cosYaw * dist;
               break;
            case 2:
               x += cosYaw * dist;
               z += sinYaw * dist;
               break;
            case 3:
               x += -cosYaw * dist;
               z += -sinYaw * dist;
               break;
            case 4:
               y += dist;
            }

            String name = (String)this.playerName.getValue();
            if (name == null || name.trim().isEmpty()) {
               name = "Steve";
            }

            GameProfile profile = new GameProfile(UUID.randomUUID(), name);
            OtherClientPlayerEntity fakePlayer = new OtherClientPlayerEntity(mc.world, profile);
            fakePlayer.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
            fakePlayer.setVelocity(0.0D, 0.0D, 0.0D);
            if ((Boolean)this.copyRotation.getValue()) {
               float playerYaw = mc.player.getYaw();
               float playerPitch = mc.player.getPitch();
               fakePlayer.setYaw(playerYaw);
               fakePlayer.setPitch(playerPitch);
               fakePlayer.headYaw = mc.player.headYaw;
               fakePlayer.bodyYaw = mc.player.bodyYaw;
            }

            if ((Boolean)this.copyPose.getValue()) {
               fakePlayer.setPose(mc.player.getPose());
            }

            fakePlayer.setHealth(20.0F);
            fakePlayer.getInventory().clone(mc.player.getInventory());
            fakePlayer.setInvisible(false);
            fakePlayer.noClip = false;
            mc.world.addEntity(fakePlayer);
            this.fakePlayers.add(fakePlayer);
         }
      }
   }

   public void clearFakePlayers() {
      if (mc.world != null) {
         Iterator var1 = (new ArrayList(this.fakePlayers)).iterator();

         while(var1.hasNext()) {
            OtherClientPlayerEntity fakePlayer = (OtherClientPlayerEntity)var1.next();
            mc.world.removeEntity(fakePlayer.getId(), RemovalReason.DISCARDED);
         }

         this.fakePlayers.clear();
      }
   }
}
