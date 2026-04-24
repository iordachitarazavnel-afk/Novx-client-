package foure.dev.module.impl.misc;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.ui.notification.NotificationManager;
import foure.dev.ui.notification.NotificationType;

@ModuleInfo(
    name = "RainNotifier",
    category = Category.MISC,
    desc = "Notifies when it starts or stops raining"
)
public class RainNotifier extends Function {

    private final BooleanSetting notifyStart = new BooleanSetting("Notify Start", this, true);
    private final BooleanSetting notifyStop  = new BooleanSetting("Notify Stop",  this, true);
    private final BooleanSetting chatMessage = new BooleanSetting("Chat Message", this, false);

    private boolean wasRaining = false;
    private boolean firstTick  = true;

    public RainNotifier() {
        this.addSettings(new Setting[]{ notifyStart, notifyStop, chatMessage });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        firstTick  = true;
        wasRaining = false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        boolean isRaining = mc.world.isRaining();

        // First tick — just sync state, don't notify
        if (firstTick) {
            wasRaining = isRaining;
            firstTick  = false;
            return;
        }

        if (isRaining && !wasRaining) {
            // Started raining — like Krypton "Rain / Started raining"
            if ((Boolean) notifyStart.getValue()) {
                NotificationManager.add("Rain", "Started raining", NotificationType.INFO);
            }
            if ((Boolean) chatMessage.getValue() && mc.player != null) {
                mc.player.sendMessage(
                    net.minecraft.text.Text.literal("§b[RainNotifier] §fIt started raining!"), false);
            }
        } else if (!isRaining && wasRaining) {
            // Stopped raining
            if ((Boolean) notifyStop.getValue()) {
                NotificationManager.add("Rain", "Stopped raining", NotificationType.INFO);
            }
            if ((Boolean) chatMessage.getValue() && mc.player != null) {
                mc.player.sendMessage(
                    net.minecraft.text.Text.literal("§b[RainNotifier] §fIt stopped raining!"), false);
            }
        }

        wasRaining = isRaining;
    }
}
