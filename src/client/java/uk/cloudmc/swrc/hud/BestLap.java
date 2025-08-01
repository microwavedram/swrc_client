package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;

import java.text.DecimalFormat;

public class BestLap implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");

    private final static double TOP_TARGET_PERCENTAGE = 0.1;
    private final static double ON_SCREEN_TIME = 2;
    private static long begin_time = 0;

    private int scaledWidth;
    private int scaledHeight;



    private static final DecimalFormat decimalFormat = new DecimalFormat("00.000");

    public BestLap() {}

    @Override
    public boolean shouldRender() {
        if (SWRC.minecraftClient.player == null) return false;
        return SWRC.getRace() != null;
    }

    @Override
    public void render(DrawContext graphics, float tickDelta) {
        Race race = SWRC.getRace();

        if (race.getFlap() == null) return;

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int u = 32;
        int v = 0;
        int w = 192;
        int h = 22;

        int p = (int) (scaledHeight * TOP_TARGET_PERCENTAGE);
        double x = (double) (System.currentTimeMillis() - begin_time) / 1000;

        // y=\max\left(0,\min\left(p,-\frac{6px}{t^{2}}\left(x-t\right)\left\{0<x<t\right\}\right)\right) :: p = peak :: t = ticks on screen
        int animationHeight = (int) Math.floor(Math.max(0, Math.min(p, -(6 * p * x)/(ON_SCREEN_TIME * ON_SCREEN_TIME) * (x - ON_SCREEN_TIME))));

        graphics.drawTexture(RenderLayer::getGuiTextured, WIDGETS_TEXTURE, scaledWidth / 2 - w / 2, animationHeight - h, u, v, w, h, 256, 256);

    }

    public void show() {
        begin_time = System.currentTimeMillis();
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
