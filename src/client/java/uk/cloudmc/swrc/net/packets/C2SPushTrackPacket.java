package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class C2SPushTrackPacket extends Packet<C2SPushTrackPacket> {
    public static final char packetId = 0x03;

    @Expose public String race_id;
    @Expose public Track track;
    @Expose public int total_laps;
    @Expose public int total_pits;

    @Override
    public String toString() {
        return "C2SPushTrackPacket{" +
                "track=" + track +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SPushTrackPacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2SPushTrackPacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
