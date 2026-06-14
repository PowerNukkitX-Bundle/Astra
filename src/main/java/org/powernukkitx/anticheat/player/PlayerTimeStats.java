package org.powernukkitx.anticheat.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kaooot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerTimeStats {

    public static final String LAST_MOVE_PREDICTION_CORRECTION = "last_move_prediction_correction";
    public static final String LAST_CLIENT_MOVE_PREDICTION_SYNC =
        "last_client_move_prediction_sync";
    public static final String LAST_PLAYER_AUTH_INPUT = "last_player_auth_input";
    public static final String LAST_INVALID_CREATIVE_DESTROY_ACTION =
        "last_invalid_creative_destroy_action";
    public static final String LAST_CHAT_MESSAGE_SENT = "last_chat_message_sent";

    private long timeInMS;
    private long clientFrame;
    private long serverTick;
}