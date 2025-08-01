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
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.C2SCreateNewSessionPacket;
import uk.cloudmc.swrc.net.packets.C2SDestroySessionPacket;
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

                    builder.suggest(id );
                }
            }

            return builder.buildFuture();
        }
    }

    private static class SWRCSuggester implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            String suggestion = '"' + SWRCConfig.getInstance().default_server + '"';

            if (suggestion.toLowerCase().contains(builder.getRemainingLowerCase())) {
                builder.suggest(suggestion);
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
                    .suggests(new SWRCSuggester())
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
                        literal("destroy")
                        .executes(this::doDestroySession)
                    )
                )
                .executes(
                        this::showSessions
                )
            );
    }

    private int doDestroySession(CommandContext<FabricClientCommandSource> context) {
        String session = StringArgumentType.getString(context, "session");

        if (WebsocketManager.swrcSocketAvalible()) {

            if (!WebsocketManager.swrcWebsocketConnection.sessions.containsKey(session)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Invalid session"));
                return 0;
            }

            C2SDestroySessionPacket destroySessionPacket = new C2SDestroySessionPacket();

            destroySessionPacket.key = SWRCConfig.getInstance().swrc_key;
            destroySessionPacket.session = session;

            WebsocketManager.swrcWebsocketConnection.sendPacket(destroySessionPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Not connected"));
        return 0;
    }

    private int showSessions(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.swrcSocketAvalible()) {
            WebsocketManager.swrcWebsocketConnection.promptSessions();

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Not connected"));
        return 0;
    }

    private int doConnectSession(CommandContext<FabricClientCommandSource> context) {
        String session = StringArgumentType.getString(context, "session");

        if (WebsocketManager.swrcSocketAvalible()) {

            if (!WebsocketManager.swrcWebsocketConnection.sessions.containsKey(session)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Invalid session"));
                return 0;
            }

            WebsocketManager.connectSession(session);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Not connected"));
        return 0;
    }

    private int doCreateSession(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.swrcSocketAvalible()) {
            C2SCreateNewSessionPacket createNewSessionPacket = new C2SCreateNewSessionPacket();

            createNewSessionPacket.key = SWRCConfig.getInstance().swrc_key;

            WebsocketManager.swrcWebsocketConnection.sendPacket(createNewSessionPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Not connected"));
        return 0;
    }

    private int doConnect(CommandContext<FabricClientCommandSource> context) {
        String server = StringArgumentType.getString(context, "uri");

        if (server.contains("http://") || server.contains("https://")) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Saisho please no"));
            return 0;
        }

        WebsocketManager.connect(URI.create(server));

        return Command.SINGLE_SUCCESS;
    }
}
