package org.powernukkitx.anticheat.module.misc;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.server.PacketReceiveEvent;
import java.util.Optional;
import org.cloudburstmc.protocol.bedrock.packet.RequestPermissionsPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.module.Module;
import org.powernukkitx.anticheat.module.ModuleType;
import org.powernukkitx.anticheat.player.AntiCheatPlayer;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class RequestPermissionModule extends Module {

    public RequestPermissionModule(AntiCheatPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onRequestPermissions(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof RequestPermissionsPacket packet)) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry()
            .getPlayer(event.getPlayer().getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        if (packet.getTargetPlayerId() != player.getServerPlayer().getId()) {
            this.sendViolationWarning(player);
            return;
        }
        if (!player.getServerPlayer().isOp()) {
            this.sendViolationWarning(player);
        }
    }

    @Override
    public String getName() {
        return "RequestPermission";
    }

    @Override
    public ModuleType getType() {
        return ModuleType.MISC;
    }

    private void sendViolationWarning(AntiCheatPlayer player) {
        player.sendViolationWarning(
            ViolationId.INVALID_PERMISSION_REQUEST,
            player.getName() + " sent an invalid permission request"
        );
    }
}