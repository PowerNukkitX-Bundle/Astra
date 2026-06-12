package org.powernukkitx.anticheat.listener;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerHackDetectedEvent;

/**
 * @author Kaooot
 */
public class PlayerHackDetectedListener implements Listener {

    @EventHandler
    public void onPlayerHackDetected(PlayerHackDetectedEvent event) {
        event.setKick(false);
    }
}