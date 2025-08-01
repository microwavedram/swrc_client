package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class S2CMessagePacket extends Packet<S2CMessagePacket> {
    public static final char packetId = 0x07;

    @Expose public String message;

    @Override
    public String toString() {
        return "S2CMessagePacket{" +
                "message=" + message +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CMessagePacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), S2CMessagePacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
