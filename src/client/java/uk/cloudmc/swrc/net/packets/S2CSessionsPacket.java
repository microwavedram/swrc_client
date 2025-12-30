package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import it.unimi.dsi.fastutil.Hash;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class S2CSessionsPacket extends Packet<S2CSessionsPacket> {
    public static final char packetId = 0x4f;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static class Session {
        @Expose public String state;
        @Expose public String status;
        @Expose public double perf;
    }

    @Expose public Map<String, Session> sessions = new HashMap<>();
    @Expose public double perf;

    @Override
    public String toString() {
        return "S2CSessionsPacket{" +
                "gson=" + gson +
                ", perf=" + perf +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public S2CSessionsPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), S2CSessionsPacket.class);
    }

    @Override
    public byte[] serialize() {
        return new byte[] {};
    }
}
