package uk.cloudmc.swrc.net;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.net.packets.Packet;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;

public abstract class AbstractWebsocketConnection extends WebSocketClient {
    AbstractWebsocketConnection(URI uri) {
        super(uri);
    }

    public void sendPacket(Packet<?> packet) {
        send(packet.serializeForNetwork().array());
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        onConnect();
    }
    public void onConnect() {}
    public void onDisconnect(int code, String reason, boolean remote) {

    }
    public void onPacket(Packet<?> packet) {}

    @Override
    public void onMessage(String message) {}

    @Override
    public void onClose(int code, String reason, boolean remote) {
        if (!remote) SWRC.LOGGER.warn("Disconnect from WS ({}) {}", code, reason);
        onDisconnect(code, reason, remote);
    }

    @Override
    public void onError(Exception ex) {
        SWRC.LOGGER.error(ex.toString(), ex.getMessage());
        if (ex.getMessage().contains("Connection refused: connect")) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("Failed to connect"));
            return;
        }
        SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(ex.getMessage()));
    }
}
