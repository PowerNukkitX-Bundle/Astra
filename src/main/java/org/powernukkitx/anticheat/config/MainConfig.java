package org.powernukkitx.anticheat.config;

import cn.nukkit.utils.Config;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.io.File;
import java.util.LinkedHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.powernukkitx.anticheat.AntiCheatPlugin;
import org.powernukkitx.anticheat.util.ViolationId;

/**
 * @author Kaooot
 */
@Getter
@RequiredArgsConstructor
public class MainConfig {

    private final AntiCheatPlugin plugin;

    // Violations
    private long violationWarningInterval = 10000L;
    private boolean broadcastViolationWarnings = true;
    private String violationBroadcastReceivePermission = "astra.violations";
    private final Object2BooleanMap<ViolationId> kickValueOverrides =
        new Object2BooleanOpenHashMap<>();

    // Server Auth Block Breaking
    private float maxBlockBreakDistance = 5.0f;
    private float maxBlockBreakDistanceCreative = 6.0f;
    private float blockBreakProgressOffset = 0.35f;

    private float maxActorInteractionRange = 3.0f;
    private float maxActorInteractionRangeCreative = 5.0f;

    public void init() {
        final Config config = new Config(
            new File(this.plugin.getDataFolder(), "config.json"), Config.JSON
        );

        final LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("violation_warning_interval_ms", this.violationWarningInterval);
        defaults.put("broadcast_violation_warnings", this.broadcastViolationWarnings);
        defaults.put(
            "violation_broadcast_receive_permission",
            this.violationBroadcastReceivePermission
        );
        for (final ViolationId violationId : ViolationId.values()) {
            this.kickValueOverrides.put(violationId, violationId.isShouldKick());
            defaults.put(
                violationId.name().toLowerCase() + "_should_kick",
                violationId.isShouldKick()
            );
        }
        defaults.put("max_block_break_distance", this.maxBlockBreakDistance);
        defaults.put("max_block_break_distance_creative", this.maxBlockBreakDistanceCreative);
        defaults.put("block_break_progress_offset", this.blockBreakProgressOffset);

        config.setDefault(defaults);
        config.save();

        this.violationWarningInterval = config.getInt("violation_warning_interval_ms");
        this.broadcastViolationWarnings = config.getBoolean("broadcast_violation_warnings");
        this.violationBroadcastReceivePermission = config.getString(
            "violation_broadcast_receive_permission"
        );
        for (final ViolationId value : ViolationId.values()) {
            this.kickValueOverrides.put(
                value,
                config.getBoolean(value.name().toLowerCase() + "_should_kick")
            );
        }
        this.maxBlockBreakDistance = (float) config.getDouble("max_block_break_distance");
        this.maxBlockBreakDistanceCreative = (float) config.getDouble(
            "max_block_break_distance_creative"
        );
        this.blockBreakProgressOffset = (float) config.getDouble("block_break_progress_offset");
    }
}