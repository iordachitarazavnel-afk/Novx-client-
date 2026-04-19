package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.config.FriendManager;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.ui.notification.NotificationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

/**
 * FriendAdder — right-click a player while this module is enabled to add/remove them as friend.
 */
@ModuleInfo(
    name = "FriendAdder",
    category = Category.MISC,
    desc = "Click a player to add/remove as friend"
)
public class FriendAdder extends Function {

    private final BooleanSetting toggle = new BooleanSetting("Auto Toggle Off", true);
    private boolean wasAttacking = false;

    public FriendAdder() {
        addSettings(new Setting[]{ toggle });
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        boolean attacking = mc.options.attackKey.isPressed();

        if (attacking && !wasAttacking) {
            HitResult hit = mc.crosshairTarget;
            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerEntity player) {
                String name = player.getName().getString();
                FriendManager fm = FriendManager.getInstance();

                if (fm.isFriend(name)) {
                    fm.removeFriend(name);
                    NotificationManager.add("FriendAdder", "Removed " + name + " from friends", NotificationType.DISABLE);
                } else {
                    fm.addFriend(name);
                    NotificationManager.add("FriendAdder", "Added " + name + " as friend", NotificationType.SUCCESS);
                }

                if ((Boolean) toggle.getValue()) toggle();
            }
        }
        wasAttacking = attacking;
    }
}
