package uk.cloudmc.swrc.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ChatFormatter {
    public static MutableText SWRC_PREFIX() {
        MutableText swPart = Text.literal("SW").formatted(Formatting.GOLD);
        MutableText rcPart = Text.literal("RC").formatted(Formatting.AQUA);
        return swPart.append(rcPart);
    }

    public static MutableText GENERIC_MESSAGE_PREFIX() {
        MutableText prefix = SWRC_PREFIX();
        MutableText separator = Text.literal(" | ").formatted(Formatting.WHITE);
        return prefix.append(separator);
    }

    public static MutableText GENERIC_MESSAGE(String message) {
        MutableText prefix = GENERIC_MESSAGE_PREFIX();
        MutableText content = Text.literal(message).formatted(Formatting.WHITE);
        return prefix.append(content);
    }

    public static MutableText HINT_COMMAND(String prefix, String command, String suffix) {
        return Text.empty()
                .styled(style -> style.withFormatting(Formatting.ITALIC).withFormatting(Formatting.GRAY))
                .append(Text.literal("hint: "))
                .append(prefix)
                .append(Text.literal(" "))
                .append(Text.literal(command).styled(style -> style
                        .withFormatting(Formatting.UNDERLINE)
                        .withFormatting(Formatting.BLUE)
                        .withClickEvent(new ClickEvent.SuggestCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal(command).styled(style1 -> style1.withFormatting(Formatting.BLUE))))
                ))
                .append(Text.literal(" "))
                .append(suffix);
    }

    public static MutableText HINT(String message) {
        return Text.empty()
                .styled(style -> style.withFormatting(Formatting.ITALIC).withFormatting(Formatting.GRAY))
                .append(Text.literal("hint: "))
                .append(message);
    }
}
