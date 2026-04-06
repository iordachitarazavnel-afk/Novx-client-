package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.player.EventAttackEntity;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.ModeSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(
   name = "TriggerBot",
   category = Category.COMBAT,
   desc = "Automatically hits players for you"
)
public class TriggerBot extends Function {
   private final ModeSetting mode = new ModeSetting("Mode", "Weapons", new String[]{"Mace", "Weapons", "Mace and Weapons", "All Items"});
   private final BooleanSetting inScreen = new BooleanSetting("Work In Screen", false);
   private final BooleanSetting whileUse = new BooleanSetting("While Use", false);
   private final BooleanSetting onLeftClick = new BooleanSetting("On Left Click", false);
   private final NumberSetting swordDelayMin = new NumberSetting("Sword Delay Min", this, 540.0D, 1.0D, 1000.0D, 1.0D);
   private final NumberSetting swordDelayMax = new NumberSetting("Sword Delay Max", this, 550.0D, 1.0D, 1000.0D, 1.0D);
   private final NumberSetting axeDelayMin = new NumberSetting("Axe Delay Min", this, 780.0D, 1.0D, 1000.0D, 1.0D);
   private final NumberSetting axeDelayMax = new NumberSetting("Axe Delay Max", this, 800.0D, 1.0D, 1000.0D, 1.0D);
   private final BooleanSetting checkShield = new BooleanSetting("Check Shield", false);
   private final BooleanSetting onlyCritSword = new BooleanSetting("Only Crit Sword", false);
   private final BooleanSetting onlyCritAxe = new BooleanSetting("Only Crit Axe", false);
   private final BooleanSetting swing = new BooleanSetting("Swing Hand", true);
   private final BooleanSetting whileAscend = new BooleanSetting("While Ascending", false);
   private final BooleanSetting clickSimulation = new BooleanSetting("Click Simulation", true);
   private final BooleanSetting strayBypass = new BooleanSetting("Bypass Mode", true);
   private final BooleanSetting allEntities = new BooleanSetting("All Entities", false);
   private final BooleanSetting sticky = new BooleanSetting("Same Player", false);
   private long lastAttackTime = 0L;
   private int currentSwordDelay;
   private int currentAxeDelay;
   private boolean isMaceAttack;
   private Entity lastTarget = null;

   public TriggerBot() {
      this.addSettings(new Setting[]{this.mode, this.inScreen, this.whileUse, this.onLeftClick, this.swordDelayMin, this.swordDelayMax, this.axeDelayMin, this.axeDelayMax, this.checkShield, this.whileAscend, this.sticky, this.onlyCritSword, this.onlyCritAxe, this.swing, this.clickSimulation, this.strayBypass, this.allEntities});
   }

   public void onEnable() {
      super.onEnable();
      this.currentSwordDelay = this.getRandomDelay(this.swordDelayMin, this.swordDelayMax);
      this.currentAxeDelay = this.getRandomDelay(this.axeDelayMin, this.axeDelayMax);
      this.lastAttackTime = System.currentTimeMillis();
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      try {
         if (!this.canRun()) {
            return;
         }

         Item item = mc.player.getMainHandStack().getItem();
         if (!this.isItemAllowed(item)) {
            return;
         }

         if (this.isMaceItem(item)) {
            this.handleMace();
         } else if (this.isSwordItem(item)) {
            this.handleSword();
         } else if (this.isAxeItem(item)) {
            this.handleAxe();
         } else {
            this.handleAllItems();
         }
      } catch (Exception var3) {
      }

   }

