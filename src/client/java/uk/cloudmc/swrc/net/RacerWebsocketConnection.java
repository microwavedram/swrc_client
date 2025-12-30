package uk.cloudmc.swrc.net;

import uk.cloudmc.swrc.Race;
import uk.cloudmc.swrc.SWRC;
import uk.cloudmc.swrc.net.packets.*;
import uk.cloudmc.swrc.util.ChatFormatter;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class RacerWebsocketConnection extends AbstractWebsocketConnection {
    public RacerWebsocketConnection(URI uri) { super(uri); }

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
            case 0xFF: break;
            default:
                SWRC.LOGGER.info("Got unknown packet id {}", packetId);
                SWRC.LOGGER.info(Arrays.toString(bytes));
        }
    }

    @Override
    public void onPacket(Packet<?> uPacket) {
        if (uPacket instanceof S2CHelloPacket packet) {
            C2SHandshakePacket handshake = new C2SHandshakePacket();

            assert SWRC.minecraftClient.player != null;

            handshake.username = SWRC.minecraftClient.player.getName().getString();
            handshake.uuid = SWRC.minecraftClient.player.getUuidAsString();
            handshake.version = SWRC.VERSION;

            sendPacket(handshake);
        }
        if (uPacket instanceof S2CHandshakePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE("[Racer] Authenticated: " + packet.motd));
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
                current_race.getTimerDuration(packet.timer_duration);
                current_race.racer_clients = packet.racer_clients;
                current_race.rc_clients = packet.rc_clients;

                var us = packet.rc_clients.get(SWRC.minecraftClient.player.getName().getString());
                if (us != null) {
                    current_race.probably_tracking = us.tracking;
                }

                if (packet.flap != null && (current_race.flap == null || current_race.flap.hashCode() != packet.flap.hashCode())) SWRC.bestLap.show(packet.flap);

                current_race.setFlap(packet.flap);
            }
        }
        if (uPacket instanceof S2CMessagePacket packet) {
            SWRC.minecraftClient.inGameHud.getChatHud().addMessage(ChatFormatter.GENERIC_MESSAGE(String.format("[Racer] %s", packet.message)));

            SWRC.eventsQueue.addLine(packet.message);
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
