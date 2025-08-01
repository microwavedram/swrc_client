package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.net.packets.S2CUpdatePacket;

import java.text.DecimalFormat;
import java.util.HashMap;

public class RaceLeaderboard implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");
    private static final Identifier GREY_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/grey.png");

    private int scaledWidth;
    private int scaledHeight;

    private static final DecimalFormat decimalFormat = new DecimalFormat("00.000");

    private static final HashMap<String, Double> rowHeight = new HashMap<>();

    public RaceLeaderboard() {}

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && SWRC.getRace().getRaceState() != Race.RaceState.QUALI;
    }

    @Override
    public void render(DrawContext graphics, float tickDelta) {
        Race race = SWRC.getRace();

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int width = 200;
        int body_height = SWRC.getRace().raceLeaderboardPositions.size() * 9 + 2;
        int x = 10;
        int y = 10;

        //renderBox(graphics, WIDGETS_TEXTURE, 0, 0, x, y, 12, body_height, width);

        graphics.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, x + 3, y + 3, 5, 0, 25, 10, 256, 256);

        renderText(graphics, String.format(SWRCConfig.getInstance().header_text, SWRC.getRaceName()), x + 32, y + 4, 0xFFFFFF);
        renderText(graphics, String.format("%s Laps %s Pits", race.getTotalLaps(), race.getTotalPits()), x + 120, y + 4, 0xFFFFFF);

        int offset = 0;
        for (S2CUpdatePacket.RaceLeaderboardPosition position : race.raceLeaderboardPositions) {
            int pos_color = 0xFFFFFF;

            if (offset == 0) pos_color = 0xFCBA03;
            if (offset == 1) pos_color = 0xB2B1BD;
            if (offset == 2) pos_color = 0x805B2B;

            double precise_targeted_height = lerp(rowHeight.getOrDefault(position.player_name, (double) offset  * 9), offset  * 9, 0.05);
            int derived_height = (int) precise_targeted_height;

            PlayerListEntry playerListEntry = SWRC.minecraftClient.getNetworkHandler().getPlayerListEntry(position.player_name);

            if (playerListEntry != null) {
                PlayerSkinDrawer.draw(graphics, playerListEntry.getSkinTextures(), x + 12 + 6, y + 14 + derived_height + 4, 8);
            }

            renderText(graphics, String.format("%s", offset + 1), x + 4, y + 14 + derived_height + 4, pos_color);
            renderText(graphics, String.format("%s", position.player_name), x + 22 + 6, y + 14 + derived_height + 4, race.getFlap() != null && race.getFlap().getPlayer_name().equals(position.player_name) ? 0x9803FC : 0xFFFFFF);

            if (race.laps.getOrDefault(position.player_name, 0) > race.getTotalLaps()) {
                int start_pos = width - widthOfText("FINISHED") - 2;

                renderText(graphics, "FINISHED", x + start_pos + 6, y + 14 + derived_height + 4, 0xBBBBBB );
            } else if (position.in_pit) {
                int start_pos = width - widthOfText("IN PIT") - 2;

                renderText(graphics, "IN PIT", x + start_pos + 6, y + 14 + derived_height + 4, 0x888888 );
            } else {
                int start_pos = width - widthOfText("-" + msToTimeString(position.time_delta)) - 2;

                renderText(graphics, String.format("%s%s", position.time_delta > 0 ? "+" : "" , msToTimeString(position.time_delta)), x + start_pos + 6, y + 14 + derived_height + 4, position.time_delta >= 0 ? 0x00FF00 : 0xFF0000 );
            }

            int pits = race.pits.getOrDefault(position.player_name, 0);

            renderText(graphics, String.format("%s", pits), x + width - 57 + 6, y + 14 + derived_height + 4, 0x00FFFF );
            renderText(graphics, String.format("%s", race.laps.getOrDefault(position.player_name, 0)), x + width - 70 + 6, y + 14 + derived_height + 4, 0xFFFF00 );

            rowHeight.put(position.player_name, precise_targeted_height);

            offset += 1;
        }
        if (race.getFlap() != null) {
            renderText(graphics, String.format("%s: %s", race.getFlap().getPlayer_name(), msToTimeString(race.getFlap().getTime())), x + 22, y + 14 + offset * 9 + 4, 0xAFFF14);
        }
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
