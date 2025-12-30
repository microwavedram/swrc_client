package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;

public class C2SEndRacePacket extends Packet<C2SEndRacePacket> {
    public static final char packetId = 0x11;

    @Expose
    public boolean dump = false;

    @Override
    public String toString() {
        return "C2SEndRacePacket{" +
                "dump=" + dump +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SEndRacePacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2SEndRacePacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
