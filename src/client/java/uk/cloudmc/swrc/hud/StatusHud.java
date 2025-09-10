package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2fStack;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;

import java.util.List;

public class StatusHud implements Hud {

    private static abstract class StatusChip {
        StatusChip() {

        }

        public abstract boolean shouldRender();

        public abstract void render(DrawContext context);

        public int renderChip(DrawContext context, String text, int x, int y, int color) {
            return StatusHud.drawTextWithBackground(context, text, x, y, color, 1);
        }
    }

    private final List<StatusChip> statuses = List.of(
        new StatusChip() {
            @Override
            public boolean shouldRender() {
                return WebsocketManager.swrcSocketAvalible() && WebsocketManager.racerSocketAvalible();
            }

            @Override
            public void render(DrawContext context) {
                int a = this.renderChip(context, "RACER", 0, 0, 0xAA555555);

                if (WebsocketManager.racerWebsocketConnection.isClosing()) {
                    this.renderChip(context, "CLOSING", a, 0, 0xAAF5786A);
                } else if (WebsocketManager.racerWebsocketConnection.isClosed()) {
                    this.renderChip(context, "CLOSED", a, 0, 0xAAF5786A);
                } else if (WebsocketManager.racerWebsocketConnection.isOpen()) {
                    if (SWRC.getRace() != null) {
                        this.renderChip(context, "OPEN", a, 0, 0xAA6AF596);
                    } else {
                        this.renderChip(context, "IDLE", a, 0, 0xAAF4D96B);
                    }
                } else {
                    this.renderChip(context, "UNKNOWN", a, 0, 0xAAAAAAAA);
                }
            }
        },
        new StatusChip() {
            @Override
            public boolean shouldRender() {
                return WebsocketManager.swrcSocketAvalible() && WebsocketManager.rcSocketAvalible();
            }

            @Override
            public void render(DrawContext context) {
                int a = this.renderChip(context, "RC", 0, 0, 0xAA555555);

                if (WebsocketManager.racerSocketAvalible()) {
                    if (SWRC.getRace() != null) {
                        if (SWRCConfig.getInstance().pos_tracking) {
                            this.renderChip(context, "TRACKING", a, 0, 0xAA6AF596);
                        } else {
                            this.renderChip(context, "OBSERVING", a, 0, 0xAA6AB9F5);
                        }
                    } else {
                        this.renderChip(context, "IDLE", a, 0, 0xAAF4D96B);
                    }
                }
            }
        },
        new StatusChip() {
            @Override
            public boolean shouldRender() {
                return SWRC.getRace() != null;
            }

            @Override
            public void render(DrawContext context) {
                int a = this.renderChip(context, "RACE", 0, 0, 0xAA555555);

                if (WebsocketManager.racerSocketAvalible()) {
                    switch (SWRC.getRace().getRaceState()) {
                        case NONE -> this.renderChip(context, "NONE", a, 0, 0xAAAAAAAA);
                        case RACE -> this.renderChip(context, "RACE", a, 0, 0xAA6AF596);
                        case QUALI -> this.renderChip(context, "QUALI", a, 0, 0xAA6AF596);
                    }
                } else {
                    this.renderChip(context, "DISCONNECTED", a, 0, 0xAAF5786A);
                }
            }
        }
    );

    public StatusHud() {}

    @Override
    public boolean shouldRender() {
        return true;
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        int scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        int scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

       // RenderSystem.setShader(() -> GameRenderer.getPositionProgram());

        Matrix3x2fStack matrices = context.getMatrices();

         int x = 0;
         int y = scaledHeight - 10;

        matrices.pushMatrix();
        matrices.translate(x, y);

        for (StatusChip chip : this.statuses) {
            if (!chip.shouldRender()) {
                continue;
            }

            matrices.translate(0, -10);

            chip.render(context);
        }

        matrices.popMatrix();
    }

    public static int drawTextWithBackground(DrawContext context, String text, int x, int y, int color, int padding) {
        int text_width = SWRC.minecraftClient.textRenderer.getWidth(text);
        int total_width = text_width + padding * 2;

        context.drawText(SWRC.minecraftClient.textRenderer, text, x + padding, y + padding, 0xFFFFFFFF, true);
        context.fill(x, y, x + total_width, y + 10, color);
        //context.drawText(SWRC.minecraftClient.textRenderer, text, x + padding, y + padding, 0xFFFFFF, true);

        return total_width;
    }
}
