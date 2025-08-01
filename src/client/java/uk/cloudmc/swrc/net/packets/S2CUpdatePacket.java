package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class S2CUpdatePacket extends Packet<S2CUpdatePacket> {
    public static final char packetId = 0x05;

    public static class RaceLeaderboardPosition {
        @Expose public String player_name;
        @Expose public long time_delta;
        @Expose public boolean in_pit;
        @Expose public long flap;

        public RaceLeaderboardPosition(String player_name, long time_delta, boolean in_pit, long flap) {
            this.player_name = player_name;
            this.time_delta = time_delta;
            this.in_pit = in_pit;
            this.flap = flap;
        }

        @Override
        public String toString() {
            return "RaceLeaderboardPosition{" +
                    "player_name='" + player_name + '\'' +
                    ", time_delta=" + time_delta +
                    ", in_pit=" + in_pit +
                    ", flap=" + flap +
                    '}';
        }
    }

    public static class PlayerSplit {
        @Expose public String player_name;
        @Expose public long timestamp;

        public PlayerSplit(String player_name, long timestamp) {
            this.player_name = player_name;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return "RaceLeaderboardPosition{" +
                    "player_name='" + player_name + '\'' +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }

    public static class Flap {
        @Expose String player_name;
        @Expose int lap;
        @Expose long time;
        @Expose long acquired;

        public Flap(String player_name, int lap, long time, long acquired) {
            this.player_name = player_name;
            this.lap = lap;
            this.time = time;
            this.acquired = acquired;
        }

        public String getPlayer_name() {
            return player_name;
        }

        public int getLap() {
            return lap;
        }

        public long getTime() {
            return time;
        }

        public long getAcquired() {
            return acquired;
        }

        @Override
        public String toString() {
            return "Flap{" +
                    "player_name='" + player_name + '\'' +
                    ", lap=" + lap +
                    ", time=" + time +
                    ", acquired=" + acquired +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            Flap flap = (Flap) o;
            return lap == flap.lap && time == flap.time && acquired == flap.acquired && player_name.equals(flap.player_name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(player_name, lap, time, acquired);
        }
    }

    @Expose public ArrayList<String> racers;
    @Expose public ArrayList<RaceLeaderboardPosition> race_leaderboard;
    @Expose public ArrayList<PlayerSplit> race_lap_begin;
    @Expose public HashMap<String, Integer> racer_pits;
    @Expose public HashMap<String, Integer> racer_laps;
    @Expose public Flap flap;
    @Expose public long timer_start;
    @Expose public long timer_duration;

    @Override
    public String toString() {
        return "S2CUpdatePacket{" +
                "racers=" + racers +
                ", race_leaderboard=" + race_leaderboard +
                ", race_lap_begin=" + race_lap_begin +
                ", racer_pits=" + racer_pits +
                ", racer_laps=" + racer_laps +
                ", flap=" + flap +
                ", timer_start=" + timer_start +
                ", timer_duration=" + timer_duration +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CUpdatePacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), S2CUpdatePacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
