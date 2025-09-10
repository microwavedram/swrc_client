package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
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

    public QualiLeaderboard() {}

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && SWRC.getRace().getRaceState() == Race.RaceState.QUALI;
    }

    @Override
    public void render(DrawContext graphics, float tickDelta) {
        Race race = SWRC.getRace();

        //RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int width = 50;
        int body_height = SWRC.getRace().raceLeaderboardPositions.size() * 9 + 2;
        int x = 10;
        int y = 10;

        int race_lap = 0;

        if (!race.raceLeaderboardPositions.isEmpty()) {
            race_lap = race.laps.getOrDefault(race.raceLeaderboardPositions.get(0).player_name, 0);
        }

        //renderBox(graphics, WIDGETS_TEXTURE, 0, 0, x, y, 12, body_height, width);

        //graphics.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, x + 3, y + 3, 5, 0, 25, 10, 256, 256);

        String header = String.format(SWRCConfig.getInstance().header_text, SWRC.getRaceName());

        renderText(graphics, header, x + 32, y + 5, 0xFFFFFFFF);

        renderText(graphics, String.format("Lap %s / %s", race_lap, race.getTotalLaps()), x + 46 + widthOfText(header), y + 5, 0xFFFFFFFF);

        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            width = Math.max(width, widthOfText(position.player_name));
        }

        long last_delta = 0;

        int offset = 0;
        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            int pos_color = 0xFFFFFFFF;

            if (offset == 0) pos_color = 0xFFFCBA03;
            if (offset == 1) pos_color = 0xFFB2B1BD;
            if (offset == 2) pos_color = 0xFF805B2B;

            double precise_targeted_height = lerp(rowHeight.getOrDefault(position.player_name, (double) offset  * 9), offset  * 9, 0.05);
            int derived_height = (int) Math.round(precise_targeted_height);

            PlayerListEntry playerListEntry = SWRC.minecraftClient.getNetworkHandler().getPlayerListEntry(position.player_name);

//            if (playerListEntry != null) {
//                PlayerSkinDrawer.draw(graphics, playerListEntry.getSkinTextures(), x + 12 + 6, y + 14 + derived_height + 4, 8);
//            }

            renderText(graphics, String.format("%s", offset + 1), x + 4, y + 14 + derived_height + 4, pos_color);
            renderText(graphics, String.format("%s", position.player_name), x + 28, y + 14 + derived_height + 4, 0xFFFFFFFF);

            int start_pos = width - widthOfText("-" + DeltaFormat.formatDelta(position.time_delta)) + 110;

            if (position.flap == -1) {
                renderText(graphics, "-", x + start_pos - 30, y + 14 + derived_height + 4, 0xFFEBCC34 );
            } else {
                if (position.time_delta == 0) {
                    renderText(
                            graphics,
                            "INTERVAL",
                            x + start_pos + 26,
                            y + 14 + derived_height + 4,
                            0xFFCCCCCC
                    );
                } else {
                    renderText(
                            graphics,
                            DeltaFormat.formatDelta(position.time_delta),
                            x + start_pos + 26,
                            y + 14 + derived_height + 4,
                            ColorUtil.lerpColor(
                                    0xFFf5ee6a, // yellow
                                    0xFFf56a6a, // red
                                    clamp((float) Math.pow(2, position.time_delta / 60000f * -5), 0, 1)
                            )
                    );
                }

                renderText(graphics, String.format("%s", DeltaFormat.formatMillis(position.flap)), x + start_pos - 24, y + 14 + derived_height + 4, 0xFFEBCC34 );

                last_delta = position.time_delta;
            }

            if (playerListEntry != null) {
                PlayerSkinDrawer.draw(graphics, playerListEntry.getSkinTextures(), x + 12 + 6, y + 14 + derived_height + 4, 8);
            }

            rowHeight.put(position.player_name, precise_targeted_height);

            offset += 1;
        }
        graphics.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, x + 3, y + 3, 5, 0, 25, 10, 256, 256);
    }

    public static void renderText(DrawContext graphics, String text, int x, int y, int color) {
        graphics.drawText(SWRC.minecraftClient.textRenderer, text, x, y, color, SWRCConfig.getInstance().leaderboard_shadow);
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }

    /*public static void renderBox(DrawContext graphics, Identifier texture, int tx, int ty, int x, int y, int hh, int bh, int w) {
        // Top
        graphics.drawTexture(texture, x, y, tx, ty, 3, 3);
        for (int i = 0; i < w; i++) {
            graphics.drawTexture(texture, x + 3 + i, y, tx + 3, ty, 1, 3);
        }
        graphics.drawTexture(texture, x + w + 3, y, tx + 4, ty, 1, 3);

        for (int hy = 0; hy < hh; hy++) {
            graphics.drawTexture(texture, x, y + hy + 3, tx, ty + 3, 3, 1);
            graphics.drawTexture(texture, x + w + 3, y + hy + 3, tx + 4, ty + 3, 1, 1);
        }


        graphics.drawTexture(GREY_TEXTURE, x + 3, y + 3, 0, 0, w, hh);

        graphics.drawTexture(texture, x, y + hh + 3, tx, ty + 4, 3, 1);
        for (int hsx = 0; hsx < w; hsx++) {
            graphics.drawTexture(texture, x + hsx + 3, y + hh + 3, tx + 3, tx + 4, 1, 1);
        }
        graphics.drawTexture(texture, x + 3 + w, y + hh + 3, tx + 4, ty + 4, 1, 1);

        graphics.drawTexture(GREY_TEXTURE, x + 3, y + hh + 4, 0, 0, w, bh);
        for (int bby = 0; bby < bh; bby++) {
            graphics.drawTexture(texture, x, y + hh + bby + 4, tx, ty + 5, 3, 1);

            graphics.drawTexture(texture, x + w + 3, y + hh + bby + 4, tx + 4, ty + 5, 1, 1);
        }

        graphics.drawTexture(texture, x, y + hh + bh + 4, tx, ty + 6, 3, 3);
        for (int fx = 0; fx < w; fx++) {
            graphics.drawTexture(texture, x + fx + 3, y + hh + bh + 4, tx + 3, ty + 6, 1, 3);
        }
        graphics.drawTexture(texture, x + w + 3, y + hh + bh + 4, tx + 4, ty + 6, 1, 3);
    }*/
}
