package uk.cloudmc.swrc.net;

import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.util.ChatFormatter;
import uk.cloudmc.swrc.util.NTPTimeSync;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RCWebsocketConnection extends AbstractWebsocketConnection {
    public RCWebsocketConnection(URI uri) {
        super(uri);
    }

    @Override
    public void onMessage(ByteBuffer buffer) {
        
        byte[] bytes = buffer.array();

        int packetId = bytes[0] & 0xFF;
        byte[] payload = Arrays.copyOfRange(bytes, 1, bytes.length);

        switch (packetId) {
            case(S2CHelloPacket.packetId):
                onPacket(new S2CHelloPacket().fromBytes(payload));
                break;
            case(S2CHandshakePacket.packetId):
                onPacket(new S2CHandshakePacket().fromBytes(payload));
                break;
            case(S2CMessagePacket.packetId):
                onPacket(new S2CMessagePacket().fromBytes(payload));
                break;
            case 0xFF: break;
            default:
                SWRC.LOGGER.info("Got unknown packet id {}", packetId);
                SWRC.LOGGER.info(Arrays.toString(bytes));
        }
    }

    @Override
    public void onPacket(Packet<?> uPacket) {
        if (uPacket instanceof S2CHelloPacket) {
            C2SHandshakePacket handshake = new C2SHandshakePacket();

            assert SWRC.minecraftClient.player != null;

            handshake.username = SWRC.minecraftClient.player.getName().getString();
            handshake.uuid = SWRC.minecraftClient.player.getUuidAsString();
            handshake.version = SWRC.VERSION;
            handshake.clock_precise = NTPTimeSync.isPrecise();
            handshake.clock_precision = NTPTimeSync.getOffset();

            sendPacket(handshake);
        }
        if (uPacket instanceof S2CHandshakePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[RC] Authenticated: " + packet.motd));
        }
        if (uPacket instanceof S2CMessagePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[RC] %s", packet.message)));

            SWRC.eventsQueue.addLine(packet.message);
        }
    }
}
