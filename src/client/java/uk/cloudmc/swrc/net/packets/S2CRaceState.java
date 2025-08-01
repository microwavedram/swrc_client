package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class S2CRaceState extends Packet<S2CRaceState> {
    public static final char packetId = 0x09;

    @Expose public Race.RaceState state;

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CRaceState fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), S2CRaceState.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
