package foure.dev.module.impl.donut;

import com.google.common.eventbus.Subscribe;
import foure.dev.event.impl.game.EventUpdate;
import foure.dev.event.impl.game.PacketEvent;
import foure.dev.module.api.Category;
import foure.dev.module.api.Function;
import foure.dev.module.api.ModuleInfo;
import foure.dev.module.setting.api.Setting;
import foure.dev.module.setting.impl.BooleanSetting;
import foure.dev.module.setting.impl.NumberSetting;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.OnGroundOnly;

import java.util.ArrayDeque;
import java.util.Deque;

@ModuleInfo(
    name = "AntiCheatBypass",
    category = Category.DONUT,
    desc = "Tweaks la nivel de packet pentru a reduce false flag-uri pe Donut SMP."
)
public class AntiCheatBypass extends Function {

    // Movement
    private final BooleanSetting noFallBypass    = new BooleanSetting("No Fall Bypass",        false);
    private final BooleanSetting smoothSprint    = new BooleanSetting("Smooth Sprint",          true);
    private final NumberSetting  speedCap        = new NumberSetting("Speed Cap", this, 0.0, 0.0, 1.0, 0.01);

    // Packets
    private final BooleanSetting packetDelay     = new BooleanSetting("Packet Delay",           false);
    private final NumberSetting  delayTicks      = new NumberSetting("Delay Ticks",  this, 2, 1, 10, 1);
    private final BooleanSetting cancelRedundant = new BooleanSetting("Cancel Redundant Moves", true);

    // Spoofing
    private final BooleanSetting spoofOnGround   = new BooleanSetting("Spoof On Ground",        false);
    private final BooleanSetting rotationSmooth  = new BooleanSetting("Rotation Smoothing",     true);

    private final Deque<PlayerMoveC2SPacket> packetQueue = new ArrayDeque<>();

    private double lastX, lastY, lastZ;
    private float  lastYaw, lastPitch;

    public AntiCheatBypass() {
        this.addSettings(new Setting[]{
            noFallBypass, smoothSprint, speedCap,
            packetDelay, delayTicks, cancelRedundant,
            spoofOnGround, rotationSmooth
        });
    }

    @Override
    public void onEnable() {
        super.onEnable();
        packetQueue.clear();
        if (mc.player != null) {
            lastX     = mc.player.getX();
            lastY     = mc.player.getY();
            lastZ     = mc.player.getZ();
            lastYaw   = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        // Flush orice pachete rămase în coadă la dezactivare
        if (mc.getNetworkHandler() != null) {
            for (PlayerMoveC2SPacket pkt : packetQueue)
                mc.getNetworkHandler().sendPacket(pkt);
        }
        packetQueue.clear();
    }

    @Subscribe
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null) return;
        if (!(event.getPacket() instanceof PlayerMoveC2SPacket pkt)) return;

        // Anulează pachete redundante (aceeași poziție/unghi ca tick-ul anterior)
        if ((Boolean) cancelRedundant.getValue()) {
            double px    = (pkt instanceof Full f) ? f.getX(lastX)     : lastX;
            double py    = (pkt instanceof Full f) ? f.getY(lastY)     : lastY;
            double pz    = (pkt instanceof Full f) ? f.getZ(lastZ)     : lastZ;
            float  yaw   = pkt.getYaw(lastYaw);
            float  pitch = pkt.getPitch(lastPitch);
            if (px == lastX && py == lastY && pz == lastZ && yaw == lastYaw && pitch == lastPitch) {
                event.cancel();
                return;
            }
        }

        // Speed cap — limitează distanța raportată per tick
        double cap = speedCap.getValueFloat();
        if (cap > 0 && pkt instanceof Full f) {
            double dx   = f.getX(lastX) - lastX;
            double dz   = f.getZ(lastZ) - lastZ;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > cap) {
                double scale = cap / dist;
                event.cancel();
                mc.getNetworkHandler().sendPacket(new Full(
                    lastX + dx * scale,
                    f.getY(lastY),
                    lastZ + dz * scale,
                    f.getYaw(lastYaw),
                    f.getPitch(lastPitch),
                    f.isOnGround(),
                    mc.player.horizontalCollision
                ));
                return;
            }
        }

        // Packet delay — bufferează mișcările
        if ((Boolean) packetDelay.getValue()) {
            event.cancel();
            packetQueue.add(pkt);
            return;
        }

        // Actualizează ultima poziție cunoscută
        if (pkt instanceof Full f) {
            lastX     = f.getX(lastX);
            lastY     = f.getY(lastY);
            lastZ     = f.getZ(lastZ);
            lastYaw   = f.getYaw(lastYaw);
            lastPitch = f.getPitch(lastPitch);
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Eliberează treptat pachetele din coadă
        if ((Boolean) packetDelay.getValue() && !packetQueue.isEmpty()) {
            int toRelease = Math.max(1, packetQueue.size() - delayTicks.getValueInt());
            for (int i = 0; i < toRelease && !packetQueue.isEmpty(); i++)
                mc.getNetworkHandler().sendPacket(packetQueue.poll());
        }

        // Spoof on-ground în fiecare tick
        if ((Boolean) spoofOnGround.getValue())
            mc.getNetworkHandler().sendPacket(new OnGroundOnly(true, mc.player.horizontalCollision));
    }
}
