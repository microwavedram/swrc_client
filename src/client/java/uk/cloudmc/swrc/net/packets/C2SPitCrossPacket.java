package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class C2SPitCrossPacket extends Packet<C2SPitCrossPacket> {
    public static final char packetId = 0x08;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public long timestamp;
    @Expose public ArrayList<String> pit_crosses;

    @Override
    public String toString() {
        return "C2SPitCrossPacket{" +
                "timestamp=" + timestamp +
                ", pit_crosses=" + pit_crosses +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SPitCrossPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SPitCrossPacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
