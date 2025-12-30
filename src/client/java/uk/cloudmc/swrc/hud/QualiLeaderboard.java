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

public class QualiLeaderboard implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");
    private static final HashMap<String, Double> rowHeight = new HashMap<>();

    private static final int COLOR_GOLD = 0xFFFCBA03;
    private static final int COLOR_SILVER = 0xFFB2B1BD;
    private static final int COLOR_BRONZE = 0xFF805B2B;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_LIGHT_GRAY = 0xFFCCCCCC;
    private static final int COLOR_FLAP_YELLOW = 0xFFEBCC34;
    private static final int COLOR_YELLOW_LERP = 0xFFf5ee6a;
    private static final int COLOR_RED_LERP = 0xFFf56a6a;

    private static final String STR_INTERVAL = "INTERVAL";
    private static final String STR_DASH = "-";

    private static int cachedDashWidth = -1;

    private static final double LERP_SPEED = 0.05;

    public QualiLeaderboard() {}

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        Race race = SWRC.getRace();
        return race != null && race.getRaceState() == Race.RaceState.QUALI;
    }

    @Override
    public void $render(DrawContext context, RenderTickCounter tickDelta) {
        Race race = SWRC.getRace();

        Matrix3x2fStack stack = context.getMatrices();

        stack.pushMatrix();
        stack.scale((float) SWRCConfig.getInstance().leaderboard_scale);

        final int baseX = (int) (context.getScaledWindowWidth() * SWRCConfig.getInstance().leaderboard_x) + 10;
        final int baseY = (int) (context.getScaledWindowHeight() * SWRCConfig.getInstance().leaderboard_y) + 10;
        final int textYOffset = 18;

        if (cachedDashWidth == -1) {
            cachedDashWidth = widthOfText(STR_DASH);
        }

        int width = 50;
        int raceLap = 0;

        if (!race.raceLeaderboardPositions.isEmpty()) {
            raceLap = race.laps.getOrDefault(race.raceLeaderboardPositions.get(0).player_name, 0);
        }

        String header = "Quali at " + SWRC.getRaceName();
        renderText(context, header, baseX + 32, baseY + 5, COLOR_WHITE);

        String lapInfo = "Lap " + raceLap + " / " + race.getTotalLaps();
        renderText(context, lapInfo, baseX + 46 + widthOfText(header), baseY + 5, COLOR_WHITE);

        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            int nameWidth = widthOfText(position.player_name);
            if (nameWidth > width) {
                width = nameWidth;
            }
        }

        final boolean hasShadow = SWRCConfig.getInstance().leaderboard_shadow;

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

            renderTextDirect(context, Integer.toString(offset + 1), baseX + 4, rowY, posColor, hasShadow);

            renderTextDirect(context, playerName, baseX + 28, rowY, COLOR_WHITE, hasShadow);

            int startPos = width - widthOfText(STR_DASH + DeltaFormat.formatDelta(position.time_delta)) + 110;

            if (position.flap == -1) {
                renderTextDirect(context, STR_DASH, baseX + startPos - 30, rowY, COLOR_FLAP_YELLOW, hasShadow);
            } else {
                if (position.time_delta == 0) {
                    renderTextDirect(context, STR_INTERVAL, baseX + startPos + 26, rowY, COLOR_LIGHT_GRAY, hasShadow);
                } else {
                    int deltaColor = ColorUtil.lerpColor(
                            COLOR_YELLOW_LERP,
                            COLOR_RED_LERP,
                            clamp((float) Math.pow(2, position.time_delta / 60000f * -5), 0, 1)
                    );
                    renderTextDirect(context, DeltaFormat.formatDelta(position.time_delta), baseX + startPos + 26, rowY, deltaColor, hasShadow);
                }

                renderTextDirect(context, DeltaFormat.formatMillis(position.flap), baseX + startPos - 24, rowY, COLOR_FLAP_YELLOW, hasShadow);
            }

            PlayerListEntry playerListEntry = SWRC.minecraftClient.getNetworkHandler().getPlayerListEntry(playerName);
            if (playerListEntry != null) {
                PlayerSkinDrawer.draw(context, playerListEntry.getSkinTextures(), baseX + 18, rowY, 8);
            }

            rowHeight.put(playerName, preciseHeight);
            offset++;
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