package uk.cloudmc.swrc.command;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.C2SCreateNewSessionPacket;
import uk.cloudmc.swrc.net.packets.C2SPushTrackPacket;
import uk.cloudmc.swrc.net.packets.S2CSessionsPacket;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.tt.TimeTrial;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class TTCommand implements CommandNodeProvider {
    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("tt")
            .then(
                literal("new")
                .then(
                    argument("track_file", StringArgumentType.string())
                    .suggests(new RaceCommand.TrackFileSuggestor())
                    .executes(this::doTTCreate)
                )
            )
            .then(
                literal("start")
                .executes(this::doTTStart)
            )
            .then(
                literal("stop")
                .executes(this::doTTStop)
            ).then(
                literal("reset")
                .executes(this::doTTReset)
            ).then(
                literal("quit")
                .executes(this::doTTQuit)
            );
    }

    private int doTTQuit(CommandContext<FabricClientCommandSource> context) {
        TimeTrial timeTrial = SWRC.getTimeTrial();

        if (timeTrial != null) {
            SWRC.setTimeTrial(null);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Quit timetrial session."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a timetrial active"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc tt new ", "to start a timetrial"));
        return 0;
    }

    private int doTTReset(CommandContext<FabricClientCommandSource> context) {
        TimeTrial timeTrial = SWRC.getTimeTrial();

        if (timeTrial != null) {
            timeTrial.reset();
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Reset Fastest."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a timetrial active"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc tt new ", "to start a timetrial"));
        return 0;
    }

    private int doTTStart(CommandContext<FabricClientCommandSource> context) {
        TimeTrial timeTrial = SWRC.getTimeTrial();

        if (timeTrial != null) {
            timeTrial.start();
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Started."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a timetrial active"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc tt new ", "to start a timetrial"));
        return 0;
    }

    private int doTTStop(CommandContext<FabricClientCommandSource> context) {
        TimeTrial timeTrial = SWRC.getTimeTrial();

        if (timeTrial != null) {
            timeTrial.stop();
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Stopped."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a timetrial active"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc tt new ", "to start a timetrial"));
        return 0;
    }

    private int doTTCreate(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "track_file");

        if (SWRC.getTimeTrial() != null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as a time trial is already active"));
            context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc tt quit", "to exit timetrial"));
            return 0;
        }

        String filename = target + ".json";

        try {
            String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("tracks").resolve(filename));

            Track track = Track.deserialize(content);

            TimeTrial timeTrial = new TimeTrial(track);

            SWRC.setTimeTrial(timeTrial);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Successfully loaded from config/swrc/tracks/%s", filename)));
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE_PREFIX().append(Text.literal("[START] ").styled(style ->
                    style.withFormatting(Formatting.GREEN)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("/swrc tt start")))
                            .withClickEvent(new ClickEvent.RunCommand("/swrc tt start"))
            )).append(Text.literal("[STOP] ").styled(style ->
                    style.withFormatting(Formatting.RED)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("/swrc tt stop")))
                            .withClickEvent(new ClickEvent.RunCommand("/swrc tt stop"))
            )).append(Text.literal("[RESET] ").styled(style ->
                    style.withFormatting(Formatting.YELLOW)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("/swrc tt reset")))
                            .withClickEvent(new ClickEvent.RunCommand("/swrc tt reset"))
            )).append(Text.literal("[QUIT]").styled(style ->
                    style.withFormatting(Formatting.GRAY)
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("/swrc tt quit")))
                            .withClickEvent(new ClickEvent.SuggestCommand("/swrc tt quit"))
            )));
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to load from config/swrc/tracks/%s - %s", filename, e.getMessage())));
            return 0;
        } catch (JsonParseException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to parse config/swrc/tracks/%s - %s", filename, e.getMessage())));
            return 0;
        }
    }
}
