package uk.cloudmc.swrc.util;

import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import uk.cloudmc.swrc.SWRC;

public class PlayerNameValidator {

    private static int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= len2; j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1,     // deletion
                                dp[i][j - 1] + 1),    // insertion
                        dp[i - 1][j - 1] + cost        // substitution
                );
            }
        }

        return dp[len1][len2];
    }

    public static void validateName(String name) {
        assert SWRC.minecraftClient.getNetworkHandler() != null;

        for (PlayerListEntry playerListEntry : SWRC.minecraftClient.getNetworkHandler().getListedPlayerListEntries()) {
            if (levenshteinDistance(name, playerListEntry.getProfile().getName()) < 3 && !name.equals(playerListEntry.getProfile().getName())) {
                SWRC.minecraftClient.inGameHud.getChatHud().addMessage(
                        ChatFormatter.GENERIC_MESSAGE_PREFIX()
                            .append(Text.literal(name).styled(style -> style.withFormatting(Formatting.GOLD)))
                            .append(Text.literal(" seems awfully similar to ").styled(style -> style.withFormatting(Formatting.WHITE)))
                            .append(Text.literal(playerListEntry.getProfile().getName()).styled(style -> style.withFormatting(Formatting.GOLD)))

                );
                SWRC.minecraftClient.inGameHud.getChatHud().addMessage(
                        ChatFormatter.GENERIC_MESSAGE_PREFIX()
                            .append(Text.literal("Do you want to rename them? "))
                            .append(Text.literal("[RENAME]").styled(style ->
                                style
                                    .withFormatting(Formatting.GREEN)
                                    .withHoverEvent(
                                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Rename to " + playerListEntry.getProfile().getName()))
                                    )
                                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/swrc race player rename " + name + " " + playerListEntry.getProfile().getName()))
                                )
                            )
                );

            }
        }
    }
}
