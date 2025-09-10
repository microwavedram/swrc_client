package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import uk.cloudmc.swrc.SWRC;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EventsQueue implements Hud {
    private int scaledWidth;
    private int scaledHeight;

    private static final ConcurrentLinkedDeque<EventEntry> lines = new ConcurrentLinkedDeque<>();

    private static double calculated_height = 0;

    private record EventEntry(String line, long expiry) {}

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && !lines.isEmpty();
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }

    public void addLine(String line) {
        lines.add(new EventEntry(line, System.currentTimeMillis() + 10000));
    }

    @Override
    public void render(DrawContext context, float tickDelta) {
        this.scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        this.scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        //RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        calculated_height = lerp(calculated_height, lines.size() * 9, 0.05);

        int h = scaledHeight - (int) Math.round(calculated_height) - 10;

        for (Iterator<EventEntry> it = lines.descendingIterator(); it.hasNext(); ) {
            EventEntry entry = it.next();

            context.drawTextWithShadow(SWRC.minecraftClient.textRenderer, entry.line, scaledWidth - widthOfText(entry.line) - 10, h, 0xFFFFFFFF);

            h += 9;
        }

        EventEntry eventEntry = lines.peekFirst();

        if (eventEntry != null && eventEntry.expiry < System.currentTimeMillis()) {
            lines.removeFirst();
        }
    }
}
