package uk.cloudmc.swrc.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import org.joml.Matrix3x2fStack;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.util.DeltaFormat;
import uk.cloudmc.swrc.util.NTPTimeSync;

public class TimerHud implements Hud {

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && SWRC.getRace().getTimerDuration() > 0;
    }

    @Override
    public void $render(DrawContext context, RenderTickCounter tickDelta) {
        int scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        int scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        long time_current = NTPTimeSync.getTrueTime();
        long time_remaining = Math.min(Math.max(SWRC.getRace().getTimerDuration() * 1000 - (time_current - SWRC.getRace().getTimerStart()), 0), SWRC.getRace().getTimerDuration() * 1000);

        if (SWRC.getRace().getTimerStart() == -1) {
            time_remaining = SWRC.getRace().getTimerDuration() * 1000;
        }

        Matrix3x2fStack matrixStack = context.getMatrices();
        matrixStack.pushMatrix();

        matrixStack.translate(scaledWidth - scaledHeight * .1f, scaledHeight * .1f);
        matrixStack.scale(2, 2);

        String label = DeltaFormat.formatMillis(time_remaining);

        context.drawText(
                SWRC.minecraftClient.textRenderer,
                label,
                -SWRC.minecraftClient.textRenderer.getWidth(label),
                0,
                0xFFFFFFFF,
                SWRCConfig.getInstance().leaderboard_shadow
        );

        matrixStack.popMatrix();
    }
}
