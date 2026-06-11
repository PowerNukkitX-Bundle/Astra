package org.powernukkitx.anticheat.module;

import cn.nukkit.event.Listener;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;

/**
 * @author Kaooot
 */
@Getter
@RequiredArgsConstructor
public abstract class Module implements Listener {

    protected final AntiCheatPlugin plugin;

    public abstract String getName();

    public abstract ModuleType getType();
}