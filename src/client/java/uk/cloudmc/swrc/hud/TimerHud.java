package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;

import java.text.DecimalFormat;

public class TimerHud implements Hud {
    private int scaledWidth;
    private int scaledHeight;
    private static final DecimalFormat decimalFormat = new DecimalFormat("00.000");

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && SWRC.getRace().getDuration() > 0;
    }

    public String msToTimeString(long ms) {
        double secconds = (double) ms / 1000;

        String prefix = "";

        if (secconds > 60) {
            int mins = (int) secconds / 60;

            prefix = String.format("%s:", mins);
        }

        return prefix + decimalFormat.format(secconds % 60);
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        long time_current = System.currentTimeMillis() ;
        long time_remaining = Math.min(Math.max(SWRC.getRace().getDuration() * 1000 - (time_current - SWRC.getRace().getStartTime()), 0), SWRC.getRace().getDuration() * 1000);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        context.drawText(
                SWRC.minecraftClient.textRenderer,
                msToTimeString(time_remaining),
                (scaledWidth/10)*9, (scaledHeight/10),
                0xffffff,
                SWRCConfig.getInstance().leaderboard_shadow);
    }
}
