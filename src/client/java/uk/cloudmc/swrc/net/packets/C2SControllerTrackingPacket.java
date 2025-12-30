package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.nio.charset.StandardCharsets;

public class C2SControllerTrackingPacket extends Packet<C2SControllerTrackingPacket> {
    public static final char packetId = 0x17;

    private final Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose public String controller;
    @Expose public boolean state;

    @Override
    public String toString() {
        return "C2SControllerTrackingPacket{" +
                "gson=" + gson +
                ", controller='" + controller + '\'' +
                ", state=" + state +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SControllerTrackingPacket fromBytes(byte[] data) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), C2SControllerTrackingPacket.class);
    }

    @Override
    public byte[] serialize() {
        return gson.toJson(this).getBytes();
    }
}
