package org.powernukkitx.anticheat.listener;

import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInvalidMoveEvent;

/**
 * @author Kaooot
 */
public class PlayerInvalidMoveListener implements Listener {

    public void onPlayerInvalidMove(PlayerInvalidMoveEvent event){
        event.setCancelled();
    }
}