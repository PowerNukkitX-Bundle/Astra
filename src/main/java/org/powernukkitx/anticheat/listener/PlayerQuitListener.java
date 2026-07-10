package org.powernukkitx.anticheat.listener;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerQuitEvent;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@RequiredArgsConstructor
public class PlayerQuitListener implements Listener {

    private final AntiCheatPlugin plugin;

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        this.plugin.getPlayerRegistry().unregisterPlayer(event.getPlayer().getUniqueId());
    }
}