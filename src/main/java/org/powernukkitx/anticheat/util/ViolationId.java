package org.powernukkitx.anticheat.util;

import lombok.Getter;

/**
 * @author Kaooot
 */
@Getter
public enum ViolationId {

    INVALID_CREATIVE_DESTROY_ACTION(true),
    INAPPROPRIATE_BLOCK_INTERACTION_RANGE(true),
    INVALID_MINE_ABILITY_STATE,
    EXCEEDED_ACTOR_INTERACTION_RANGE,
    EXCEEDED_ACTOR_ATTACK_RANGE,
    EXCEEDED_MAX_CPS,
    INVALID_ATTACK_ACTOR(true),
    INVALID_PERMISSION_REQUEST(true),
    INVALID_CHAT(true),
    TOO_MUCH_CHAT_ATTEMPTS(true);

    ViolationId() {
        this.shouldKick = false;
    }

    ViolationId(boolean shouldKick) {
        this.shouldKick = shouldKick;
    }

    private final boolean shouldKick;
}