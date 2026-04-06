package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.FourEClient;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

@ModuleInfo(
   name = "AutoDoubleHand",
   category = Category.COMBAT,
   desc = "Automatically switches to totems on pop or low health"
)
public class AutoDoubleHand extends Function {
   private final BooleanSetting onPop = new BooleanSetting("On Pop", true);
   private final BooleanSetting onHealth = new BooleanSetting("On Health", true);
   private final NumberSetting healthThreshold = new NumberSetting("Health", this, 10.0D, 0.0D, 20.0D, 1.0D);
   private int previousSlot = -1;
   private float lastHealth = 0.0F;
   private boolean hasTriggered = false;

   public AutoDoubleHand() {
      this.addSettings(new Setting[]{this.onPop, this.onHealth, this.healthThreshold});
   }

   public void onEnable() {
      super.onEnable();
      this.previousSlot = -1;
      this.lastHealth = 0.0F;
      this.hasTriggered = false;
   }

   public void onDisable() {
      super.onDisable();
      this.previousSlot = -1;
      this.lastHealth = 0.0F;
      this.hasTriggered = false;
   }

   @Subscribe
   public void onUpdate(EventUpdate event) {
      if (!fullNullCheck()) {
         if ((Boolean)this.onHealth.getValue()) {
            float playerHealth = mc.player.getHealth();
            if (playerHealth <= this.healthThreshold.getValueFloat() && !this.hasTriggered) {
               this.switchToTotem();
               this.hasTriggered = true;
            } else if (playerHealth > this.healthThreshold.getValueFloat()) {
               this.hasTriggered = false;
            }

            this.lastHealth = playerHealth;
         }

      }
   }

   @Subscribe
   public void onPacket(PacketEvent event) {
      Packet var3 = event.getPacket();
      if (var3 instanceof EntityStatusS2CPacket) {
         EntityStatusS2CPacket packet = (EntityStatusS2CPacket)var3;
         if (packet.getStatus() != 35 || fullNullCheck()) {
            return;
         }

         Entity entity = packet.getEntity(mc.world);
         if (entity == null || !entity.equals(mc.player) || !(Boolean)this.onPop.getValue()) {
            return;
         }

         this.switchToTotem();
      }

   }

   private void switchToTotem() {
      if (mc.player != null) {
         boolean useSecondTotem = false;
         Iterator var2 = FourEClient.getInstance().getFunctionManager().getModules().iterator();

         while(var2.hasNext()) {
            Function module = (Function)var2.next();
            if (module.getName().equals("AutoTotem") && module.isToggled()) {
               useSecondTotem = true;
               break;
            }
         }

         int totemSlot = useSecondTotem ? this.findSecondTotemInHotbar() : this.findTotemInHotbar();
         if (totemSlot != -1) {
            if (this.previousSlot == -1) {
               this.previousSlot = mc.player.getInventory().selectedSlot;
            }

            mc.player.getInventory().selectedSlot = totemSlot;
         }
      }
   }

   private int findSecondTotemInHotbar() {
      int count = 0;

      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            ++count;
            if (count == 2) {
               return i;
            }
         }
      }

      return -1;
   }

   private int findTotemInHotbar() {
      for(int i = 0; i < 9; ++i) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            return i;
         }
      }

      return -1;
   }
}
