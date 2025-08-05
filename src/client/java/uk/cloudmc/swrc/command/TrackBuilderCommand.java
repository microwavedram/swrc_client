package uk.cloudmc.swrc.command;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.CommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.track.Checkpoint;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.track.TrackBuilder;
import uk.cloudmc.swrc.track.Trap;
import uk.cloudmc.swrc.util.ChatFormatter;
import uk.cloudmc.swrc.util.NYTAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class TrackBuilderCommand implements CommandNodeProvider {
    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> command() {
        return literal("track_builder")
            .executes(this::doTrackBuilderData)
            .then(
                literal("new")
                .then(
                    argument("id", StringArgumentType.string())
                    .executes(this::doNewTrackBuilder)
                )
            )
            .then(
                literal("trap")
                .executes(this::doNewTrap)
                .then(
                    literal("done")
                    .executes(this::finishTrapBuilder)
                )
            )
            .then(
                literal("checkpoint")
                .executes(this::doNewCheckpoint)
                .then(
                    literal("left")
                    .executes(this::doCheckpointLeft)
                )
                .then(
                    literal("right")
                    .executes(this::doCheckpointRight)
                )
                .then(
                    literal("pit")
                    .then(
                        literal("trigger")
                        .executes(this::doNewPit)
                    )
                    .then(
                        literal("enter")
                        .executes(this::doNewPitEnter)
                    )
                )
                .then(
                    literal("trap")
                    .then(
                        literal("enter")
                        .executes(this::newTrapEnter)
                    )
                    .then(
                        literal("exit")
                        .executes(this::newTrapExit)
                    )
                )
                .then(
                    literal("done")
                    .executes(this::doCheckpointDone)
                )
            )
            .then(
                literal("meta")
                .executes(this::doMetaInfo)
                .then(
                    argument("meta", StringArgumentType.string())
                    .suggests(
                            this::suggestMeta
                    )
                    .executes(this::queryMeta)
                    .then(
                        argument("value", StringArgumentType.string())
                        .executes(this::setMeta)
                    )
                )
            )
            .then(
                literal("save")
                .then(
                    argument("filename", StringArgumentType.string())
                    .suggests(new RaceCommand.TrackFileSuggestor())
                    .executes(context -> this.doSaveTrack(context, false))
                    .then(
                        argument("wordle", StringArgumentType.string())
                        .executes(context -> {
                            String attempt = StringArgumentType.getString(context, "wordle");

                            LocalDateTime dateTime = LocalDateTime.now();

                            String answer = NYTAPI.getWordleAnswer(dateTime.getYear(), dateTime.getMonth().getValue(), dateTime.getDayOfMonth());

                            if (answer != null && answer.equals(attempt)) {
                                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Correct."));
                                return this.doSaveTrack(context, true);
                            }

                            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Incorrect."));

                            return 0;
                        })
                    )
                )
            )
            .then(
                literal("load")
                .then(
                    argument("filename", StringArgumentType.string())
                    .suggests(new RaceCommand.TrackFileSuggestor())
                    .executes(this::doLoadTrack)
                )
            )
            .then(
                literal("exit")
                .executes(this::doExitTrackBuilder));
    }

    private int doExitTrackBuilder(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();

        if (trackBuilder != null) {
            SWRC.setTrackBuilder(null);

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Exited TrackBuilder"));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doLoadTrack(CommandContext<FabricClientCommandSource> context) {
        String target = StringArgumentType.getString(context, "filename");

        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder == null) {
            String filename = target + ".json";

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Reading from config/swrc/tracks/%s", filename)));

            try {
                String content = Files.readString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("tracks").resolve(filename));

                Track track = Track.deserialize(content);

                TrackBuilder newTrackBuilder = new TrackBuilder(track.id);
                newTrackBuilder.setName(track.name);
                newTrackBuilder.setMinimumLapTime(track.minimumLapTime);

                // force it to not give the wordle prompt
                newTrackBuilder.setPitCountsAsLap(track.pitCountsAsLap);

                track.checkpoints.forEach(newTrackBuilder::addCheckpoint);
                track.traps.forEach(newTrackBuilder::addTrap);

                if (track.pit != null) {
                    newTrackBuilder.setPit(track.pit);
                }

                if (track.pit_enter != null) {
                    newTrackBuilder.setPitEnter(track.pit_enter);
                }

                SWRC.setTrackBuilder(newTrackBuilder);

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

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed to create a TrackBuilder as one is already active"));
        return 0;
    }

    private int doSaveTrack(CommandContext<FabricClientCommandSource> context, boolean force) {
        String target = StringArgumentType.getString(context, "filename");

        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.numberOfCheckpoints() > 0) {
                String filename = target + ".json";

                if (!trackBuilder.hasTouchedPitCountsAsLap() && !force) {
                    new Thread(() -> {
                        MutableText mutableText = ChatFormatter.GENERIC_MESSAGE_PREFIX()
                                .append(Text.literal("You have not explicitly set pit counts as lap.\n").styled(style -> style.withFormatting(Formatting.DARK_RED)))
                                .append(Text.literal("Please click on today's wordle answer to confirm what you are about to do.\n").styled(style -> style.withFormatting(Formatting.RED)));

                        LocalDateTime date = LocalDateTime.now();

                        for (int i = 0; i < 5; i++) {
                            LocalDateTime dateTime = date.minusDays(i);

                            String answer = NYTAPI.getWordleAnswer(dateTime.getYear(), dateTime.getMonth().getValue(), dateTime.getDayOfMonth());

                            mutableText = mutableText.append(
                                    Text.literal(String.format("[%s]\n", answer)).styled(style -> style.withFormatting(Formatting.GOLD)
                                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Submit")))
                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/swrc track_builder save " + target + " " + answer))
                                    )
                            );
                        }

                        mutableText = mutableText.append(Text.literal("Please choose wisely").styled(style -> style.withFormatting(Formatting.WHITE)));

                        context.getSource().sendFeedback(
                                mutableText
                        );
                    }).start();
                    return 0;
                }

                Track track = trackBuilder.finish();

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Saving to config/swrc/tracks/%s", filename)));

                try {
                    Files.writeString(FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("tracks").resolve(filename), track.serialize());
                } catch (IOException e) {
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Failed to save to config/swrc/tracks/%s - %s", filename, e.getMessage())));
                    return 0;
                }

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("Successfully saved to config/swrc/tracks/%s", filename)));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("You need at least 1 checkpoint"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int setMeta(CommandContext<FabricClientCommandSource> context) {
        String meta_category = StringArgumentType.getString(context, "meta");
        String value = StringArgumentType.getString(context, "value");

        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            switch (meta_category) {
                case "min_lap_time":
                    trackBuilder.setMinimumLapTime(Long.parseLong(value));
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Set minimumLapTime to: " + value));
                    break;
                case "name":
                    trackBuilder.setName(value);
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Set name to: " + value));
                    break;
                case "pit_counts_as_lap":
                    trackBuilder.setPitCountsAsLap(Boolean.parseBoolean(value));
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Set pit_counts_as_lap to: " + value));
                    break;
                default:
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(meta_category + " does not exist"));
                    return 0;
            }

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int queryMeta(CommandContext<FabricClientCommandSource> context) {
        String meta_category = StringArgumentType.getString(context, "meta");

        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            switch (meta_category) {
                case "min_lap_time":
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("minimumLapTime is currently: " + trackBuilder.getMinimumLapTime()));
                    break;
                case "name":
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("name is currently: " + trackBuilder.getName()));
                    break;
                case "pit_counts_as_lap":
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("pit_counts_as_lap is currently: " + trackBuilder.getPitCountsAsLap()));
                    break;
                default:
                    context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(meta_category + " does not exist"));
                    return 0;
            }

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private CompletableFuture<Suggestions> suggestMeta(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        return CommandSource.suggestMatching(
                new String[] {"min_lap_time", "name", "pit_counts_as_lap"},
                suggestionsBuilder
        );
    }

    private int doMetaInfo(CommandContext<FabricClientCommandSource> context) {

        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("id = " + trackBuilder.id));
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("name = " + trackBuilder.getName()));
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("minLapTime = " + trackBuilder.getMinimumLapTime()));
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("pit_counts_as_lap = " + trackBuilder.getMinimumLapTime()));

            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("No TrackBuilder has been created"));
        return 0;
    }

    private int doCheckpointDone(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.canFinalize()) {
                Checkpoint new_checkpoint = trackBuilder.checkpointBuilder.finalizeCheckpoint();

                trackBuilder.addCheckpoint(new_checkpoint);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized checkpoint"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as checkpoint is invalid"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int newTrapExit(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.canFinalize()) {
                Checkpoint new_checkpoint = trackBuilder.checkpointBuilder.finalizeCheckpoint();

                trackBuilder.trapBuilder.setExit(new_checkpoint);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized trap exit"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as trap exit is invalid"));
            return 0;

        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int newTrapEnter(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.canFinalize()) {
                Checkpoint new_checkpoint = trackBuilder.checkpointBuilder.finalizeCheckpoint();

                trackBuilder.trapBuilder.setEnter(new_checkpoint);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized trap entrance"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as trap entrance is invalid"));
            return 0;

        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doNewPitEnter(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.canFinalize()) {
                Checkpoint new_checkpoint = trackBuilder.checkpointBuilder.finalizeCheckpoint();

                trackBuilder.setPitEnter(new_checkpoint);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized pit entrance"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as pit entrance is invalid"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doNewPit(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.canFinalize()) {
                Checkpoint new_checkpoint = trackBuilder.checkpointBuilder.finalizeCheckpoint();

                trackBuilder.setPit(new_checkpoint);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized pit"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as pit is invalid"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doCheckpointRight(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();

        assert SWRC.minecraftClient.player != null;

        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.hasActiveCheckpoint()) {
                Vec3d position = SWRC.minecraftClient.player.getPos();

                trackBuilder.checkpointBuilder.setRight(position);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Set right at " + position.toString()));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no checkpoint has been created"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doCheckpointLeft(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();

        assert SWRC.minecraftClient.player != null;

        if (trackBuilder != null) {
            if (trackBuilder.checkpointBuilder.hasActiveCheckpoint()) {
                Vec3d position = SWRC.minecraftClient.player.getPos();

                trackBuilder.checkpointBuilder.setLeft(position);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Set left at " + position.toString()));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no checkpoint has been created"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doNewCheckpoint(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            boolean successful = trackBuilder.checkpointBuilder.newCheckpoint(checkpoint -> {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Added previous checkpoint"));
                trackBuilder.addCheckpoint(checkpoint);
            });

            if (successful) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully created new checkpoint"));
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Current Checkpoint is not valid and can not be finalized"));
                return 0;
            }
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int finishTrapBuilder(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            if (trackBuilder.trapBuilder.canFinalize()) {
                Trap new_trap = trackBuilder.trapBuilder.finalizeTrap();

                trackBuilder.addTrap(new_trap);

                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully finalized trap"));
                return Command.SINGLE_SUCCESS;
            }

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as trap is invalid"));
            return 0;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doNewTrap(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            boolean successful = trackBuilder.trapBuilder.newTrap(trap -> {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Added previous trap"));
                trackBuilder.addTrap(trap);
            });

            if (successful) {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Successfully created new trap"));
                return Command.SINGLE_SUCCESS;
            } else {
                context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Current Trap is not valid and can not be finalized"));
                return 0;
            }
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed as no TrackBuilder has been created"));
        return 0;
    }

    private int doNewTrackBuilder(CommandContext<FabricClientCommandSource> context) {
        String id = StringArgumentType.getString(context, "id");

        if (SWRC.getTrackBuilder() == null) {
            SWRC.setTrackBuilder(new TrackBuilder(id));

            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Created TrackBuilder"));
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("Failed to create a TrackBuilder as one is already active"));
        return 0;
    }

    private int doTrackBuilderData(CommandContext<FabricClientCommandSource> context) {
        TrackBuilder trackBuilder = SWRC.getTrackBuilder();
        if (trackBuilder != null) {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE(String.format("TrackBuilder active (id=%s, #checkpoints=%s)", trackBuilder.id, trackBuilder.numberOfCheckpoints())));
        } else {
            context.getSource().sendFeedback(ChatFormatter.GENERIC_MESSAGE("TrackBuilder not active"));
        }

        return Command.SINGLE_SUCCESS;
    }
}
