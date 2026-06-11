package org.powernukkitx.anticheat.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;
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