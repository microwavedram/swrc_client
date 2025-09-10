package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2fStack;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.util.DeltaFormat;

public class TimerHud implements Hud {

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && SWRC.getRace().getTimerDuration() > 0;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        int scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        int scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        long time_current = System.currentTimeMillis();
        long time_remaining = Math.min(Math.max(SWRC.getRace().getTimerDuration() * 1000 - (time_current - SWRC.getRace().getTimerStart()), 0), SWRC.getRace().getTimerDuration() * 1000);

        if (SWRC.getRace().getTimerStart() == -1) {
            time_remaining = SWRC.getRace().getTimerDuration() * 1000;
        }

        //RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

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
                0xffffff,
                SWRCConfig.getInstance().leaderboard_shadow
        );

        matrixStack.popMatrix();
    }
}