   private boolean canRun() {
      if (!(Boolean)this.inScreen.getValue() && mc.currentScreen != null) {
         return false;
      } else if ((Boolean)this.onLeftClick.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) {
         return false;
      } else if (!(Boolean)this.whileUse.getValue() && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 1) == 1) {
         return false;
      } else {
         return (Boolean)this.whileAscend.getValue() || mc.player.isOnGround() || !(mc.player.getVelocity().y > 0.0D);
      }
   }

   private boolean isItemAllowed(Item item) {
      String modeValue = (String)this.mode.getValue();
      byte var4 = -1;
      switch(modeValue.hashCode()) {
      case -1406985801:
         if (modeValue.equals("Weapons")) {
            var4 = 1;
         }
         break;
      case 2390294:
         if (modeValue.equals("Mace")) {
            var4 = 0;
         }
         break;
      case 72755265:
         if (modeValue.equals("All Items")) {
            var4 = 3;
         }
         break;
      case 1122540644:
         if (modeValue.equals("Mace and Weapons")) {
            var4 = 2;
         }
      }

      boolean var10000;
      switch(var4) {
      case 0:
         var10000 = this.isMaceItem(item);
         break;
      case 1:
         var10000 = this.isSwordItem(item) || this.isAxeItem(item);
         break;
      case 2:
         var10000 = this.isMaceItem(item) || this.isSwordItem(item) || this.isAxeItem(item);
         break;
      case 3:
         var10000 = true;
         break;
      default:
         var10000 = false;
      }

      return var10000;
   }

   private boolean isMaceItem(Item item) {
      return item == Items.MACE;
   }

   private boolean isSwordItem(Item item) {
      return item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD || item == Items.IRON_SWORD || item == Items.GOLDEN_SWORD || item == Items.STONE_SWORD || item == Items.WOODEN_SWORD;
   }

   private boolean isAxeItem(Item item) {
      return item == Items.NETHERITE_AXE || item == Items.DIAMOND_AXE || item == Items.IRON_AXE || item == Items.GOLDEN_AXE || item == Items.STONE_AXE || item == Items.WOODEN_AXE;
   }

   private Entity getTarget() {
      HitResult result = mc.crosshairTarget;
      if (result instanceof EntityHitResult) {
         EntityHitResult hit = (EntityHitResult)result;
         return (Boolean)this.sticky.getValue() && this.lastTarget != null && hit.getEntity() != this.lastTarget ? null : hit.getEntity();
      } else {
         return null;
      }
   }

   private boolean isValidTarget(Entity entity, boolean critCheck) {
      if (entity != null && entity.isAlive()) {
         boolean typeValid = entity instanceof PlayerEntity || (Boolean)this.strayBypass.getValue() && entity instanceof HostileEntity || (Boolean)this.allEntities.getValue();
         if (!typeValid) {
            return false;
         } else {
            if (entity instanceof PlayerEntity) {
               PlayerEntity player = (PlayerEntity)entity;
               if ((Boolean)this.checkShield.getValue() && player.isBlocking()) {
                  return false;
               }
            }

            return critCheck ? this.canCrit() : true;
         }
      } else {
         return false;
      }
   }

   private boolean canCrit() {
      return !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0D && !mc.player.isUsingItem() && !mc.player.isSubmergedInWater();
   }

   private void handleMace() {
      Entity entity = this.getTarget();
      if (this.isValidTarget(entity, (Boolean)this.onlyCritSword.getValue())) {
         this.isMaceAttack = true;
         this.doAttack(entity);
         this.isMaceAttack = false;
      }
   }

   private void handleSword() {
      Entity entity = this.getTarget();
      if (this.isValidTarget(entity, (Boolean)this.onlyCritSword.getValue())) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastAttackTime >= (long)this.currentSwordDelay) {
            this.doAttack(entity);
            this.currentSwordDelay = this.getRandomDelay(this.swordDelayMin, this.swordDelayMax);
            this.lastAttackTime = currentTime;
         }
      }
   }

   private void handleAxe() {
      Entity entity = this.getTarget();
      if (this.isValidTarget(entity, (Boolean)this.onlyCritAxe.getValue())) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastAttackTime >= (long)this.currentAxeDelay) {
            this.doAttack(entity);
            this.currentAxeDelay = this.getRandomDelay(this.axeDelayMin, this.axeDelayMax);
            this.lastAttackTime = currentTime;
         }
      }
   }

   private void handleAllItems() {
      Entity entity = this.getTarget();
      if (this.isValidTarget(entity, (Boolean)this.onlyCritSword.getValue())) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastAttackTime >= (long)this.currentSwordDelay) {
            this.doAttack(entity);
            this.currentSwordDelay = this.getRandomDelay(this.swordDelayMin, this.swordDelayMax);
            this.lastAttackTime = currentTime;
         }
      }
   }

   private void doAttack(Entity entity) {
      mc.interactionManager.attackEntity(mc.player, entity);
      if ((Boolean)this.swing.getValue()) {
         mc.player.swingHand(Hand.MAIN_HAND);
      }

      if ((Boolean)this.clickSimulation.getValue()) {
         mc.options.attackKey.setPressed(true);
         mc.options.attackKey.setPressed(false);
      }

      this.lastTarget = entity;
   }

   private int getRandomDelay(NumberSetting min, NumberSetting max) {
      int minVal = (int)min.getValueFloat();
      int maxVal = (int)max.getValueFloat();
      return minVal >= maxVal ? minVal : minVal + (int)(Math.random() * (double)(maxVal - minVal));
   }

   @Subscribe
   public void onAttack(EventAttackEntity event) {
      if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) != 1) {
         event.cancel();
      }

   }

   public boolean isMaceAttackActive() {
      return this.isMaceAttack;
   }
}
