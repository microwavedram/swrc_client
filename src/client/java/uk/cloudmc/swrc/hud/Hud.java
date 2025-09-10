package uk.cloudmc.swrc.hud;

import net.minecraft.client.gui.DrawContext;

public interface Hud {
    boolean shouldRender();
    void render(DrawContext context, float tickDelta);
}
