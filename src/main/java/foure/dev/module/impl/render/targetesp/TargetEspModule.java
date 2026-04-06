package foure.dev.module.impl.render.targetesp;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.render.Render3DEvent;
import foure.dev.event.impl.render.RenderEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.impl.combat.Killaura;
import foure.dev.module.impl.render.targetesp.modes.TargetEspCircle;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.entity.LivingEntity;

@ModuleInfo(
   name = "TargetESP",
   category = Category.RENDER,
   desc = "Visuals for your target"
)
public class TargetEspModule extends Function {
   private static TargetEspModule instance;
   public final ModeSetting mode = new ModeSetting("Mode", this, "Circle", new String[]{"Circle"});
   public final NumberSetting circleSpeed = new NumberSetting("Speed", this, 2.0D, 0.1D, 10.0D, 0.1D);
   public final NumberSetting circleSize = new NumberSetting("Size", this, 1.0D, 0.1D, 3.0D, 0.1D);
   public final BooleanSetting circleBloom = new BooleanSetting("Bloom", this, true);
   public final NumberSetting circleBloomSize = new NumberSetting("Bloom Size", this, 0.5D, 0.1D, 2.0D, 0.1D);
   public final BooleanSetting circleRedOnImpact = new BooleanSetting("Red on Impact", this, true);
   public final NumberSetting circleImpactFadeIn = new NumberSetting("Impact Fade In", this, 0.5D, 0.1D, 1.0D, 0.1D);
   public final NumberSetting circleImpactFadeOut = new NumberSetting("Impact Fade Out", this, 0.1D, 0.01D, 1.0D, 0.01D);
   public final NumberSetting circleImpactIntensity = new NumberSetting("Impact Intensity", this, 1.0D, 0.1D, 1.0D, 0.1D);
   public final NumberSetting showAnimation = new NumberSetting("Alpha Multiplier", this, 1.0D, 0.1D, 1.0D, 0.1D);
   private TargetEspMode currentMode;

   public TargetEspModule() {
      instance = this;
      this.addSettings(new Setting[]{this.mode, this.circleSpeed, this.circleSize, this.circleBloom, this.circleBloomSize, this.circleRedOnImpact, this.circleImpactFadeIn, this.circleImpactFadeOut, this.circleImpactIntensity, this.showAnimation});
      this.updateMode();
   }

   public static TargetEspModule getInstance() {
      return instance;
   }

   private void updateMode() {
      if (this.mode.is("Circle") && !(this.currentMode instanceof TargetEspCircle)) {
         this.currentMode = new TargetEspCircle();
      }

   }

   @Subscribe
   public void onUpdate(EventUpdate e) {
      this.updateMode();
      if (this.currentMode != null) {
         this.currentMode.updateTarget();
         this.currentMode.onUpdate();
      }

   }

   @Subscribe
   public void onRender3D(Render3DEvent e) {
      if (this.currentMode != null) {
         this.currentMode.onRender3D(e);
      }

   }

   @Subscribe
   public void onRender2D(RenderEvent e) {
      if (this.currentMode != null) {
         this.currentMode.onRender2D(e);
      }

   }

   public LivingEntity getTarget() {
      Killaura killaura = (Killaura)FourEClient.getInstance().getFunctionManager().getModule(Killaura.class);
      return killaura != null && killaura.isToggled() ? killaura.getTarget() : null;
   }
}
