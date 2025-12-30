package uk.cloudmc.swrc.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.C2SCreateNewSessionPacket;
import uk.cloudmc.swrc.net.packets.C2SEndSessionPacket;
import uk.cloudmc.swrc.net.packets.C2SNameSessionPacket;
import uk.cloudmc.swrc.net.packets.S2CSessionsPacket;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class ServerCommand implements CommandNodeProvider {

    private static class SessionSuggester implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (WebsocketManager.swrcSocketAvalible()) {
                for (Map.Entry<String, S2CSessionsPacket.Session> entry : WebsocketManager.swrcWebsocketConnection.getSessions().entrySet()) {
                    String id = entry.getKey();

                    if (!id.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(id);
                }
            }

            return builder.buildFuture();
        }
    }

    private static class ServerSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRCConfig.getInstance().default_server.toLowerCase().contains(builder.getRemainingLowerCase())) {
                builder.suggest('"' + SWRCConfig.getInstance().default_server + '"');
            }

            if ("ws://localhost:7777".toLowerCase().contains(builder.getRemainingLowerCase()) && FabricLoader.getInstance().isDevelopmentEnvironment()) {
                builder.suggest('"' + "ws://localhost:7777" + '"');
            }

            return builder.buildFuture();
        }
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("server").
            then(
                literal("connect")
                .then(
                    argument("uri", StringArgumentType.string())
                    .suggests(new ServerSuggestor())
                    .executes(this::doConnect)
                )
            )
            .then(
                literal("sessions")
                .then(
                        literal("create")
                        .executes(this::doCreateSession)
                )
                .then(
                    argument("session", StringArgumentType.string())
                    .suggests(new SessionSuggester())
                    .then(
                        literal("connect")
                        .executes(this::doConnectSession)
                    )
                    .then(
                        literal("status")
                        .then(
                            argument("status_text", StringArgumentType.greedyString())
                            .executes(this::doSetSessionStatus)
                        )
                    )
                    .then(
                        literal("destroy")
                        .executes(this::doDestroySession)
                    )
                )
                .executes(
                        this::showSessions
                )
            );
    }

    private int doSetSessionStatus(CommandContext<FabricClientCommandSource> context) {
        String session = StringArgumentType.getString(context, "session");
        String status_text = StringArgumentType.getString(context, "status_text");

        if (WebsocketManager.swrcSocketAvalible()) {

            if (!WebsocketManager.swrcWebsocketConnection.sessions.containsKey(session)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Invalid session"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", ""));
                return 0;
            }

            C2SNameSessionPacket packet = new C2SNameSessionPacket();
            packet.name = status_text;
            packet.key = SWRCConfig.getInstance().swrc_key;
            packet.session = session;

            WebsocketManager.swrcWebsocketConnection.sendPacket(packet);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: SWRC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc", "to connect to the default server"));
        return 0;
    }

    private int doDestroySession(CommandContext<FabricClientCommandSource> context) {
        String session = StringArgumentType.getString(context, "session");

        if (WebsocketManager.swrcSocketAvalible()) {

            if (!WebsocketManager.swrcWebsocketConnection.sessions.containsKey(session)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Invalid session"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", ""));
                return 0;
            }

            C2SEndSessionPacket packet = new C2SEndSessionPacket();

            packet.session = session;
            packet.key = SWRCConfig.getInstance().swrc_key;

            WebsocketManager.swrcWebsocketConnection.sendPacket(packet);
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Destroying Session."));

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: SWRC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc", "to connect to the default server"));
        return 0;
    }

    private int showSessions(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.swrcSocketAvalible()) {
            WebsocketManager.swrcWebsocketConnection.promptSessions();

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: SWRC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc", "to connect to the default server"));
        return 0;
    }

    private int doConnectSession(CommandContext<FabricClientCommandSource> context) {
        String session = StringArgumentType.getString(context, "session");

        if (WebsocketManager.swrcSocketAvalible()) {

            if (!WebsocketManager.swrcWebsocketConnection.sessions.containsKey(session)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Invalid session"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", ""));
                return 0;
            }

            WebsocketManager.connectSession(session);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: SWRC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc", "to connect to the default server"));
        return 0;
    }

    private int doCreateSession(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.swrcSocketAvalible()) {
            C2SCreateNewSessionPacket createNewSessionPacket = new C2SCreateNewSessionPacket();

            createNewSessionPacket.key = SWRCConfig.getInstance().swrc_key;

            WebsocketManager.swrcWebsocketConnection.sendPacket(createNewSessionPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: SWRC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc", "to connect to the default server"));
        return 0;
    }

    private int doConnect(CommandContext<FabricClientCommandSource> context) {
        String server = StringArgumentType.getString(context, "uri");

        if (server.contains("http://") || server.contains("https://")) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Unsupported Protocol"));
            context.getSource().sendFeedback(ChatFormatter.HINT("use ws:// or wss://"));
            return 0;
        }

        WebsocketManager.connect(URI.create(server));

        return Command.SINGLE_SUCCESS;
    }
}
