package uk.cloudmc.swrc.net.packets;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;
import uk.cloudmc.swrc.util.SpeedTrapResult;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class C2SSpeedTrapPacket extends Packet<C2SSpeedTrapPacket> {
    public static final char packetId = 0x12;

    @Expose public SpeedTrapResult speedTrapResult;

    @Override
    public String toString() {
        return "C2SSpeedTrapPacket{" +
                ", speedTrapResult=" + speedTrapResult +
                '}';
    }

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SSpeedTrapPacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2SSpeedTrapPacket.class);
    }

    @Override
    public byte[] serialize() {
        return SpeedTrapResult.gsonSerializer.toJson(this).getBytes();
    }
}
