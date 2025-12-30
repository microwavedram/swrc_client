package uk.cloudmc.swrc.hud;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.net.packets.S2CUpdatePacket;
import uk.cloudmc.swrc.util.ColorUtil;
import uk.cloudmc.swrc.util.DeltaFormat;

import java.util.HashMap;

import static net.minecraft.util.math.MathHelper.clamp;

public class RaceLeaderboard implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");
    private static final HashMap<String, Double> rowHeight = new HashMap<>();

    // Cache frequently used colors
    private static final int COLOR_GOLD = 0xFFFCBA03;
    private static final int COLOR_SILVER = 0xFFB2B1BD;
    private static final int COLOR_BRONZE = 0xFF805B2B;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_FLAP = 0xFF9803FC;
    private static final int COLOR_GRAY = 0xFFAAAAAA;
    private static final int COLOR_ERROR = 0xFFDDAAAA;
    private static final int COLOR_GREEN = 0xFF6af573;
    private static final int COLOR_CYAN = 0xFF00FFFF;
    private static final int COLOR_FLAP_YELLOW = 0xFFEBCC34;
    private static final int COLOR_YELLOW = 0xFFFFFF00;
    private static final int COLOR_YELLOW_DELTA = 0xFFf5ee6a;
    private static final int COLOR_RED_DELTA = 0xFFf56a6a;

    private static final String STR_IN_PIT = "IN PIT";
    private static final String STR_FINISHED = "FINISHED";
    private static final String STR_INTERVAL = "INTERVAL";
    private static final String STR_ERR = "ERR";
    private static final String STR_DASH = "-";

    private static int cachedInPitWidth = -1;
    private static int cachedDeltaPlaceholderWidth = -1;

    private static final double LERP_SPEED = 0.05;

    public RaceLeaderboard() {}

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        Race race = SWRC.getRace();
        return race != null && race.getRaceState() != Race.RaceState.QUALI;
    }

    @Override
    public void $render(DrawContext context, RenderTickCounter tickDelta) {
        Race race = SWRC.getRace();

        Matrix3x2fStack stack = context.getMatrices();

        stack.pushMatrix();
        stack.scale((float) SWRCConfig.getInstance().leaderboard_scale);

        final int baseX = (int) (context.getScaledWindowWidth() * SWRCConfig.getInstance().leaderboard_x) + 10;
        final int baseY = (int) (context.getScaledWindowHeight() * SWRCConfig.getInstance().leaderboard_y) + 10;
        final int textYOffset = 18; // y + 14 + 4

        if (cachedInPitWidth == -1) {
            cachedInPitWidth = widthOfText(STR_IN_PIT);
            cachedDeltaPlaceholderWidth = widthOfText("-" + DeltaFormat.formatDelta(0));
        }

        int width = 50;
        int raceLap = 0;

        if (!race.raceLeaderboardPositions.isEmpty()) {
            raceLap = race.laps.getOrDefault(race.raceLeaderboardPositions.get(0).player_name, 0);
            if (raceLap > race.getTotalLaps()) {
                raceLap = race.getTotalLaps();
            }
        }

        String header = "Race at " + SWRC.getRaceName();
        renderText(context, header, baseX + 32, baseY + 5, COLOR_WHITE);

        if (race.getTotalLaps() < 10000) {
            int totalPits = race.getTotalPits();
            String pitText = totalPits == 1 ? " Pit" : " Pits";
            String lapInfo = "Lap " + raceLap + "/" + race.getTotalLaps() + " :: " + totalPits + pitText;
            renderText(context, lapInfo, baseX + 46 + widthOfText(header), baseY + 5, COLOR_WHITE);
        }

        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            int nameWidth = widthOfText(position.player_name);
            if (nameWidth > width) {
                width = nameWidth;
            }
        }
        width += 98;

        final int totalLaps = race.getTotalLaps();
        final String flapPlayerName = race.getFlap() != null ? race.getFlap().getPlayerName() : null;
        final boolean hasShadow = SWRCConfig.getInstance().leaderboard_shadow;

        long lastDelta = 0;
        int offset = 0;

        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            final String playerName = position.player_name;

            int posColor = COLOR_WHITE;
            if (offset == 0) posColor = COLOR_GOLD;
            else if (offset == 1) posColor = COLOR_SILVER;
            else if (offset == 2) posColor = COLOR_BRONZE;

            double currentHeight = rowHeight.getOrDefault(playerName, (double) (offset * 9));
            double targetHeight = offset * 9.0;
            double preciseHeight = lerp(currentHeight, targetHeight, LERP_SPEED);
            int derivedHeight = (int) Math.round(preciseHeight);

            final int rowY = baseY + textYOffset + derivedHeight;

            Integer pits = race.pits.get(playerName);
            Integer laps = race.laps.get(playerName);
            int pitCount = pits != null ? pits : 0;
            int lapCount = laps != null ? laps : 0;

            renderTextDirect(context, Integer.toString(offset + 1), baseX + 4, rowY, posColor, hasShadow);

            boolean isFlapHolder = flapPlayerName != null && flapPlayerName.equals(playerName);
            int nameColor = isFlapHolder ? COLOR_FLAP : COLOR_WHITE;
            renderTextDirect(context, playerName, baseX + 28, rowY, nameColor, hasShadow);

            if (position.in_pit) {
                int startPos = width - cachedInPitWidth - 2;
                renderTextDirect(context, STR_IN_PIT, baseX + startPos + 6, rowY, COLOR_GRAY, hasShadow);
            } else if (lapCount > totalLaps) {
                renderTextDirect(context, STR_FINISHED, baseX + width - 34, rowY, COLOR_GRAY, hasShadow);
            } else {
                int startPos = width - cachedDeltaPlaceholderWidth - 2;
                long delta = position.time_delta - lastDelta;

                if (delta < 0) {
                    int deltaColor = ColorUtil.lerpColor(
                            COLOR_YELLOW_DELTA,
                            COLOR_RED_DELTA,
                            clamp((float) Math.pow(2, delta / 60000f * -5), 0, 1)
                    );
                    renderTextDirect(context, DeltaFormat.formatDelta(delta), baseX + startPos + 6, rowY, deltaColor, hasShadow);
                } else if (delta > 0) {
                    renderTextDirect(context, STR_ERR, baseX + startPos + 6, rowY, COLOR_ERROR, hasShadow);
                } else {
                    String displayText = offset == 0 ? STR_INTERVAL : STR_DASH;
                    int displayColor = offset == 0 ? COLOR_GREEN : COLOR_GRAY;
                    renderTextDirect(context, displayText, baseX + startPos + 6, rowY, displayColor, hasShadow);
                }

                lastDelta = position.time_delta;
            }

            renderTextDirect(context, Integer.toString(pitCount), baseX + width - 51, rowY, COLOR_CYAN, hasShadow);

            if (lapCount <= totalLaps) {
                renderTextDirect(context, Integer.toString(lapCount), baseX + width - 64, rowY, COLOR_YELLOW, hasShadow);
            }

            PlayerListEntry playerListEntry = SWRC.minecraftClient.getNetworkHandler().getPlayerListEntry(playerName);
            if (playerListEntry != null) {
                PlayerSkinDrawer.draw(context, playerListEntry.getSkinTextures(), baseX + 18, rowY, 8);
            }

            rowHeight.put(playerName, preciseHeight);
            offset++;
        }

        if (race.getFlap() != null) {
            String flapText = race.getFlap().getPlayerName() + ": " + DeltaFormat.formatMillis(race.getFlap().getTime());
            renderTextDirect(context, flapText, baseX + 22, baseY + textYOffset + offset * 9, COLOR_FLAP_YELLOW, hasShadow);
        }

        context.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, baseX + 3, baseY + 3, 0, 0, 25, 10, 256, 256);

        stack.popMatrix();
    }

    private static void renderTextDirect(DrawContext graphics, String text, int x, int y, int color, boolean shadow) {
        graphics.drawText(SWRC.minecraftClient.textRenderer, text, x, y, color, shadow);
    }

    public static void renderText(DrawContext graphics, String text, int x, int y, int color) {
        graphics.drawText(SWRC.minecraftClient.textRenderer, text, x, y, color, SWRCConfig.getInstance().leaderboard_shadow);
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }
}