package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;

public class C2SRaceState extends Packet<C2SRaceState> {
    public static final char packetId = 0x09;

    @Expose public Race.RaceState state;

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SRaceState fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2SRaceState.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
