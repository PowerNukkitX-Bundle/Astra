package org.powernukkitx.anticheat.util;

import lombok.experimental.UtilityClass;

/**
 * @author Kaooot
 */
@UtilityClass
public class MathUtil {

    public float round(float value) {
        return round(value, 2);
    }

    public float round(float value, int n) {
        final float f = (float) Math.pow(10, n);
        return Math.round(value * f) / f;
    }
}