package org.powernukkitx.anticheat.module.misc;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.server.PacketReceiveEvent;
import java.util.Optional;
import org.cloudburstmc.protocol.bedrock.data.payload.text.AuthorAndMessage;
import org.cloudburstmc.protocol.bedrock.packet.TextPacket;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.module.Module;
import org.powernukkitx.anticheat.module.ModuleType;
import org.powernukkitx.anticheat.player.AntiCheatPlayer;
import org.powernukkitx.anticheat.player.PlayerTimeStats;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
public class ChatModule extends Module {

    private final long interval;
    private final int maxChatAttempts;
    private final int maxChatMessages;

    public ChatModule(AntiCheatPlugin plugin) {
        super(plugin);
        this.interval = plugin.getMainConfig().getChatMessageInterval();
        this.maxChatAttempts = plugin.getMainConfig().getMaxChatAttempts();
        this.maxChatMessages = plugin.getMainConfig().getMaxChatMessages();
    }

    @EventHandler
    public void onText(PacketReceiveEvent event) {
        if (!(event.getPacket() instanceof TextPacket packet)) {
            return;
        }
        final Optional<AntiCheatPlayer> optional = this.plugin.getPlayerRegistry()
            .getPlayer(event.getPlayer().getUniqueId());
        if (optional.isEmpty()) {
            return;
        }
        final AntiCheatPlayer player = optional.get();
        player.increaseChatAttempts();
        if (player.getChatAttempts() >= this.maxChatAttempts) {
            player.sendViolationWarning(
                ViolationId.TOO_MUCH_CHAT_ATTEMPTS,
                player.getName() + " had too much chat attempts");
            event.setCancelled();
            return;
        }
        if (packet.isLocalize() ||
            !packet.getSendersXUID().equals(event.getPlayer().getXUID()) ||
            !(packet.getBody() instanceof AuthorAndMessage body)) {
            this.sendViolationWarning(player);
            event.setCancelled();
            return;
        }
        if (!body.getPlayerName().equalsIgnoreCase(player.getName())) {
            this.sendViolationWarning(player);
            event.setCancelled();
            return;
        }
        if (System.currentTimeMillis() -
            player.getTimeStatistic(PlayerTimeStats.LAST_CHAT_MESSAGE_SENT).getTimeInMS() <
            this.interval || player.getChatMessageCount() + 1 > this.maxChatMessages) {
            player.getServerPlayer().sendMessage("§cYou're sending messages too fast");
            event.setCancelled();
            return;
        }
        if (body.getMessage().length() > this.plugin.getMainConfig().getMaxMessageLength()) {
            player.getServerPlayer().sendMessage("§cYour message contains too many characters");
            event.setCancelled();
            return;
        }
        String message = body.getMessage().replaceAll("\n", "");
        if (message.isEmpty()) {
            this.sendViolationWarning(player);
            event.setCancelled();
            return;
        }
        for (final char c : message.toCharArray()) {
            if (!this.plugin.getMainConfig().getAllowedChars().contains(String.valueOf(c))) {
                player.getServerPlayer().sendMessage(
                    "§cYour message contains forbidden characters"
                );
                event.setCancelled();
                return;
            }
        }
        if (this.plugin.getMainConfig().isEnableChatProfanityFiltering()) {
            message = this.filterMessage(message);
        }
        body.setMessage(message);
        player.increaseChatMessageCount();
        player.updateTimeStatistic(
            PlayerTimeStats.LAST_CHAT_MESSAGE_SENT,
            System.currentTimeMillis(),
            -1,
            this.plugin.getServer().getTick()
        );
    }

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public ModuleType getType() {
        return ModuleType.MISC;
    }

    private void sendViolationWarning(AntiCheatPlayer player) {
        player.sendViolationWarning(
            ViolationId.INVALID_CHAT,
            player.getName() + " sent an invalid TextPacket"
        );
    }

    private String filterMessage(String message) {
        return message; // TODO
    }
}