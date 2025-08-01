package uk.cloudmc.swrc.net;

import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.hud.BestLap;
import uk.cloudmc.swrc.hud.EventsQueue;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;
import java.util.Arrays;

public class RacerWebsocketConnection extends AbstractWebsocketConnection {
    public RacerWebsocketConnection(URI uri) { super(uri); }

    @Override
    public void onDisconnect(int code, String reason, boolean remote) {
        SWRC.LOGGER.warn("[RACER] [{}] {} {}", code, reason, remote);
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
            case(S2CNewRacePacket.packetId):
                onPacket(new S2CNewRacePacket().fromBytes(payload));
                break;
            case(S2CUpdatePacket.packetId):
                onPacket(new S2CUpdatePacket().fromBytes(payload));
                break;
            case(S2CMessagePacket.packetId):
                onPacket(new S2CMessagePacket().fromBytes(payload));
                break;
            case(S2CRaceState.packetId):
                onPacket(new S2CRaceState().fromBytes(payload));
                break;
            case(S2CEndRacePacket.packetId):
                onPacket(new S2CEndRacePacket().fromBytes(payload));
                break;
            default:
                SWRC.LOGGER.info("Got unknown packet id {}", packetId);
        }
    }

    @Override
    public void onPacket(Packet<?> uPacket) {
        if (uPacket instanceof S2CHelloPacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[Racer] Successfully connected to server."));

            C2SHandshakePacket handshake = new C2SHandshakePacket();

            assert SWRC.minecraftClient.player != null;

            handshake.username = SWRC.minecraftClient.player.getName().getString();
            handshake.uuid = SWRC.minecraftClient.player.getUuidAsString();
            handshake.version = SWRC.VERSION;

            sendPacket(handshake);
        }
        if (uPacket instanceof S2CHandshakePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[Racer] " + packet.motd));
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[Racer] Authenticated."));
        }
        if (uPacket instanceof S2CNewRacePacket packet) {

            SWRC.setRace(new Race(packet.race_id, packet.track, packet.total_laps, packet.total_pits));

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[Racer] Received new race from server (%s)", packet.race_id)));
        }
        if (uPacket instanceof S2CUpdatePacket packet) {

            Race current_race = SWRC.getRace();
            if (current_race != null) {
                current_race.setRacers(packet.racers);
                current_race.setLeaderboard(packet.race_leaderboard);
                current_race.setLapBeginTimes(packet.race_lap_begin);
                current_race.setPits(packet.racer_pits);
                current_race.setLapCounts(packet.racer_laps);
                current_race.setStartTime(packet.timer_start);
                current_race.setDuration(packet.timer_duration);

                if (current_race.flap == null || current_race.flap.hashCode() != packet.flap.hashCode()) ((BestLap) SWRC.bestLap).show();

                current_race.setFlap(packet.flap);
            }
        }
        if (uPacket instanceof S2CMessagePacket packet) {

            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[Racer] %s", packet.message)));

            ((EventsQueue) SWRC.eventsQueue).addLine(packet.message);
        }
        if (uPacket instanceof S2CRaceState packet) {

            Race current_race = SWRC.getRace();
            if (current_race != null) {
                current_race.setRaceState(packet.state);
            }
        }
        if (uPacket instanceof S2CEndRacePacket packet) {
            Race current_race = SWRC.getRace();

            if (current_race != null) {
                SWRC.setRace(null);
            }
        }
    }
}
