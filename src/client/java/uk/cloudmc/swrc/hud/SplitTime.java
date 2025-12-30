package uk.cloudmc.swrc.hud;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.tt.TimeTrial;
import uk.cloudmc.swrc.util.DeltaFormat;
import uk.cloudmc.swrc.util.NTPTimeSync;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public class SplitTime implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");

    private int scaledWidth;
    private int scaledHeight;

    private static int background_color = 0x66212121;
    private static int bad_color = 0xFFFF0000;
    private static int neutral_color = 0xFF0000FF;
    private static int good_color = 0xFF00FF00;

    public static void initColors() {
        Optional<Resource> resource = SWRC.minecraftClient.getResourceManager().getResource(WIDGETS_TEXTURE);
        if (resource.isPresent()) {
            try (InputStream stream = resource.get().getInputStream()) {
                NativeImage image = NativeImage.read(stream);

                background_color = image.getColorArgb(26, 0);
                bad_color = image.getColorArgb(25, 0);
                neutral_color = image.getColorArgb(25, 1);
                good_color = image.getColorArgb(25, 2);
            } catch (IOException ignored) {}
        }
    }

    public SplitTime() {}

    @Override
    public boolean shouldRender() {
        if (SWRC.minecraftClient.player == null) return false;

        if (SWRC.getTimeTrial() != null) {
            return true;
        }

        if (SWRC.getRace() == null) return false;
        if (SWRC.getRace().getRaceState() == Race.RaceState.NONE) return false;
        if (SWRC.getRace().raceLeaderboardPositions.isEmpty()) return false;

        return SWRC.getRace().isRacing(SWRC.minecraftClient.player.getName().getString());
    }

    // ultra spaghetti
    public void $renderTT(DrawContext context, RenderTickCounter tickDelta) {
        TimeTrial timeTrial = SWRC.getTimeTrial();

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        int x = (int) (this.scaledWidth * SWRCConfig.getInstance().split_hud_x);
        int y = (int) (this.scaledHeight * SWRCConfig.getInstance().split_hud_y);

        long current_lap = NTPTimeSync.getTrueTime() - timeTrial.getLapBeginTime();

        if (timeTrial.getLapBeginTime() == -1) {
            current_lap = 0;
        }

        String time_text = DeltaFormat.formatMillis(current_lap);
        String split_text = DeltaFormat.formatDelta(timeTrial.getLastDelta());

        if (timeTrial.getLastDelta() == -1) {
            split_text = "  -  ";
        }

        boolean bad_split = true;
        boolean neutral_split = false;

        if (timeTrial.isActive()) {
            bad_split = timeTrial.getLastDelta() > 0;
        } else {
            neutral_split = true;
            split_text = "   -   ";
        }

        int combined_length = widthOfText(time_text) + widthOfText(split_text) + 4;

        x -= combined_length / 2;

        int color = good_color;

        if (neutral_split) {
            color = neutral_color;
        } else if (bad_split) {
            color = bad_color;
        }

        context.fill(x, y, x + widthOfText(time_text) + 2, y + 10, background_color);
        context.fill(x + widthOfText(time_text) + 2, y, x + widthOfText(time_text) + widthOfText(split_text) + 5, y + 10, color);

        renderText(context, time_text, x + 1, y + 1, 0xFFFFFFFF);
        renderText(context, split_text, x + widthOfText(time_text) + 3, y + 1, 0xFFFFFFFF);

    }

    @Override
    public void $render(DrawContext context, RenderTickCounter tickDelta) {
        Race race = SWRC.getRace();
        TimeTrial timeTrial = SWRC.getTimeTrial();

        if (timeTrial != null) {
            $renderTT(context, tickDelta);
            return;
        }

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        int x = (int) (this.scaledWidth * SWRCConfig.getInstance().split_hud_x);
        int y = (int) (this.scaledHeight * SWRCConfig.getInstance().split_hud_y);

        int selfPlace = race.getSelfBoardPosition();
        long current_lap = NTPTimeSync.getTrueTime() - race.getLapBeginTime(SWRC.minecraftClient.player.getName().getString());
        long delta_to_infront = 0;

        if (race.laps.getOrDefault(SWRC.minecraftClient.player.getName().getString(), 0) > race.getTotalLaps()) return;

        if (selfPlace > 0) {
            long self_delta = race.raceLeaderboardPositions.get(selfPlace).time_delta;
            long infront_delta = race.raceLeaderboardPositions.get(selfPlace - 1).time_delta;

            delta_to_infront = self_delta - infront_delta;
        }

        String position_text = String.format("P%s ", selfPlace + 1);

        String time_text = position_text + DeltaFormat.formatMillis(current_lap);
        String split_text = DeltaFormat.formatDelta(delta_to_infront);

        boolean bad_split = true;
        boolean neutral_split = false;

        if (race.getRaceState() == Race.RaceState.QUALI) {
            long lap_delta = race.raceLeaderboardPositions.get(selfPlace).lap_delta;
            split_text = DeltaFormat.formatDelta(lap_delta);

            if (lap_delta < 0) {
                bad_split = false;
            }
        } else if (selfPlace == 0) {
            split_text = "INTERVAL";
            neutral_split = true;
        }

        int combined_length = widthOfText(time_text) + widthOfText(split_text) + 4;

        x -= combined_length / 2;

        int color = good_color;

        if (neutral_split) {
            color = neutral_color;
        } else if (bad_split) {
            color = bad_color;
        }

        context.fill(x, y, x + widthOfText(time_text) + 2, y + 10, background_color);
        context.fill(x + widthOfText(time_text) + 2, y, x + widthOfText(time_text) + 2 + widthOfText(split_text) + 2, y + 10, color);

        renderText(context, time_text, x + 1, y + 1, 0xFFFFFFFF);
        renderText(context, split_text, x + widthOfText(time_text) + 3, y + 1, 0xFFFFFFFF);

    }

    public static void renderText(DrawContext graphics, String text, int x, int y, int color) {
        graphics.drawText(SWRC.minecraftClient.textRenderer, text, x, y, color, SWRCConfig.getInstance().leaderboard_shadow);
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }
}
