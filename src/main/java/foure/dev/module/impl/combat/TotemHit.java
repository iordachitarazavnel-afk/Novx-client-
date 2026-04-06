package foure.dev.module.impl.combat;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.util.Hand;

@ModuleInfo(
    name = "TotemHit",
    category = Category.COMBAT,
    desc = "More knockback when hitting players with totems"
)
public class TotemHit extends Function {

    public TotemHit() {
        this.addSettings(new Setting[]{});
    }

    public void onEnable() {
        super.onEnable();
    }

    public void onDisable() {
        super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!fullNullCheck() && mc.currentScreen == null) {
            if (mc.player.getMainHandStack().getItem() == Items.TOTEM_OF_UNDYING) {
                if (mc.options.attackKey.isPressed()) {
                    Entity target = mc.targetedEntity;
                    if (target instanceof PlayerEntity) {
                        for (int i = 0; i < 9; i++) {
                            ItemStack stack = mc.player.getInventory().getStack(i);
                            if (stack.getItem() instanceof SwordItem) {
                                int prevSlot = mc.player.getInventory().selectedSlot;
                                mc.player.getInventory().selectedSlot = i;
                                mc.interactionManager.attackEntity(mc.player, target);
                                mc.player.swingHand(Hand.MAIN_HAND);
                                mc.player.getInventory().selectedSlot = prevSlot;
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
}
