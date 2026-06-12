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

    // Reach
    private float maxActorInteractionRange = 4.0f;
    private float maxActorInteractionRangeCreative = 5.0f;
    private float actorInteractionRangeLimit = 3.0f;
    private float maxActorAttackRange = 6.0f;
    private float maxActorAttackRangeCreative = 7.0f;
    private float actorAttackRangeLimit = 5.0f;

    // Auto Clicker
    private int maxClicksPerSecond = 20;
    private int cpsLimit = 10;

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

        defaults.put("max_actor_interaction_range", this.maxActorInteractionRange);
        defaults.put("max_actor_interaction_range_creative", this.maxActorInteractionRangeCreative);
        defaults.put("actor_interaction_range_limit", this.actorInteractionRangeLimit);
        defaults.put("max_actor_attack_range", this.maxActorAttackRange);
        defaults.put("max_actor_attack_range_creative", this.maxActorAttackRangeCreative);
        defaults.put("actor_attack_range_limit", this.actorAttackRangeLimit);
        defaults.put("max_cps", this.maxClicksPerSecond);
        defaults.put("cps_limit", this.cpsLimit);

        config.setDefault(defaults);
        config.save();

        this.violationWarningInterval = config.getLong(
            "violation_warning_interval_ms", this.violationWarningInterval
        );
        this.broadcastViolationWarnings = config.getBoolean(
            "broadcast_violation_warnings", this.broadcastViolationWarnings
        );
        this.violationBroadcastReceivePermission = config.getString(
            "violation_broadcast_receive_permission", this.violationBroadcastReceivePermission
        );
        for (final ViolationId value : ViolationId.values()) {
            this.kickValueOverrides.put(
                value,
                config.getBoolean(value.name().toLowerCase() + "_should_kick", value.isShouldKick())
            );
        }
        this.maxBlockBreakDistance = (float) config.getDouble(
            "max_block_break_distance", this.maxBlockBreakDistance
        );
        this.maxBlockBreakDistanceCreative = (float) config.getDouble(
            "max_block_break_distance_creative", this.maxBlockBreakDistanceCreative
        );
        this.blockBreakProgressOffset = (float) config.getDouble(
            "block_break_progress_offset", this.blockBreakProgressOffset
        );
        this.maxActorInteractionRange = (float) config.getDouble(
            "max_actor_interaction_range", this.maxActorInteractionRange
        );
        this.maxActorInteractionRangeCreative = (float) config.getDouble(
            "max_actor_interaction_range_creative", this.maxActorInteractionRangeCreative
        );
        this.actorInteractionRangeLimit = (float) config.getDouble(
            "actor_interaction_range_limit", this.actorInteractionRangeLimit
        );
        this.maxActorAttackRange = (float) config.getDouble(
            "max_actor_attack_range", this.maxActorAttackRange
        );
        this.maxActorAttackRangeCreative = (float) config.getDouble(
            "max_actor_attack_range_creative", this.maxActorAttackRangeCreative
        );
        this.actorAttackRangeLimit = (float) config.getDouble(
            "actor_attack_range_limit", this.actorAttackRangeLimit
        );
        this.maxClicksPerSecond = config.getInt("max_cps", this.maxClicksPerSecond);
        this.cpsLimit = config.getInt("cps_limit", this.cpsLimit);
    }
}