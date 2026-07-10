package org.powernukkitx.anticheat.listener;

import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerHackDetectedEvent;

/**
 * @author Kaooot
 */
public class PlayerHackDetectedListener implements Listener {

    @EventHandler
    public void onPlayerHackDetected(PlayerHackDetectedEvent event) {
        event.setKick(false);
    }
}