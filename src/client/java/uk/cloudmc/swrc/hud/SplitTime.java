package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;

import java.text.DecimalFormat;

public class SplitTime implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");

    private int scaledWidth;
    private int scaledHeight;

    private static final DecimalFormat decimalFormat = new DecimalFormat("00.000");

    public SplitTime() {}

    @Override
    public boolean shouldRender() {
        if (SWRC.minecraftClient.player == null) return false;
        if (SWRC.getRace() == null) return false;
        if (SWRC.getRace().getRaceState() == Race.RaceState.NONE) return false;

        if (SWRC.getRace().laps.getOrDefault(SWRC.minecraftClient.player.getGameProfile().getName(), 0) > SWRC.getRace().getTotalLaps()) return false;

        return SWRC.getRace().isRacing(SWRC.minecraftClient.player.getName().getString());
    }

    @Override
    public void render(DrawContext graphics, float tickDelta) {
        Race race = SWRC.getRace();

        if (race.raceLeaderboardPositions.isEmpty()) return;

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int x = this.scaledWidth / 2;
        int y = (int) (this.scaledHeight * 0.6);

        int selfPlace = race.getSelfBoardPosition();
        long current_lap = System.currentTimeMillis() - race.getLapBeginTime(SWRC.minecraftClient.player.getName().getString());
        long delta_to_infront = 0;

        if (selfPlace > 0) {
            long self_delta = race.raceLeaderboardPositions.get(selfPlace).time_delta;
            long infront_delta = race.raceLeaderboardPositions.get(selfPlace - 1).time_delta;

            delta_to_infront = self_delta - infront_delta;
        }

        String time_text = msToTimeString(current_lap) + " P" + (selfPlace + 1);
        String split_text = (delta_to_infront >= 0 ? "+" : "") + msToTimeString(delta_to_infront);

        int combined_length = widthOfText(time_text) + widthOfText(split_text) + 4;

        int tx = 30;
        int ty = delta_to_infront >= 0 ? 0 : 10;

        x -= combined_length / 2;

        for (int ax = 0; ax < widthOfText(time_text) + 2; ax++) {
            graphics.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, x + ax, y, tx, ty, 1, 10, 256, 256);
            for (int ay = 0; ay < 10; ay++) {
                graphics.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, x + ax, y + ay, tx, ty, 1, 1, 256, 256);
            }
        }


        for (int bx = 0; bx < widthOfText(split_text) + 2; bx++) {
            graphics.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, x + bx + widthOfText(time_text) + 2, y, tx + 1, ty, 1, 10, 256, 256);
        }

        renderText(graphics, time_text, x + 1, y + 1, 0xFFFFFF);
        renderText(graphics, split_text, x + widthOfText(time_text) + 3, y + 1, 0xFFFFFF);
        renderText(graphics, split_text, x + widthOfText(time_text) + 3, y + 1, 0xFFFFFF);
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

    public static void renderText(DrawContext graphics, String text, int x, int y, int color) {
        graphics.drawText(SWRC.minecraftClient.textRenderer, text, x, y, color, SWRCConfig.getInstance().leaderboard_shadow);
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }
}
