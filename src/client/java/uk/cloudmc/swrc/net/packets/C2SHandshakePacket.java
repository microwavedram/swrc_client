package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;

public class C2SHandshakePacket extends Packet<C2SHandshakePacket> {
    public static final char packetId = 0x01;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public String username;
    @Expose public String uuid;
    @Expose public String version;
    @Expose public boolean clock_precise;
    @Expose public long clock_precision;

    @Override
    public String toString() {
        return "C2SHandshakePacket{" +
                "username='" + username + '\'' +
                ", uuid='" + uuid + '\'' +
                ", version='" + version + '\'' +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SHandshakePacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SHandshakePacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
