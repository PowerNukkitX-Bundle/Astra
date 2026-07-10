package org.powernukkitx.anticheat.config;

import org.powernukkitx.utils.Config;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Set;
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

    // Misc
    private long chatMessageInterval = 2000;
    private int maxChatAttempts = 5; // per second
    private int maxChatMessages = 10; // per minute
    private String allowedChars = "abcdefghijklmnopqrstuvwxyz" +
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
        "0123456789" +
        " .:;'(!?)+-*/=<>\"|^§$%&[]{}~#_\\";
    private int maxMessageLength = 100;
    private boolean enableChatProfanityFiltering = true;
    private final Set<String> wordlist = new ObjectOpenHashSet<>();

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

        defaults.put("chat_message_interval_ms", this.chatMessageInterval);
        defaults.put("max_chat_attempts", this.maxChatAttempts);
        defaults.put("max_chat_messages", this.maxChatMessages);
        defaults.put("allowed_chars", this.allowedChars);
        defaults.put("max_message_length", this.maxMessageLength);
        defaults.put("enable_chat_profanity_filtering", this.enableChatProfanityFiltering);

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
        this.chatMessageInterval = config.getLong("chat_message_interval_ms");
        this.maxChatAttempts = config.getInt("max_chat_attempts");
        this.maxChatMessages = config.getInt("max_chat_messages");
        this.allowedChars = config.getString("allowed_chars");
        this.maxMessageLength = config.getInt("max_message_length");
        this.enableChatProfanityFiltering = config.getBoolean("enable_chat_profanity_filtering");

        if (this.enableChatProfanityFiltering) {
            this.initWordlist();
        }
    }

    private void initWordlist() {
        final File file = new File(this.plugin.getDataFolder(), "profanity_filter.wlist");
        if (!file.exists()) {
            throw new IllegalArgumentException("Failed to find wordlist");
        }
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            final String data = new String(
                Base64.getDecoder().decode(new String(inputStream.readAllBytes()))
            );
            this.wordlist.addAll(Arrays.asList(data.split("\n")));
            this.plugin.getLogger().info(
                "Loaded " + this.wordlist.size() + " words from the profanity filter word list"
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}