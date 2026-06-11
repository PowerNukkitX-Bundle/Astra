package org.powernukkitx.anticheat.config;

import cn.nukkit.utils.Config;
import java.io.File;
import java.util.LinkedHashMap;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@RequiredArgsConstructor
public class MainConfig {

    private final AntiCheatPlugin plugin;

    private Config config;

    public void init() {
        this.config = new Config(new File(this.plugin.getDataFolder(), "config.json"), Config.JSON);

        final LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();

        this.config.setDefault(defaults);
        this.config.save();
    }
}