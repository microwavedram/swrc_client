package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;
import uk.cloudmc.swrc.SWRC;

public class DisconnectBanner implements Hud {

    private static final Identifier WIDGETS_TEXTURE = Identifier.of(SWRC.NAMESPACE, "textures/widgets.png");

    private final static double TOP_TARGET_PERCENTAGE = 0.15;
    private final static double ON_SCREEN_TIME = 10;
    private static long begin_time = 0;

    private int scaledWidth;
    private int scaledHeight;

    public DisconnectBanner() {}

    @Override
    public boolean shouldRender() {
        return true;
    }

    @Override
    public void render(DrawContext graphics, float tickDelta) {

        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        //RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        int u = 32;
        int v = 22;
        int w = 192;
        int h = 22;

        int p = (int) (scaledHeight * TOP_TARGET_PERCENTAGE);
        double x = (double) (System.currentTimeMillis() - begin_time) / 1000;

        // y=\max\left(0,\min\left(p,-\frac{6px}{t^{2}}\left(x-t\right)\left\{0<x<t\right\}\right)\right) :: p = peak :: t = ticks on screen
        int animationHeight = (int) Math.floor(Math.max(0, Math.min(p, -(6 * p * x)/(ON_SCREEN_TIME * ON_SCREEN_TIME) * (x - ON_SCREEN_TIME))));

        graphics.drawTexture(RenderPipelines.GUI_TEXTURED, WIDGETS_TEXTURE, scaledWidth / 2 - w / 2, animationHeight - h, u, v, w, h, 256, 256);
    }

    public void show() {
        begin_time = System.currentTimeMillis();
    }
}
