package uk.cloudmc.swrc.util;

import net.minecraft.text.Text;

import static net.minecraft.util.math.MathHelper.clamp;

public class DeltaFormat {
    public static String formatMillis(long millis) {
        long absMillis = Math.abs(millis);
        long minutes = absMillis / 60000;
        long seconds = (absMillis % 60000) / 1000;
        long milliseconds = absMillis % 1000;

        if (minutes > 0) {
            return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
        } else {
            return String.format("%02d.%03d", seconds, milliseconds);
        }
    }

    public static String formatDelta(long millis) {
        String prefix = "";

        if (millis > 0) {
            prefix = "+";
        } else if (millis < 0) {
            prefix = "-";
        }

        return prefix + formatMillis(millis);
    }

    public static Text formatLapDelta(long millis, long interval) {
        double normalized = Math.clamp((float) millis / interval, -2, 2);
        double lerp = 1f/(1f + Math.exp(-2f * normalized));

        return Text.literal(formatDelta(millis)).withColor(ColorUtil.lerpColor(
                0xFF6BF490,// red
                0xFFf56a6a,  // green
                (float) clamp(lerp, 0, 1)
        ));
    }
}
