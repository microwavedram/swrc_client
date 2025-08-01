package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;

public class C2SDestroySessionPacket extends Packet<C2SDestroySessionPacket> {
    public static final char packetId = 0x42;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public String session;
    @Expose public String key;

    @Override
    public String toString() {
        return "C2SDestroySessionPacket{" +
                "session='" + session + '\'' +
                ", key='" + key + '\'' +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SDestroySessionPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SDestroySessionPacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
