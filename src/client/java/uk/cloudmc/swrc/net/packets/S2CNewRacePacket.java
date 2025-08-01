package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;

public class S2CNewRacePacket extends Packet<S2CNewRacePacket> {
    public static final char packetId = 0x04;

    @Expose public String race_id;
    @Expose public Track track;
    @Expose public int total_laps;
    @Expose public int total_pits;

    @Override
    public String toString() {
        return "S2CNewRacePacket{" +
                "race_id='" + race_id + '\'' +
                ", track=" + track +
                ", total_laps=" + total_laps +
                ", total_pits=" + total_pits +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CNewRacePacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), S2CNewRacePacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
