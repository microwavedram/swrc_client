package uk.cloudmc.swrc.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import uk.cloudmc.swrc.SWRC;

import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class EventsQueue implements Hud {
    private int scaledWidth;
    private int scaledHeight;
    private static final DecimalFormat decimalFormat = new DecimalFormat("00.000");

    private static Deque<EventEntry> lines = new ArrayDeque<>();

    private static double calculated_height = 0;

    private record EventEntry(String line, long expiry) {}

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    @Override
    public boolean shouldRender() {
        return SWRC.getRace() != null && !lines.isEmpty();
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

        long time_current = System.currentTimeMillis() ;
        long time_remaining = Math.min(Math.max(SWRC.getRace().getDuration() * 1000 - (time_current - SWRC.getRace().getStartTime()), 0), SWRC.getRace().getDuration() * 1000);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        calculated_height = lerp(calculated_height, lines.size() * 9, 0.05);

        int h = scaledHeight - (int) Math.round(calculated_height) - 10;

        for (Iterator<EventEntry> it = lines.descendingIterator(); it.hasNext(); ) {
            EventEntry entry = it.next();

            context.drawTextWithShadow(SWRC.minecraftClient.textRenderer, entry.line, scaledWidth - widthOfText(entry.line) - 10, h, 0xFFFFFF);

            h += 9;
        }

        EventEntry eventEntry = lines.peekFirst();

        if (eventEntry != null && eventEntry.expiry < System.currentTimeMillis()) {
            lines.removeFirst();
        }
    }
}
