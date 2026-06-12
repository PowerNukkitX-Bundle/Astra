package org.powernukkitx.anticheat.module.block;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.server.PacketReceiveEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.cloudburstmc.protocol.bedrock.data.PlayerActionType;
import org.cloudburstmc.protocol.bedrock.data.PlayerBlockActionData;
import org.cloudburstmc.protocol.bedrock.packet.PlayerActionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.module.Module;
import org.powernukkitx.anticheat.module.ModuleType;
import org.powernukkitx.anticheat.player.AntiCheatPlayer;
import org.powernukkitx.anticheat.player.PlayerTimeStats;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class ServerAuthBlockBreakingModule extends Module {

    private static final List<PlayerActionType> BREAKING_ACTIONS = Arrays.asList(
        PlayerActionType.START_DESTROY_BLOCK,
        PlayerActionType.ABORT_DESTROY_BLOCK,
        PlayerActionType.PREDICT_DESTROY_BLOCK,
        PlayerActionType.CONTINUE_DESTROY_BLOCK
    );

    public ServerAuthBlockBreakingModule(AntiCheatPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onPlayerAuthInput(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof PlayerAuthInputPacket packet)) {
            return;
        }
        if (packet.getPlayerActions().isEmpty()) {
            return;
        }
        final List<PlayerBlockActionData> playerActions = packet.getPlayerActions();
        if (playerActions.stream().noneMatch(data -> BREAKING_ACTIONS.contains(data.getAction()))) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry().getPlayer(
            event.getPlayer().getUniqueId()
        );
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        player.processBlockBreak(packet);
    }

    @EventHandler
    public void onPlayerAction(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof PlayerActionPacket packet)) {
            return;
        }
        if (!packet.getAction().equals(PlayerActionType.CREATIVE_DESTROY_BLOCK)) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry().getPlayer(
            event.getPlayer().getUniqueId()
        );
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        if (!player.getServerPlayer().isCreative()) {
            player.updateTimeStatistic(
                PlayerTimeStats.LAST_INVALID_CREATIVE_DESTROY_ACTION, System.currentTimeMillis(),
                -1, this.plugin.getServer().getTick()
            );
            player.sendViolationWarning(
                ViolationId.INVALID_CREATIVE_DESTROY_ACTION,
                player.getName() + " failed creative block destroy"
            );
        }
    }

    @Override
    public String getName() {
        return "ServerAuthBlockBreaking";
    }

    @Override
    public ModuleType getType() {
        return ModuleType.BLOCK_BREAKING;
    }
}