package uk.cloudmc.swrc.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.lang.module.Configuration;
import java.net.URI;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class RootCommand implements CommandNodeProvider {

    public ServerCommand serverCommand = new ServerCommand();
    public TrackBuilderCommand trackBuilderCommand = new TrackBuilderCommand();
    public RaceCommand raceCommand = new RaceCommand();

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("swrc")
                .executes(this::doQuickConnect)
                .then(serverCommand.command())
                .then(trackBuilderCommand.command())
                .then(raceCommand.command());
    }

    private int doQuickConnect(CommandContext<FabricClientCommandSource> context) {
        URI default_uri = URI.create(SWRCConfig.getInstance().default_server);

        try {
            WebsocketManager.connect(default_uri);
        } catch (Exception e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed to connect: " + e.getMessage()));
            return 0;
        }

        return Command.SINGLE_SUCCESS;
    }
}
