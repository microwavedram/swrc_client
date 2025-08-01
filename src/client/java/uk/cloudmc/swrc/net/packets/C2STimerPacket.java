package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;

public class C2STimerPacket extends Packet<C2STimerPacket> {
    public static final char packetId = 0x14;

    @Expose public long start_time;
    @Expose public long duration;

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2STimerPacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2STimerPacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
