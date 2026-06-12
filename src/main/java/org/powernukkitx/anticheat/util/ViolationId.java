package org.powernukkitx.anticheat.util;

import lombok.Getter;

/**
 * @author Kaooot
 */
@Getter
public enum ViolationId {

    INVALID_CREATIVE_DESTROY_ACTION(true),
    INAPPROPRIATE_BLOCK_INTERACTION_RANGE(true),
    INVALID_MINE_ABILITY_STATE(false),
    EXCEEDED_ACTOR_INTERACTION_RANGE(false),
    EXCEEDED_ACTOR_ATTACK_RANGE(false),
    EXCEEDED_MAX_CPS(false),
    INVALID_ATTACK_ACTOR(true);

    ViolationId() {
        this.shouldKick = false;
    }

    ViolationId(boolean shouldKick) {
        this.shouldKick = shouldKick;
    }

    private final boolean shouldKick;
}