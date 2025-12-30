package uk.cloudmc.swrc.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.util.NTPTimeSync;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class EventsQueue implements Hud {

    private static final ConcurrentLinkedDeque<EventEntry> lines = new ConcurrentLinkedDeque<>();

    private static double calculated_height = 0;

    private record EventEntry(String line, long expiry) {}

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && !lines.isEmpty() && SWRCConfig.getInstance().renderEventFeed;
    }

    public static int widthOfText(String text) {
        return SWRC.minecraftClient.textRenderer.getWidth(text);
    }

    public void addLine(String line) {
        lines.add(new EventEntry(line, NTPTimeSync.getTrueTime() + 10000));
    }

    @Override
    public void $render(DrawContext context, RenderTickCounter tickDelta) {
        final int scaledWidth = SWRC.minecraftClient.getWindow().getScaledWidth();
        final int scaledHeight = SWRC.minecraftClient.getWindow().getScaledHeight();

        calculated_height = lerp(calculated_height, lines.size() * 9, 0.05);

        int h = scaledHeight - (int) Math.round(calculated_height) - 10;

        for (EventEntry entry : lines) {
            context.drawTextWithShadow(SWRC.minecraftClient.textRenderer, entry.line, scaledWidth - widthOfText(entry.line) - 10, h, 0xFFFFFFFF);

            h += 9;
        }

        EventEntry eventEntry = lines.peekFirst();

        if (eventEntry != null && eventEntry.expiry < NTPTimeSync.getTrueTime()) {
            lines.removeFirst();
        }
    }
}
