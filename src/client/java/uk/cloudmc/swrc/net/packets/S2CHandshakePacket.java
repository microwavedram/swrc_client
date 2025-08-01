package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.SWRC;

import java.nio.charset.StandardCharsets;

public class S2CHandshakePacket extends Packet<S2CHandshakePacket> {
    public static final char packetId = 0x01;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public String motd;

    @Override
    public String toString() {
        return "S2CHandshakePacket{" +
                "motd='" + motd + '\'' +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CHandshakePacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), S2CHandshakePacket.class);
    }

    @Override
    public byte[] serialize() {
        return new byte[] {};
    }
}
