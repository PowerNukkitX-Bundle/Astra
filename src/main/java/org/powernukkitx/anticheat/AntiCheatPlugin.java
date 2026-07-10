package org.powernukkitx.anticheat;

import org.powernukkitx.plugin.PluginBase;
import lombok.Getter;
import org.powernukkitx.anticheat.config.MainConfig;
import org.powernukkitx.anticheat.listener.PlayerHackDetectedListener;
import org.powernukkitx.anticheat.listener.PlayerInvalidMoveListener;
import org.powernukkitx.anticheat.listener.PlayerJoinListener;
import org.powernukkitx.anticheat.listener.PlayerQuitListener;
import org.powernukkitx.anticheat.module.ModuleRegistry;
import org.powernukkitx.anticheat.player.AntiCheatPlayerRegistry;

/**
 * @author Kaooot
 */
@Getter
public class AntiCheatPlugin extends PluginBase {

    private AntiCheatPlayerRegistry playerRegistry;
    private MainConfig mainConfig;
    private ModuleRegistry moduleRegistry;

    @Override
    public void onEnable() {
        this.mainConfig = new MainConfig(this);
        this.mainConfig.init();
        this.playerRegistry = new AntiCheatPlayerRegistry(this);
        this.moduleRegistry = new ModuleRegistry(this);
        this.moduleRegistry.init();

        this.getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerInvalidMoveListener(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerHackDetectedListener(), this);
    }
}