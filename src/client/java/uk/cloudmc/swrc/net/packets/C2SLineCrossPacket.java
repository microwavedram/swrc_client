package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;

public class C2SLineCrossPacket extends Packet<C2SLineCrossPacket> {
    public static final char packetId = 0x02;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public long timestamp;
    @Expose public HashMap<Integer, ArrayList<String>> checkpoint_crosses;

    @Override
    public String toString() {
        return "C2SLineCrossPacket{" +
                "timestamp=" + timestamp +
                ", checkpoint_crosses=" + checkpoint_crosses +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SLineCrossPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SLineCrossPacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
