package foure.dev.module;

import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.impl.config.ConfigModule;
import foure.dev.module.impl.combat.AnchorMacro;
import foure.dev.module.impl.combat.MaceSwap;
import foure.dev.module.impl.combat.DoubleAnchor;
import foure.dev.module.impl.combat.AttributeSwapper;
import foure.dev.module.impl.combat.TotemHit;
import foure.dev.module.impl.combat.AnchorExploderV2;
import foure.dev.module.impl.combat.AutoCrystal;
import foure.dev.module.impl.combat.AutoCrystalV2;
import foure.dev.module.impl.combat.AutoDoubleHand;
import foure.dev.module.impl.combat.AutoWeapon;
import foure.dev.module.impl.combat.CrystalOptimizer;
import foure.dev.module.impl.combat.AnchorPlacerV2;
import foure.dev.module.impl.combat.AutoTotemV2;
import foure.dev.module.impl.combat.HoverTotem;
import foure.dev.module.impl.combat.InvTotem;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.combat.NoInteract;
import foure.dev.module.impl.combat.PearlThrow;
import foure.dev.module.impl.combat.PopCounter;
import foure.dev.module.impl.combat.SafeAnchorMacro;
import foure.dev.module.impl.combat.TriggerBot;
import foure.dev.module.impl.config.ConfigModule;
import foure.dev.module.impl.donut.AutoBoneOrder;
import foure.dev.module.impl.donut.AutoLog;
import foure.dev.module.impl.donut.HideScoreboard;
import foure.dev.module.impl.donut.SilentHome;
import foure.dev.module.impl.misc.AutoFish;
import foure.dev.module.impl.misc.AutoTool;
import foure.dev.module.impl.misc.DebugPanelModule;
import foure.dev.module.impl.misc.FakePlayer;
import foure.dev.module.impl.misc.NameProtect;
import foure.dev.module.impl.misc.Compass;
import foure.dev.module.impl.misc.FriendAdder;
import foure.dev.module.impl.movement.AutoEat;
import foure.dev.module.impl.movement.ElytraAutoFly;
import foure.dev.module.impl.movement.AutoFirework;
import foure.dev.module.impl.movement.ShortPearl;
import foure.dev.module.impl.movement.AutoJumpReset;
import foure.dev.module.impl.movement.AutoSprint;
import foure.dev.module.impl.movement.Blink;
import foure.dev.module.impl.movement.Freecam;
import foure.dev.module.impl.movement.GuiMove;
import foure.dev.module.impl.movement.NoDelay;
import foure.dev.module.impl.movement.PlacementOptimizer;
import foure.dev.module.impl.movement.TargetPearl;
import foure.dev.module.impl.render.ClusterESP;
import foure.dev.module.impl.render.EditHudModule;
import foure.dev.module.impl.render.HUD;
import foure.dev.module.impl.render.HitParticles;
import foure.dev.module.impl.render.JumpCircles;
import foure.dev.module.impl.render.MobESP;
import foure.dev.module.impl.render.NameTags;
import foure.dev.module.impl.render.PlayerESP;
import foure.dev.module.impl.render.RenderTest;
import foure.dev.module.impl.render.ShulkerVisible;
import foure.dev.module.impl.render.StorageESP;
import foure.dev.module.impl.render.TargetHUD;
import foure.dev.module.impl.render.TestModule;
import foure.dev.module.impl.render.WatermarkModule;
import foure.dev.module.impl.render.BlockESP;
import foure.dev.module.impl.render.targetesp.TargetEspModule;
import foure.dev.module.impl.basefinds.ChunkFinder;
import foure.dev.module.impl.basefinds.PrimeChunkFinder;
import foure.dev.module.impl.basefinds.GrowthFinder;
import foure.dev.module.impl.basefinds.ActivityDebug;
import foure.dev.module.impl.basefinds.ColonFinder;
import foure.dev.module.impl.visual.FullBright;
import foure.dev.module.impl.config.ConfigModule;
import foure.dev.ui.clickgui.NovxClickGui;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Generated;

public class FunctionManager {
   private final List<Function> modules = new ArrayList();

   public FunctionManager() {
      this.modules.addAll(Arrays.asList(new AutoSprint(), new GuiMove(), new Freecam(), new NoDelay(), new AutoEat(), new AutoFirework(), new AutoJumpReset(), new Blink(), new TargetPearl(), new AnchorMacro(), new AutoCrystal(), new AutoDoubleHand(), new AutoLog(), new AutoWeapon(), new CrystalOptimizer(), new HoverTotem(), new InvTotem(), new Killaura(), new NoInteract(), new PearlThrow(), new PlacementOptimizer(), new PopCounter(), new SafeAnchorMacro(), new TriggerBot(), new AutoFish(), new AutoTool(), new DebugPanelModule(), new FakePlayer(), new NameProtect(), new ClusterESP(), new HitParticles(), new MobESP(), new NameTags(), new PlayerESP(), new RenderTest(), new StorageESP(), new TestModule(), new JumpCircles(), new HUD(), new TargetHUD(), new ChunkFinder(), new FullBright(), new ShulkerVisible(), new TargetEspModule(), new HideScoreboard(), new AutoBoneOrder(), new SilentHome(), new EditHudModule(), new WatermarkModule(), new NovxClickGui(), new ActivityDebug(), new BlockESP(), new AttributeSwapper(), new PrimeChunkFinder(),  new TotemHit(), new FriendAdder(), new AnchorExploderV2(), new AutoCrystalV2(), new AutoTotemV2(), new DoubleAnchor(), new ShortPearl(), new MaceSwap(), new GrowthFinder(), new ColonFinder(), new ElytraAutoFly(), new AnchorPlacerV2(), new Compass(), new ConfigModule()));
   }

   public List<Function> getModules(Category category) {
      return this.modules.stream().filter((module) -> {
         return module.getCategory() == category;
      }).toList();
   }

   public <T extends Function> T getModule(Class<T> tClass) {
      return (T) this.modules.stream().filter((module) -> {
         return module.getClass() == tClass;
      }).findFirst().orElse((Function) null);
   }

   @Generated
   public List<Function> getModules() {
      return this.modules;
   }
}
