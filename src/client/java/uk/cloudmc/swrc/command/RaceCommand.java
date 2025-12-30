package uk.cloudmc.swrc.command;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.util.ChatFormatter;
import uk.cloudmc.swrc.util.PlayerNameValidator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class
RaceCommand implements CommandNodeProvider {

    private static class BoolSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if ("false".toLowerCase().contains(builder.getRemainingLowerCase())) builder.suggest("false");
            if ("true".toLowerCase().contains(builder.getRemainingLowerCase())) builder.suggest("true");

            return builder.buildFuture();
        }
    }

    private static class PlayerSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRC.minecraftClient.world != null) {
                for (AbstractClientPlayerEntity player : SWRC.minecraftClient.world.getPlayers()) {
                    String name = player.getName().getString();

                    if (!name.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(name);
                }

                for (PlayerListEntry player : Objects.requireNonNull(SWRC.minecraftClient.getNetworkHandler()).getListedPlayerListEntries().stream().toList()) {
                    String display_name = player.getProfile().getName();

                    if (!display_name.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(display_name);
                }

                if (SWRC.getRace() != null) {
                    for (String name : SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList()) {
                        if (!name.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                        builder.suggest(name);
                    }
                }
            }

            return builder.buildFuture();
        }
    }

    private static class ControllerSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRC.getRace() != null) {
                for (String player : SWRC.getRace().rc_clients.keySet()) {

                    if (!player.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(player);
                }
            }

            return builder.buildFuture();
        }
    }

    private static class RacerSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRC.getRace() != null) {
                for (String player : SWRC.getRace().racer_clients.keySet()) {
                    if (SWRC.getRace().rc_clients.containsKey(player)) continue;

                    if (!player.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(player);
                }
            }

            return builder.buildFuture();
        }
    }

    private static class PlayerFileSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRC.minecraftClient.world != null) {
                for (File file : Objects.requireNonNull(FabricLoader.getInstance().getConfigDir().resolve("swrc").resolve("results").toFile().listFiles())) {
                    String name = file.getName().replaceFirst("[.][^.]+$", "");

                    if (!name.endsWith(".positions")) continue;
                    name = name.substring(0, name.length() - ".positions".length());

                    if (!name.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(name);
                }
            }

            return builder.buildFuture();
        }
    }

    public static class TrackFileSuggestor implements SuggestionProvider<FabricClientCommandSource> {
        @Override
        public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {

            if (SWRC.minecraftClient.world != null) {
                for (File file : Objects.requireNonNull(FabricLoader.getInstance().getConfigDir().resolve("swrc").resolve("tracks").toFile().listFiles())) {
                    String name = file.getName().replaceFirst("[.][^.]+$", "");

                    if (!name.toLowerCase().contains(builder.getRemainingLowerCase())) continue;

                    builder.suggest(name);
                }
            }

            return builder.buildFuture();
        }
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("race")
            .executes(this::doRace)
            .then(
                literal("state")
                .then(
                    literal("NONE")
                    .executes(context -> doUpdateRaceState(context, Race.RaceState.NONE))
                )
                .then(
                    literal("RACE")
                    .executes(context -> doUpdateRaceState(context, Race.RaceState.RACE))
                )
                .then(
                    literal("QUALI")
                    .executes(context -> doUpdateRaceState(context, Race.RaceState.QUALI))
                )
            )
            .then(
                literal("quit")
                .executes(this::doQuit)
            )
            .then(
                literal("clients")
                .executes(this::doClients)
            )
            .then(
                literal("tracking")
                .then(
                    argument("controller", StringArgumentType.string())
                    .suggests(new ControllerSuggestor())
                    .then(
                        argument("value", StringArgumentType.string())
                        .suggests(new BoolSuggestor())
                        .executes(this::doTrackingSet)
                    )
                )
            )
            .then(
                literal("exit")
                .executes(this::doExit)
            )
            .then(
                literal("pop_flap")
                .executes(this::doPopFlap)
            )
            .then(
                literal("url")
                .executes(this::doRaceUrl)
            )
            .then(
                literal("dump")
                .executes(this::doRaceDump)
                .then(
                    literal("i_understand_what_i_am_doing")
                    .executes(this::doRaceDumpFr)
                )
            )
            .then(
                literal("player")

                .then(
                    literal("add")
                    .then(
                        argument("name", StringArgumentType.string())
                        .suggests(new PlayerSuggestor())
                        .executes(this::doAddPlayerByName)
                    )
                )
                .then(
                    literal("remove")
                    .then(
                        argument("name", StringArgumentType.string())
                        .suggests(new PlayerSuggestor())
                        .executes(this::doRemovePlayerByName)
                    )
                )
                .then(
                    literal("file")
                    .then(
                        argument("filename", StringArgumentType.string())
                        .suggests(new PlayerFileSuggestor())
                        .then(
                            literal("all")
                            .executes(this::doAddPlayersFromFile)
                        )
                        .then(
                            literal("top")
                            .then(
                                argument("count", IntegerArgumentType.integer(1))
                                .executes(this::doAddPlayersFromFileTop)
                            )
                        )
                    )
                )
                .then(
                    literal("near")
                    .then(
                        argument("range", FloatArgumentType.floatArg(0))
                        .executes(this::doAddPlayersNearby)
                    )
                )
                .then(
                    literal("boat")
                    .then(
                        argument("range", FloatArgumentType.floatArg(0))
                        .executes(this::doAddPlayersNearbyBoat)
                    )
                )
                .then(
                    literal("reorder")
                    .then(
                        literal("randomly")
                        .executes(this::doReorderRandomly)
                    )
                    .then(
                        literal("reversed")
                        .executes(this::doReorderReversed)
                    )
                    .then(
                        literal("zorder")
                        .executes(this::doReorderSortLooking)
                    )
                    .then(
                        literal("file")
                        .then(
                            argument("filename", StringArgumentType.string())
                            .suggests(new PlayerFileSuggestor())
                            .executes(this::doReorderFromFile)
                        )
                    )
                    .then(
                        literal("tofront")
                        .then(
                            argument("player", StringArgumentType.string())
                            .suggests(new PlayerSuggestor())
                            .executes(this::doSendToFront)
                        )
                    )
                    .then(
                        literal("toback")
                        .then(
                            argument("player", StringArgumentType.string())
                            .suggests(new PlayerSuggestor())
                            .executes(this::doSendToBack)
                        )
                    )
                )
                .then(
                    literal("rename")
                    .then(
                        argument("from", StringArgumentType.string())
                        .then(
                            argument("to", StringArgumentType.string())
                            .executes(this::doRenamePlayer)
                        )
                    )
                )
            )
            .then(
                literal("load")
                .then(
                    argument("track_file", StringArgumentType.string())
                    .suggests(new TrackFileSuggestor())
                    .then(
                        argument("race_id", StringArgumentType.string())
                        .then(
                            argument("laps", IntegerArgumentType.integer(1))
                            .then(
                                argument("pits", IntegerArgumentType.integer(0))
                                .executes(this::doLoadNewRace)
                            )
                        )
                    )
                )
            )
            .then(
                literal("export")
                .then(
                    literal("positions")
                    .then(
                        argument("filename", StringArgumentType.string())
                        .executes(this::doExportPositions)
                    )
                )
                .then(
                    literal("flaps")
                    .then(
                        argument("filename", StringArgumentType.string())
                        .executes(this::doExportFlaps)
                    )
                )
            )
            .then(new RaceCountdownTimerCommand().command());
    }

    private int doTrackingSet(CommandContext<FabricClientCommandSource> context) {
        String controller = StringArgumentType.getString(context, "controller");
        boolean value = Boolean.parseBoolean(StringArgumentType.getString(context, "value"));

        if (WebsocketManager.rcSocketAvalible()) {

            C2SControllerTrackingPacket packet = new C2SControllerTrackingPacket();

            packet.controller = controller;
            packet.state = value;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Setting Tracking."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doClients(CommandContext<FabricClientCommandSource> context) {
        if (SWRC.getRace() == null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
            return 0;
        }

        MutableText text = Text.empty();

        text = text.append(ChatFormatter.SWRC_PREFIX());
        text = text.append(Text.literal(" Connected Clients\n"));

        for (Map.Entry<String, S2CUpdatePacket.RCClient> entry : SWRC.getRace().rc_clients.entrySet()) {
            text = text.append(
                    Text.empty()
                    .append(Text.literal("RC ").styled(style -> style.withFormatting(Formatting.RED)))
                    .append(Text.literal(entry.getKey()).styled(style -> style.withFormatting(Formatting.AQUA)))
                    .append(Text.literal(" ("))
                    .append(Text.literal(entry.getValue().version).styled(style -> style.withFormatting(Formatting.AQUA)))
                    .append(Text.literal(") Precise: "))
                    .append(
                        entry.getValue().clock_precise ?
                            Text.literal("YES").styled(style -> style.withFormatting(Formatting.GREEN)) :
                            Text.literal("NO").styled(style -> style.withFormatting(Formatting.RED))
                    )
                    .append(Text.literal(" Tracking: "))
                    .append(
                        entry.getValue().tracking ?
                            Text.literal("YES").styled(style -> style.withFormatting(Formatting.GREEN)) :
                            Text.literal("NO").styled(style -> style.withFormatting(Formatting.RED))
                    )
                    .append(Text.literal("\n"))
            );
        }

        for (Map.Entry<String, S2CUpdatePacket.RacerClient> entry : SWRC.getRace().racer_clients.entrySet()) {
            if (SWRC.getRace().rc_clients.containsKey(entry.getKey())) continue;

            text = text.append(
                    Text.empty()
                            .append(Text.literal("RACER ").styled(style -> style.withFormatting(Formatting.GOLD)))
                            .append(Text.literal(entry.getKey()).styled(style -> style.withFormatting(Formatting.AQUA)))
                            .append(Text.literal(" ("))
                            .append(Text.literal(entry.getValue().version).styled(style -> style.withFormatting(Formatting.AQUA)))
                            .append(Text.literal(")"))
                            .append(Text.literal("\n"))
            );
        }

        context.getSource().sendFeedback(text);

        return Command.SINGLE_SUCCESS;
    }

    private int doRaceDump(CommandContext<FabricClientCommandSource> context) {

        if (WebsocketManager.rcSocketAvalible()) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Click to confirm dumping of race ").append(
                Text.literal("[DUMP]")
                .styled(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/swrc race dump i_understand_what_i_am_doing"))
                    .withHoverEvent(new HoverEvent.ShowText(Text.literal("I am sure I would like to dump the race")))
                )

            ));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doRaceDumpFr(CommandContext<FabricClientCommandSource> context) {

        if (WebsocketManager.rcSocketAvalible()) {

            C2SEndRacePacket packet = new C2SEndRacePacket();

            packet.dump = true;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Dumping Race."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doRaceUrl(CommandContext<FabricClientCommandSource> context) {
        if (SWRC.getRace() == null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
            return 0;
        }

        // probably move this
        String url = "https://swrc.cloudmc.uk/races/" + SWRC.getRace().getId();

        context.getSource().sendFeedback(
                Text.empty()
                        .append(ChatFormatter.SWRC_PREFIX())
                        .append(Text.literal(" "))
                        .append(Text.literal(url).styled(style -> style
                                .withFormatting(Formatting.UNDERLINE)
                                .withFormatting(Formatting.BLUE)
                                .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal(url).styled(style1 -> style1.withFormatting(Formatting.BLUE))))
                        ))
        );

        return Command.SINGLE_SUCCESS;
    }

    private int doSendToBack(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");

        if (WebsocketManager.rcSocketAvalible()) {

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            List<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            if (names.remove(player)) names.add(player);

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doReorderFromFile(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        if (WebsocketManager.rcSocketAvalible()) {
            String filename = target + ".positions.json";

            try {
                String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename));

                Type arraylist = new TypeToken<ArrayList<String>>() {
                }.getType();

                ArrayList<String> names = (new Gson().fromJson(content, arraylist));

                C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

                WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);


            } catch (IOException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to load from config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            } catch (JsonParseException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to parse config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            }

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doSendToFront(CommandContext<FabricClientCommandSource> context) {
        String player = StringArgumentType.getString(context, "player");

        if (WebsocketManager.rcSocketAvalible()) {

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            ArrayList<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            if (names.remove(player)) names.add(0, player);

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doRenamePlayer(CommandContext<FabricClientCommandSource> context) {

        String from_name = StringArgumentType.getString(context, "from");
        String to_name = StringArgumentType.getString(context, "to");

        if (WebsocketManager.rcSocketAvalible()) {

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            if (!SWRC.getRace().isRacing(from_name)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(from_name + " is not racing"));
                return 0;
            }

            if (SWRC.getRace().isRacing(to_name)) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(to_name + " is already racing"));
                return 0;
            }

            List<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            int pos = names.indexOf(from_name);

            if (pos == -1) return 0;

            names.remove(pos);

            C2SModifyRacerPacket remove = new C2SModifyRacerPacket();

            remove.racer_name = from_name;
            remove.action = C2SModifyRacerPacket.ModifyRacerPacketAction.REMOVE;

            C2SModifyRacerPacket add = new C2SModifyRacerPacket();

            add.racer_name = to_name;
            add.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;

            names.add(pos, to_name);

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(remove);
            WebsocketManager.rcWebsocketConnection.sendPacket(add);
            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doReorderRandomly(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            List<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            Collections.shuffle(names);

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doReorderReversed(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            List<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            Collections.reverse(names);

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doReorderSortLooking(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {
            assert SWRC.minecraftClient.player != null;

            if (SWRC.getRace() == null) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as there isn't a race active"));
                context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc race load ", "with a race id and a track file"));
                return 0;
            }

            List<String> names = new ArrayList<>(SWRC.getRace().raceLeaderboardPositions.stream().map(raceLeaderboardPosition -> raceLeaderboardPosition.player_name).toList());

            names.sort(Comparator.comparingDouble(name -> {

                AbstractClientPlayerEntity player = SWRC.minecraftClient.world.getPlayers()
                        .stream()
                        .filter(clientPlayerEntity -> clientPlayerEntity.getName().getString().equals(name))
                        .findFirst()
                        .orElse(null);

                if (player != null) {
                    return SWRC.minecraftClient.player.getRotationVec(1f).dotProduct(
                             player.getPos().subtract(SWRC.minecraftClient.player.getPos())
                     );
                }

                return Double.MAX_VALUE;
            }));

            C2SReorderPacket reorderPacket = new C2SReorderPacket(names);

            WebsocketManager.rcWebsocketConnection.sendPacket(reorderPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doPopFlap(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {

            C2SPopFlapPacket popFlapPacket = new C2SPopFlapPacket();

            WebsocketManager.rcWebsocketConnection.sendPacket(popFlapPacket);

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doExportFlaps(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        String filename = target + ".flaps.json";

        if (SWRC.getRace() == null) return 0;
        if (SWRC.getRace().raceLeaderboardPositions == null) return 0;

        ArrayList<String[]> names = new ArrayList<>();

        SWRC.getRace().raceLeaderboardPositions.forEach(raceLeaderboardPosition -> {
            names.add(new String[]{raceLeaderboardPosition.player_name, String.valueOf(raceLeaderboardPosition.flap)});
        });

        Path path = FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename);

        try {
            Files.writeString(path, new Gson().toJson(names));
        } catch (IOException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to save to config/swrc/results/%s - %s", filename, e.getMessage())));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Saved to config/swrc/results/%s", filename)));
        context.getSource().sendFeedback(Text.literal("Click to open file").styled(
                style -> style
                        .withFormatting(Formatting.GRAY)
                        .withFormatting(Formatting.UNDERLINE)
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open " + FabricLoader.getInstance().getGameDir().relativize(path))))
                        .withClickEvent(new ClickEvent.OpenFile(path))
        ));

        return Command.SINGLE_SUCCESS;
    }

    private int doExportPositions(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        String filename = target + ".positions.json";

        if (SWRC.getRace() == null) return 0;
        if (SWRC.getRace().raceLeaderboardPositions == null) return 0;

        ArrayList<String> names = new ArrayList<>();

        SWRC.getRace().raceLeaderboardPositions.forEach(raceLeaderboardPosition -> {
            names.add(raceLeaderboardPosition.player_name);
        });

        Path path = FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename);

        try {
            Files.writeString(path, new Gson().toJson(names));
        } catch (IOException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to save to config/swrc/results/%s - %s", filename, e.getMessage())));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Saved to config/swrc/results/%s", filename)));
        context.getSource().sendFeedback(Text.literal("Click to open file").styled(
                style -> style
                        .withFormatting(Formatting.GRAY)
                        .withFormatting(Formatting.UNDERLINE)
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("Open " + FabricLoader.getInstance().getGameDir().relativize(path))))
                        .withClickEvent(new ClickEvent.OpenFile(path))
        ));

        return Command.SINGLE_SUCCESS;
    }

    private int doLoadNewRace(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {
            String target = StringArgumentType.getString(context, "track_file");
            String id = StringArgumentType.getString(context, "race_id");
            int laps = IntegerArgumentType.getInteger(context, "laps");
            int pits = IntegerArgumentType.getInteger(context, "pits");

            String filename = target + ".json";

            try {
                String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("tracks").resolve(filename));

                Track track = Track.deserialize(content);

                C2SPushTrackPacket packet = new C2SPushTrackPacket();
                packet.track = track;
                packet.race_id = id;
                packet.total_laps = laps;
                packet.total_pits = pits;

                WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            } catch (IOException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to load from config/swrc/tracks/%s - %s", filename, e.getMessage())));
                return 0;
            } catch (JsonParseException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to parse config/swrc/tracks/%s - %s", filename, e.getMessage())));
                return 0;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Successfully loaded from config/swrc/tracks/%s", filename)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doAddPlayersNearbyBoat(CommandContext<FabricClientCommandSource> context) {
        float range = FloatArgumentType.getFloat(context, "range");

        assert SWRC.minecraftClient.world != null;
        assert SWRC.minecraftClient.player != null;

        if (WebsocketManager.rcSocketAvalible()) {
            int added = 0;
            for (AbstractClientPlayerEntity worldPlayer : SWRC.minecraftClient.world.getPlayers()) {
                if (!(worldPlayer.getVehicle() instanceof BoatEntity) && !(worldPlayer.getVehicle() instanceof ChestBoatEntity))
                    continue;

                if (worldPlayer.getPos().distanceTo(SWRC.minecraftClient.player.getPos()) <= range) {
                    C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

                    packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
                    packet.racer_name = worldPlayer.getName().getString();

                    WebsocketManager.rcWebsocketConnection.sendPacket(packet);
                    added += 1;
                }
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Added %s nearby players in boats", added)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doAddPlayersNearby(CommandContext<FabricClientCommandSource> context) {
        float range = FloatArgumentType.getFloat(context, "range");

        assert SWRC.minecraftClient.player != null;

        if (WebsocketManager.rcSocketAvalible()) {
            int added = 0;
            assert SWRC.minecraftClient.world != null;
            for (AbstractClientPlayerEntity worldPlayer : SWRC.minecraftClient.world.getPlayers()) {
                if (worldPlayer.getName().equals(SWRC.minecraftClient.player.getName())) continue;

                if (worldPlayer.getPos().distanceTo(SWRC.minecraftClient.player.getPos()) <= range) {
                    C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

                    packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
                    packet.racer_name = worldPlayer.getName().getString();

                    WebsocketManager.rcWebsocketConnection.sendPacket(packet);
                    added += 1;
                }
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Added %s nearby players", added)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doAddPlayersFromFileTop(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");
        int count = IntegerArgumentType.getInteger(context, "count");

        if (WebsocketManager.rcSocketAvalible()) {
            String filename = target + ".positions.json";

            try {
                String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename));

                Type arraylist = new TypeToken<ArrayList<String>>() {
                }.getType();

                ArrayList<String> names = (new Gson().fromJson(content, arraylist));

                for (String name : names.stream().limit(count).toList()) {

                    C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

                    packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
                    packet.racer_name = name;

                    WebsocketManager.rcWebsocketConnection.sendPacket(packet);
                    PlayerNameValidator.validateName(name);
                }
            } catch (IOException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to load from config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            } catch (JsonParseException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to parse config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            }

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doAddPlayersFromFile(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        if (WebsocketManager.rcSocketAvalible()) {
            String filename = target + ".positions.json";

            try {
                String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename));

                Type arraylist = new TypeToken<ArrayList<String>>() {
                }.getType();

                ArrayList<String> names = (new Gson().fromJson(content, arraylist));

                for (String name : names) {

                    C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

                    packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
                    packet.racer_name = name;

                    WebsocketManager.rcWebsocketConnection.sendPacket(packet);
                    PlayerNameValidator.validateName(name);
                }


            } catch (IOException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to load from config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            } catch (JsonParseException e) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to parse config/swrc/results/%s - %s", filename, e.getMessage())));
                return 0;
            }

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
         return 0;
    }

    private int doRemovePlayerByName(CommandContext<FabricClientCommandSource> context) {
        String player_name = StringArgumentType.getString(context, "name");

        if (WebsocketManager.rcSocketAvalible()) {
            C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

            packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.REMOVE;
            packet.racer_name = player_name;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Removing %s", player_name)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doAddPlayerByName(CommandContext<FabricClientCommandSource> context) {
        String player_name = StringArgumentType.getString(context, "name");

        if (WebsocketManager.rcSocketAvalible()) {
            C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

            packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
            packet.racer_name = player_name;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            PlayerNameValidator.validateName(player_name);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Adding %s", player_name)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doExit(CommandContext<FabricClientCommandSource> context) {

        if (WebsocketManager.rcSocketAvalible()) {

            C2SEndRacePacket packet = new C2SEndRacePacket();

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Ending Race."));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }

    private int doQuit(CommandContext<FabricClientCommandSource> context) {
        SWRC.setRace(null);
        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Quit active race"));
        return Command.SINGLE_SUCCESS;
    }

    private int doRace(CommandContext<FabricClientCommandSource> context) {
        Race activeRace = SWRC.getRace();

        if (activeRace != null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Active race (stage=%s,#checkpoints=%s)", activeRace.getRaceState(), activeRace.numCheckpoints())));
        } else {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("No active race present"));
        }

        return Command.SINGLE_SUCCESS;
    }

    public static int doUpdateRaceState(CommandContext<FabricClientCommandSource> context, Race.RaceState state) {

        if (WebsocketManager.rcSocketAvalible()) {
            Race activeRace = SWRC.getRace();

            if (activeRace != null) {
                C2SRaceState packet = new C2SRaceState();

                packet.state = state;

                WebsocketManager.rcWebsocketConnection.sendPacket(packet);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Updating state to %s", state)));

                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("No active race present"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed: RC Socket Disconnected"));
        context.getSource().sendFeedback(ChatFormatter.HINT_COMMAND("try", "/swrc server sessions", "and connecting"));
        return 0;
    }
}
