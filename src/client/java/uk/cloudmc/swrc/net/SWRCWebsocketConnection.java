package uk.cloudmc.swrc.net;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.SWRCConfig;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SWRCWebsocketConnection extends AbstractWebsocketConnection {
    public boolean has_recieved_session = false;

    public Map<String, S2CSessionsPacket.Session> sessions = new HashMap<>();
    public Double server_performance = null;

    public String server_label = "no_motd";

    public SWRCWebsocketConnection(URI uri) {
        super(uri);
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
            case(S2CSessionsPacket.packetId):
                onPacket(new S2CSessionsPacket().fromBytes(payload));
                break;
            case(S2CNewSessionPacket.packetId):
                onPacket(new S2CNewSessionPacket().fromBytes(payload));
            default:
                SWRC.LOGGER.warn("Got unknown packet id {}", packetId);
        }
    }

    public Map<String, S2CSessionsPacket.Session> getSessions() {
        return sessions;
    }

    @Override
    public void onPacket(Packet<?> uPacket) {
        if (uPacket instanceof S2CHelloPacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[SWRC] Successfully connected to server."));

            server_label = packet.server_label;

            C2SHandshakePacket handshake = new C2SHandshakePacket();

            assert SWRC.minecraftClient.player != null;
            
            handshake.username = SWRC.minecraftClient.player.getName().getString();
            handshake.uuid = SWRC.minecraftClient.player.getUuidAsString();
            handshake.version = SWRC.VERSION;

            sendPacket(handshake);
        }
        if (uPacket instanceof S2CHandshakePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[SWRC] " + packet.motd));
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[SWRC] Connected to " + this.server_label));
        }
        if (uPacket instanceof S2CMessagePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[SWRC] %s", packet.message)));
        }
        if (uPacket instanceof S2CSessionsPacket packet) {
            server_performance = packet.perf;
            sessions = packet.sessions;

            if (!has_recieved_session) {

                this.promptSessions();

                has_recieved_session = true;
            }
        }
        if (uPacket instanceof S2CNewSessionPacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[SWRC] New session created %s", packet.id)));

            SWRCConfig.getInstance().race_key = packet.race_key;
            SWRCConfig.getInstance().save();

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("Race Key saved to config"));

            GLFW.glfwSetClipboardString(SWRC.minecraftClient.getWindow().getHandle(), packet.race_key);

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("Race Key copied to clipboard"));
        }
    }

    public void promptSessions() {
        MutableText text = Text.empty()
                .append(ChatFormatter.SWRC_PREFIX().append(" Connected to ").append(server_label));

        for (Map.Entry<String, S2CSessionsPacket.Session> session : sessions.entrySet()) {
            text = text.append(
                Text.literal("\n > ")
                    .append(Text.literal("[CONNECT] ").styled(style ->
                        style
                            .withFormatting(Formatting.GREEN)
                            .withHoverEvent(new HoverEvent.ShowText(
                                Text.literal("Connect to " + session.getKey())
                            ))
                            .withClickEvent(new ClickEvent.RunCommand(
                                "/swrc server sessions " + session.getKey() + " connect"
                            ))
                    ))
                    .append(Text.literal(session.getKey()).styled(style -> style.withFormatting(Formatting.AQUA)))
                    .append(Text.literal(" - "))
                    .append(Text.literal(session.getValue().state).styled(style -> style.withFormatting(Formatting.GOLD)))
                    .append(Text.literal(" - "))
                    .append(Text.literal(String.valueOf(session.getValue().perf)))
                    .append(Text.literal("mspt"))
            );
        }

        SWRC.minecraftClient.inGameHud.getChatHud().addMessage(text);
    }
}
