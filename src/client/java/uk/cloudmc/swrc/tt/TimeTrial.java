package uk.cloudmc.swrc.tt;

import com.google.common.collect.ImmutableMap;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.net.packets.S2CUpdatePacket;
import uk.cloudmc.swrc.track.Checkpoint;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.util.ChatFormatter;
import uk.cloudmc.swrc.util.DeltaFormat;
import uk.cloudmc.swrc.util.NTPTimeSync;
import uk.cloudmc.swrc.util.Snapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TimeTrial {
    private final Track track;

    private boolean active = false;
    private int next_checkpoint_index = 0;
    private long lap_begin_time = -1;
    private Map<Integer, Long> split_times = new HashMap<>();

    private long flap = -1;
    private Map<Integer, Long> flap_split_times = null;
    private long lastDelta = -1;

    public TimeTrial(Track track) {
        this.track = track;
    }

    public void update() {
        if (SWRC.minecraftClient.world == null || !this.active) return;
        assert SWRC.minecraftClient.player != null;

        long timestamp = NTPTimeSync.getTrueTime();

        ArrayList<Snapshot> snapshots = new ArrayList<>();
        snapshots.add(new Snapshot(
                SWRC.minecraftClient.player.getName().getString(),
                SWRC.minecraftClient.player.getPos(),
                SWRC.minecraftClient.player.getVelocity()
        ));

        Checkpoint next_checkpoint = track.checkpoints.get(next_checkpoint_index);

        ArrayList<Snapshot> line_crosses = next_checkpoint.getLineCrosses(snapshots);

        Optional<Snapshot> cross = line_crosses.stream().findFirst();

        if (cross.isPresent()) {

            if (lap_begin_time != -1) {
                split_times.put(next_checkpoint_index, timestamp - lap_begin_time);

                if (flap_split_times != null && flap_split_times.containsKey(next_checkpoint_index) && next_checkpoint_index != 0) {
                    long split_delta = (timestamp - lap_begin_time) - flap_split_times.get(next_checkpoint_index);

                    SWRC.minecraftClient.inGameHud.getChatHud().addMessage(
                            ChatFormatter.GENERIC_MESSAGE(String.format("Checkpoint %s: ", next_checkpoint_index))
                                    .append(DeltaFormat.formatLapDelta(split_delta, 4000))
                    );

                    lastDelta = split_delta;
                }
            }

            next_checkpoint_index++;

            if (next_checkpoint_index >= track.checkpoints.size()) {
                next_checkpoint_index = 0;
            }

            if (next_checkpoint_index == 1) {

                if (lap_begin_time != -1) {
                    long lap_time_milis = timestamp - lap_begin_time;

                    handleLap(lap_time_milis, split_times);
                }

                lap_begin_time = timestamp;
                split_times.clear();
            }
        }
    }

    private void handleLap(long lap_time_milis, Map<Integer, Long> split_times) {

        boolean isFlap = false;

        if (flap != -1) {
            long lap_time_delta = lap_time_milis - flap;

            if (lap_time_milis < flap) {
                isFlap = true;
            }

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(
                    ChatFormatter.GENERIC_MESSAGE(String.format("Finished lap in %s ", DeltaFormat.formatMillis(lap_time_milis)))
                            .append(DeltaFormat.formatLapDelta(lap_time_delta, 4000))
            );
        } else {
            isFlap = true;

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(
                    ChatFormatter.GENERIC_MESSAGE(String.format("Finished lap in %s ", DeltaFormat.formatMillis(lap_time_milis)))
            );
        }

        if (isFlap) {
            flap = lap_time_milis;
            flap_split_times = ImmutableMap.copyOf(split_times);

            SWRC.bestLap.show(new S2CUpdatePacket.Flap(
                    SWRC.minecraftClient.player.getName().getString(),
                    0,
                    flap,
                    0
            ));
        }
    }

    public void start() {
        lap_begin_time = -1;
        active = true;
    }

    public void stop() {
        lap_begin_time = -1;
        active = false;
    }

    public void reset() {
        reset();
        flap = -1;
        flap_split_times = null;
    }

    public long getLapBeginTime() {
        return lap_begin_time;
    }

    public boolean isActive() {
        return active;
    }

    public long getLastDelta() {
        return lastDelta;
    }
}
