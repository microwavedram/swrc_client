package uk.cloudmc.swrc.net.packets;

import com.google.gson.annotations.Expose;
import uk.cloudmc.swrc.track.Track;

import java.nio.charset.StandardCharsets;

public class C2SModifyRacerPacket extends Packet<C2SModifyRacerPacket> {
    public static final char packetId = 0x06;

    public enum ModifyRacerPacketAction {
        ADD,
        REMOVE,
    }

    @Expose public String racer_name;
    @Expose public ModifyRacerPacketAction action;

    @Override
    public char getPacketId() {
        return packetId;
    }

    @Override
    public C2SModifyRacerPacket fromBytes(byte[] data) {
        return Track.gsonSerializer.fromJson(new String(data, StandardCharsets.UTF_8), C2SModifyRacerPacket.class);
    }

    @Override
    public byte[] serialize() {
        return Track.gsonSerializer.toJson(this).getBytes();
    }
}
