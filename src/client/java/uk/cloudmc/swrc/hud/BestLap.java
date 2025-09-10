package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.net.packets.S2CUpdatePacket;
import uk.cloudmc.swrc.util.DeltaFormat;

public class BestLap implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");

    private final static double TOP_TARGET_PERCENTAGE = 0.15;
    private final static double ON_SCREEN_TIME = 5;
    private static long begin_time = 0;

    private String flap_owner = "";
    private long flap = 0;

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

        int scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        int scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        //DrawContext.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int u = 32;
        int v = 0;
        int w = 192;
        int h = 22;

        int p = (int) (scaledHeight * TOP_TARGET_PERCENTAGE);
        double x = (double) (System.currentTimeMillis() - begin_time) / 1000;

        // y=\max\left(0,\min\left(p,-\frac{6px}{t^{2}}\left(x-t\right)\left\{0<x<t\right\}\right)\right) :: p = peak :: t = ticks on screen
        int animationHeight = (int) Math.floor(Math.max(0, Math.min(p, -(6 * p * x)/(ON_SCREEN_TIME * ON_SCREEN_TIME) * (x - ON_SCREEN_TIME))));

        graphics.drawTexture(RenderPipelines.GUI_TEXTURED , WIDGETS_TEXTURE, scaledWidth / 2 - w / 2, animationHeight - h, u, v, w, h, 256, 256);
        graphics.drawText(
                SWRC.minecraftClient.textRenderer,
                Text.literal(flap_owner).styled(style -> style.withFormatting(Formatting.DARK_PURPLE)),
                scaledWidth / 2 - w / 2 + 103,
                animationHeight - h + 3,
                0xFFFFFFFF,
                SWRCConfig.getInstance().leaderboard_shadow
        );
        graphics.drawText(
                SWRC.minecraftClient.textRenderer,
                Text.literal(DeltaFormat.formatMillis(flap)),
                scaledWidth / 2 - w / 2 + 103,
                animationHeight - h + 3  + 9,
                0xFFFFFFFF,
                SWRCConfig.getInstance().leaderboard_shadow
        );
        //graphics.drawTexture(RenderPipelines.GUI_TEXTURED , WIDGETS_TEXTURE, scaledWidth / 2 - w / 2, animationHeight - h, u, v, w, h, 256, 256);
    }

    public void show(S2CUpdatePacket.Flap flap) {
        this.flap_owner = flap.getPlayerName();
        this.flap = flap.getTime();

        begin_time = System.currentTimeMillis();
    }
}
