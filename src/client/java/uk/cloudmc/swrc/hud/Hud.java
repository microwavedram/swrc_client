package uk.cloudmc.swrc.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public interface Hud extends HudElement {
    boolean shouldRender();

    void $render(DrawContext context, RenderTickCounter tickCounter);

    default void render(DrawContext context, RenderTickCounter tickCounter) {
        if (shouldRender()) this.$render(context, tickCounter);
    }
}
