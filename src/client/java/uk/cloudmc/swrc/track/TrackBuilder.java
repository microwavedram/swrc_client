package uk.cloudmc.swrc.track;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class TrackBuilder {

    public static class CheckpointBuilder {
        public interface FinishedCheckpointHandler {
            void handleCheckpoint(Checkpoint checkpoint);
        }

        private Checkpoint activeCheckpoint;

        public boolean newCheckpoint(FinishedCheckpointHandler handler) {
            if (hasActiveCheckpoint()) {
                if (!canFinalize()) {
                    return false;
                }

                handler.handleCheckpoint(activeCheckpoint);
            }

            activeCheckpoint = new Checkpoint();

            return true;
        }

        public boolean hasActiveCheckpoint() {
            return activeCheckpoint != null;
        }

        public boolean canFinalize() {
            if (activeCheckpoint == null) return false;
            return activeCheckpoint.isValid();
        }

        public void setLeft(Vec3d position) {
            if (hasActiveCheckpoint()) {
                activeCheckpoint.setLeft(position);
            }
        }

        public void setRight(Vec3d position) {
            if (hasActiveCheckpoint()) {
                activeCheckpoint.setRight(position);
            }
        }

        public Checkpoint finalizeCheckpoint() {
            Checkpoint finished_checkpoint = activeCheckpoint;

            activeCheckpoint = null;

            return finished_checkpoint;
        }

        public Checkpoint getActiveCheckpoint() {
            return activeCheckpoint;
        }
    }

    public static class TrapBuilder {
        public interface FinishedTrapHandler {
            void handleTrap(Trap checkpoint);
        }

        private Trap activeTrap;

        public boolean newTrap(FinishedTrapHandler handler) {
            if (hasActiveTrap()) {
                if (!canFinalize()) {
                    return false;
                }

                handler.handleTrap(activeTrap);
            }

            activeTrap = new Trap();

            return true;
        }

        public boolean hasActiveTrap() {
            return activeTrap != null;
        }

        public boolean canFinalize() {
            if (activeTrap == null) return false;
            return activeTrap.isValid();
        }

        public void setEnter(Checkpoint checkpoint) {
            activeTrap.enter = checkpoint;
        }

        public void setExit(Checkpoint checkpoint) {
            activeTrap.exit = checkpoint;
        }

        public Trap finalizeTrap() {
            Trap finished_trap = activeTrap;

            activeTrap = null;

            return finished_trap;
        }

        public Trap getActiveTrap() {
            return activeTrap;
        }
    }

    public final CheckpointBuilder checkpointBuilder = new CheckpointBuilder();
    public final TrapBuilder trapBuilder = new TrapBuilder();

    public final String id;

    private final ArrayList<Checkpoint> checkpoints = new ArrayList<>();
    private final ArrayList<Trap> traps = new ArrayList<>();
    private String name = "Unnamed";
    private long minimumLapTime = 0;

    private boolean hasTouchedPitCountsAsLap = false;

    private boolean pitCountsAsLap = false;

    private Checkpoint pit;
    private Checkpoint pitEnter;

    public TrackBuilder(String id) {
        this.id = id;
    }

    public void addCheckpoint(Checkpoint checkpoint) {
        checkpoints.add(checkpoint);
    }

    public void addTrap(Trap trap) {
        traps.add(trap);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getMinimumLapTime() {
        return minimumLapTime;
    }

    public void setPitCountsAsLap(boolean pitCountsAsLap) {
        this.pitCountsAsLap = pitCountsAsLap;
        this.hasTouchedPitCountsAsLap = true;
    }

    public boolean hasTouchedPitCountsAsLap() {
        return hasTouchedPitCountsAsLap;
    }

    public boolean getPitCountsAsLap() {
        return pitCountsAsLap;
    }

    public void setMinimumLapTime(long minimumLapTime) {
        this.minimumLapTime = minimumLapTime;
    }

    public int numberOfCheckpoints() {
        return checkpoints.size();
    }

    public int numberOfTraps() { return traps.size(); }

    public Checkpoint getPit() {
        return pit;
    }

    public void setPit(Checkpoint new_checkpoint) {
        pit = new_checkpoint;
    }

    public boolean hasPit() {
        return pit != null;
    }

    public Checkpoint getPitEnter() {
        return pitEnter;
    }

    public void setPitEnter(Checkpoint pit_enter) {
        this.pitEnter = pit_enter;
    }

    public boolean hasPitEnter() {
        return this.pitEnter != null;
    }

    public ArrayList<Checkpoint> getCheckpoints() {
        return checkpoints;
    }
    public ArrayList<Trap> getTraps() {
        return traps;
    }

    public Track finish() {
        Track track = new Track(id, name, minimumLapTime, checkpoints, traps, pitCountsAsLap);

        track.pit = this.pit;
        track.pit_enter = this.pitEnter;

        return track;
    }
}
