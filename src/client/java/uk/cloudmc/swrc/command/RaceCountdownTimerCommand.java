package uk.cloudmc.swrc.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.C2STimerPacket;
import uk.cloudmc.swrc.util.ChatFormatter;
import uk.cloudmc.swrc.util.NTPTimeSync;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class RaceCountdownTimerCommand implements CommandNodeProvider {

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("timer")
            .then(
                literal("new")
                .then(
                    argument("time", StringArgumentType.string())
                    .executes(this::doNewTimer)
                )
            )
            .then(
                literal("start")
                .executes(this::doStartTimer)
            )
            .then(
                literal("stop")
                .executes(this::doStopTimer)
            );
    }

    private int doStopTimer(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {
            C2STimerPacket timerPacket = new C2STimerPacket();
            timerPacket.duration = -1;
            timerPacket.start_time = -1;
            WebsocketManager.rcWebsocketConnection.sendPacket(timerPacket);
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Stopping timer"));
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private int doStartTimer(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {
            C2STimerPacket timerPacket = new C2STimerPacket();
            timerPacket.duration = SWRC.getRace().getTimerDuration();
            timerPacket.start_time = NTPTimeSync.getTrueTime();
            WebsocketManager.rcWebsocketConnection.sendPacket(timerPacket);
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Starting %s second(s) timer.", timerPacket.duration)));
            return Command.SINGLE_SUCCESS;
        }
        return 0;
    }

    private int doNewTimer(CommandContext<FabricClientCommandSource> context) {
        String time_set = StringArgumentType.getString(context, "time");

        int total_time = 0;

        Pattern pattern = Pattern.compile("(\\d+)([ydhms])");
        Matcher matcher = pattern.matcher(time_set);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "y":
                    total_time += value * 31536000;
                    break;
                case "d":
                    total_time += value * 86400;
                    break;
                case "h":
                    total_time += value * 3600;
                    break;
                case "m":
                    total_time += value * 60;
                    break;
                case "s":
                    total_time += value;
                    break;
            }
        }

        if (WebsocketManager.rcSocketAvalible()) {
            C2STimerPacket timerPacket = new C2STimerPacket();
            timerPacket.duration = total_time;
            timerPacket.start_time = -1;
            WebsocketManager.rcWebsocketConnection.sendPacket(timerPacket);
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Initialized %s second(s) timer.", timerPacket.duration)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }
}
