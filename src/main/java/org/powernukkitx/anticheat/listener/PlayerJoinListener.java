package org.powernukkitx.anticheat.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@RequiredArgsConstructor
public class PlayerJoinListener implements Listener {

    private final AntiCheatPlugin plugin;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getPlayerRegistry().registerPlayer(event.getPlayer());
    }
}