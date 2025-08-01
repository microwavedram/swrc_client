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
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.ChestBoatEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.WebsocketManager;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class
RaceCommand implements CommandNodeProvider {

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
                literal("url")
                .executes(this::doUrl)
            )
            .then(
                literal("quit")
                .executes(this::doQuit)
            )
            .then(
                literal("exit")
                .executes(this::doExit)
            )
            .then(
                literal("player")
                .then(
                    literal("add")
                    .then(
                        argument("name", StringArgumentType.string())
                        .executes(this::doAddPlayerByName)
                    )
                )
                .then(
                    literal("remove")
                    .then(
                        argument("name", StringArgumentType.string())
                        .executes(this::doRemovePlayerByName)
                    )
                )
                .then(
                    literal("file")
                    .then(
                        argument("filename", StringArgumentType.string())
                        .executes(this::addPlayersFromFile)
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
            )
            .then(
                literal("load")
                .then(
                    argument("filename", StringArgumentType.string())
                    .then(
                        argument("id", StringArgumentType.string())
                        .then(
                            argument("laps", IntegerArgumentType.integer(1))
                            .then(
                                argument("pits", IntegerArgumentType.integer(1))
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
                    literal("quali")
                        .then(
                            argument("filename", StringArgumentType.string())
                            .executes(this::doExportQuali)
                        )
                )
            )
            .then(new RaceTimerCommand().command());
    }

    private int doUrl(CommandContext<FabricClientCommandSource> context) {
        if (SWRC.getRace() != null) {
            String host = WebsocketManager.swrcWebsocketConnection.getURI().getHost();

            String link = "https://" + host + "/races/" + SWRC.getRace().getId();

            context.getSource().sendFeedback(
                ChatFormatter.GENERIC_MESSAGE("URL: ")
                    .append(Text.literal(link).styled(style ->
                        style
                            .withFormatting(Formatting.GOLD)
                            .withFormatting(Formatting.UNDERLINE)
                            .withClickEvent(new ClickEvent.OpenUrl(URI.create(link)))
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal(
                                    link
                            ).styled(style1 -> style1.withFormatting(Formatting.UNDERLINE))))
                    ))
            );
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("No race active"));
        return 0;
    }

    private int doExportQuali(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        String filename = target + ".json";

        if (SWRC.getRace() == null) return 0;
        if (SWRC.getRace().raceLeaderboardPositions == null) return 0;

        ArrayList<String[]> names = new ArrayList<>();

        SWRC.getRace().raceLeaderboardPositions.forEach(raceLeaderboardPosition -> {
            names.add(new String[]{raceLeaderboardPosition.player_name, String.valueOf(raceLeaderboardPosition.flap)});
        });

        try {
            Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename), new Gson().toJson(names));
        } catch (IOException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to save to config/swrc/results/%s - %s", filename, e.getMessage())));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Saved to config/swrc/results/%s", filename)));

        return Command.SINGLE_SUCCESS;
    }

    private int doExportPositions(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        String filename = target + ".json";

        if (SWRC.getRace() == null) return 0;
        if (SWRC.getRace().raceLeaderboardPositions == null) return 0;

        ArrayList<String> names = new ArrayList<>();

        SWRC.getRace().raceLeaderboardPositions.forEach(raceLeaderboardPosition -> {
            names.add(raceLeaderboardPosition.player_name);
        });

        try {
            Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("results").resolve(filename), new Gson().toJson(names));
        } catch (IOException e) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to save to config/swrc/results/%s - %s", filename, e.getMessage())));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Saved to config/swrc/results/%s", filename)));

        return Command.SINGLE_SUCCESS;
    }

    private int doLoadNewRace(CommandContext<FabricClientCommandSource> context) {
        if (WebsocketManager.rcSocketAvalible()) {
            String target = StringArgumentType.getString(context, "filename");
            String id = StringArgumentType.getString(context, "id");
            int laps = IntegerArgumentType.getInteger(context, "laps");
            int pits = IntegerArgumentType.getInteger(context, "pits");

            String filename = target + ".json";

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Reading from config/swrc/tracks/%s", filename)));

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
        } else {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Websocket disconnected"));
            return 0;
        }
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

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Websocket disconnected"));
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

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Websocket disconnected"));
        return 0;
    }

    private int addPlayersFromFile(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");
        String filename = target + ".json";

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Reading from config/swrc/results/%s", filename)));

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

    private int doRemovePlayerByName(CommandContext<FabricClientCommandSource> context) {
        String player_name = StringArgumentType.getString(context, "name");

        if (WebsocketManager.rcSocketAvalible()) {
            C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

            packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.REMOVE;
            packet.racer_name = player_name;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Send Request to remove %s", player_name)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Websocket disconnected"));
        return 0;
    }

    private int doAddPlayerByName(CommandContext<FabricClientCommandSource> context) {
        String player_name = StringArgumentType.getString(context, "name");

        if (WebsocketManager.rcSocketAvalible()) {
            C2SModifyRacerPacket packet = new C2SModifyRacerPacket();

            packet.action = C2SModifyRacerPacket.ModifyRacerPacketAction.ADD;
            packet.racer_name = player_name;

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Send Request to add %s", player_name)));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Websocket disconnected"));
        return 0;
    }

    private int doExit(CommandContext<FabricClientCommandSource> context) {

        if (WebsocketManager.rcSocketAvalible()) {

            C2SEndRacePacket packet = new C2SEndRacePacket();

            WebsocketManager.rcWebsocketConnection.sendPacket(packet);

            return Command.SINGLE_SUCCESS;
        }

        return 0;
    }

    private int doQuit(CommandContext<FabricClientCommandSource> context) {
        SWRC.setRace(null);
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
        Race activeRace = SWRC.getRace();

        if (activeRace != null) {
            if (WebsocketManager.rcSocketAvalible()) {
                C2SRaceState packet = new C2SRaceState();

                packet.state = state;

                WebsocketManager.rcWebsocketConnection.sendPacket(packet);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Sending request to update state to %s", state)));

                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("RC Socket not available"));

            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("No active race present"));

        return 0;
    }
}
