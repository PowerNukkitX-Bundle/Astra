package org.powernukkitx.anticheat.listener;

import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerInvalidMoveEvent;

/**
 * @author Kaooot
 */
public class PlayerInvalidMoveListener implements Listener {

    public void onPlayerInvalidMove(PlayerInvalidMoveEvent event){
        event.setCancelled();
    }
}