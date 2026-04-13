package foure.dev.module.impl.movement;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.NumberSetting;
import foure.dev.module.setting.impl.StringSetting;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@ModuleInfo(
    name = "ElytraAutoFly",
    category = Category.MOVEMENT,
    desc = "Auto Elytra flight with airplane-style landing"
)
public class ElytraAutoFly extends Function {

    private final NumberSetting targetHeight = new NumberSetting("Target Height", this, 150.0, 64.0, 300.0, 1.0);
    private final NumberSetting targetX      = new NumberSetting("Target X",      this,   0.0, -30000.0, 30000.0, 1.0);
    private final NumberSetting targetZ      = new NumberSetting("Target Z",      this,   0.0, -30000.0, 30000.0, 1.0);
    private final StringSetting webhookUrl   = new StringSetting("Webhook URL",   this,   "https://discord.com/api/webhooks/...");

    private Stage   stage;
    private boolean elytraStarted;
    private long    lastFireworkTime;
    private long    lookUpUntil;
    private double  lastAltitude;
    private boolean lowDurabilityLanding;

    public ElytraAutoFly() {
        this.addSettings(new Setting[]{
            this.targetHeight, this.targetX, this.targetZ, this.webhookUrl
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        stage                = Stage.INIT;
        elytraStarted        = false;
        lookUpUntil          = 0L;
        lastFireworkTime     = 0L;
        lastAltitude         = 0.0;
        lowDurabilityLanding = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.options != null) mc.options.forwardKey.setPressed(false);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (fullNullCheck()) return;

        switch (stage) {
            case INIT -> {
                // try to equip elytra from hotbar
                for (int i = 0; i < 9; i++) {
                    if (mc.player.getInventory().getStack(i).isOf(Items.ELYTRA)) {
                        mc.player.getInventory().selectedSlot = i;
                        break;
                    }
                }
                stage = Stage.ASCENDING;
            }
            case ASCENDING -> {
                if (isElytraLowDurability()) { emergencyLand(); return; }
                if (mc.player.isOnGround()) { mc.player.jump(); return; }
                if (!elytraStarted) {
                    mc.player.networkHandler.sendPacket(
                        new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    elytraStarted    = true;
                    mc.player.setPitch(-45f);
                    useFirework();
                    lastFireworkTime = mc.world.getTime();
                    lookUpUntil      = mc.world.getTime() + 60;
                }
                if (mc.world.getTime() - lastFireworkTime > 60) {
                    mc.player.setPitch(-45f);
                    useFirework();
                    lastFireworkTime = mc.world.getTime();
                    lookUpUntil      = mc.world.getTime() + 60;
                }
                if (mc.player.getY() >= targetHeight.getValueFloat()) {
                    stage        = Stage.CRUISING;
                    lastAltitude = mc.player.getY();
                    msg("§aReached height. Cruising...");
                }
            }
            case CRUISING -> {
                if (isElytraLowDurability()) { emergencyLand(); return; }
                double dx       = targetX.getValueFloat() - mc.player.getX();
                double dz       = targetZ.getValueFloat() - mc.player.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (mc.world.getTime() < lookUpUntil) {
                    mc.player.setPitch(-45f);
                } else {
                    float desiredYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                    mc.player.setYaw(mc.player.getYaw() + (desiredYaw - mc.player.getYaw()) * 0.1f);
                    mc.player.setPitch(0f);
                }
                mc.options.forwardKey.setPressed(distance > 10);
                if (distance <= 10) stage = Stage.DESCENDING;
                if (mc.world.getTime() % 100 == 0 && mc.player.getY() < targetHeight.getValueFloat() + 5) {
                    mc.player.setPitch(-45f);
                    useFirework();
                    lastFireworkTime = mc.world.getTime();
                    lookUpUntil      = mc.world.getTime() + 60;
                    lastAltitude     = mc.player.getY();
                }
                if (lastAltitude - mc.player.getY() > 10) {
                    mc.player.setPitch(-45f);
                    useFirework();
                    lastFireworkTime = mc.world.getTime();
                    lookUpUntil      = mc.world.getTime() + 60;
                    lastAltitude     = mc.player.getY();
                }
            }
            case DESCENDING -> {
                double dx       = targetX.getValueFloat() - mc.player.getX();
                double dz       = targetZ.getValueFloat() - mc.player.getZ();
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 2) {
                    mc.player.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
                    mc.player.setPitch(-15f);
                    mc.options.forwardKey.setPressed(true);
                } else {
                    mc.options.forwardKey.setPressed(false);
                    mc.player.setPitch(10f);
                }
                if (mc.player.isOnGround()) {
                    mc.options.forwardKey.setPressed(false);
                    stage = Stage.LANDED;
                    String reason = lowDurabilityLanding
                        ? "Elytra below 15% durability. Landing complete."
                        : "AutoFly landing complete.";
                    sendWebhook(reason);
                    msg("§a" + reason);
                    toggle();
                }
            }
            case LANDED -> toggle();
        }
    }

    private void emergencyLand() {
        lowDurabilityLanding = true;
        stage = Stage.DESCENDING;
        sendWebhook("⚠️ Elytra durability below 15%! Emergency landing.");
        msg("§c⚠️ Elytra durability below 15%! Emergency landing.");
    }

    private void useFirework() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isOf(Items.FIREWORK_ROCKET)) {
                mc.player.getInventory().selectedSlot = i;
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                return;
            }
        }
    }

    private boolean isElytraLowDurability() {
        ItemStack chest = mc.player.getEquippedStack(EquipmentSlot.CHEST);
        if (!chest.isOf(Items.ELYTRA) || !chest.isDamageable()) return false;
        return (double)(chest.getMaxDamage() - chest.getDamage()) / chest.getMaxDamage() <= 0.15;
    }

    private void sendWebhook(String message) {
        String url = (String) webhookUrl.getValue();
        if (url == null || url.contains("...")) return;
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(("{\"content\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8));
                }
                conn.getInputStream().close();
            } catch (Exception ignored) {}
        }, "ElytraWebhook").start();
    }

    private void msg(String text) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[ElytraAutoFly] " + text), false);
    }

    private enum Stage { INIT, ASCENDING, CRUISING, DESCENDING, LANDED }
}
