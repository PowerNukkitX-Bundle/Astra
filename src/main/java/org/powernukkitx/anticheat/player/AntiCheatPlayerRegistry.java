package org.powernukkitx.anticheat.player;

import org.powernukkitx.Player;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@RequiredArgsConstructor
public class AntiCheatPlayerRegistry {

    private final AntiCheatPlugin plugin;
    private final Map<UUID, AntiCheatPlayer> map = new Object2ObjectOpenHashMap<>();

    public void registerPlayer(Player player) {
        this.map.put(player.getUniqueId(), new AntiCheatPlayer(player, this.plugin));
    }

    public Optional<AntiCheatPlayer> getPlayer(UUID uuid) {
        return Optional.ofNullable(this.map.getOrDefault(uuid, null));
    }

    public void unregisterPlayer(UUID uuid) {
        this.map.remove(uuid);
    }
}