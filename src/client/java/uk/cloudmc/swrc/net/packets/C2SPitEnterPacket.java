package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class C2SPitEnterPacket extends Packet<C2SPitEnterPacket> {
    public static final char packetId = 0x10;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public long timestamp;
    @Expose public ArrayList<String> pit_enter_crosses;

    @Override
    public String toString() {
        return "C2SPitEnterPacket{" +
                "timestamp=" + timestamp +
                ", pitEnter_crosses=" + pit_enter_crosses +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SPitEnterPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SPitEnterPacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
