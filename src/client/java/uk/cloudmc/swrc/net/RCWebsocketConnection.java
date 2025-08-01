package uk.cloudmc.swrc.net;

import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;
import java.util.Arrays;

public class RCWebsocketConnection extends AbstractWebsocketConnection {
    public RCWebsocketConnection(URI uri) {
        super(uri);
    }

    @Override
    public void onDisconnect(int code, String reason, boolean remote) {
        SWRC.LOGGER.warn("[RC] [{}] {} {}", code, reason, remote);
    }

    @Override
    public void onMessage(String message) {
        byte[] bytes = message.getBytes();

        int packetId = bytes[0];
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
            default:
                SWRC.LOGGER.warn("Got unknown packet id {}", packetId);
        }
    }

    @Override
    public void onPacket(Packet<?> uPacket) {
        if (uPacket instanceof S2CHelloPacket) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[RC] Successfully connected to server."));

            C2SHandshakePacket handshake = new C2SHandshakePacket();

            assert SWRC.minecraftClient.player != null;

            handshake.username = SWRC.minecraftClient.player.getName().getString();
            handshake.uuid = SWRC.minecraftClient.player.getUuidAsString();
            handshake.version = SWRC.VERSION;

            sendPacket(handshake);
        }
        if (uPacket instanceof S2CHandshakePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[RC] " + packet.motd));
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[RC] Authenticated as Race Control."));
        }
        if (uPacket instanceof S2CMessagePacket packet) {

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[RC] %s", packet.message)));
        }
    }
}
