package uk.cloudmc.swrc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
//import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
//import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.cloudmc.swrc.command.RootCommand;
import uk.cloudmc.swrc.hud.*;
import uk.cloudmc.swrc.render.TrackBuilderRenderer;
import uk.cloudmc.swrc.track.TrackBuilder;

import java.io.File;

public class SWRC implements ClientModInitializer {

	public static Logger LOGGER = LoggerFactory.getLogger("SWRC");
	public static final String NAMESPACE = "swrc";
	public static final String VERSION = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow().getMetadata().getVersion().toString();

	public static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

	private static Race race;
	private static TrackBuilder trackBuilder;

	private static final TrackBuilderRenderer trackBuilderRenderer = new TrackBuilderRenderer();
	public static final RaceLeaderboard raceLeaderboard = new RaceLeaderboard();
	public static final QualiLeaderboard qualiLeaderboard = new QualiLeaderboard();
	public static final SplitTime splitTime = new SplitTime();
	public static final BestLap bestLap = new BestLap();
	public static final TimerHud timerHud = new TimerHud();
	public static final EventsQueue eventsQueue = new EventsQueue();
	public static final DisconnectBanner disconnectBanner = new DisconnectBanner();
	public static final StatusHud statusHud = new StatusHud();

	@Override
	public void onInitializeClient() {
		File config_folder = FabricLoader.getInstance().getConfigDir().resolve(NAMESPACE).toFile();
		if (!config_folder.exists()) {
            boolean _ignore = config_folder.mkdir();
		}

		File tracks_folder = FabricLoader.getInstance().getConfigDir().resolve(NAMESPACE).resolve("tracks").toFile();
		if (!tracks_folder.exists()) {
			boolean _ignore = tracks_folder.mkdir();
		}

		File results_folder = FabricLoader.getInstance().getConfigDir().resolve(NAMESPACE).resolve("results").toFile();
		if (!results_folder.exists()) {
			boolean _ignore = results_folder.mkdir();
		}

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(new RootCommand().command());
		});

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.world == null) return;
			if (race == null) return;
			if (!WebsocketManager.rcSocketAvalible()) return;

			race.update();
		});

		HudElementRegistry.addFirst(
				Identifier.of(NAMESPACE, "hud"),
				(context, tickCounter) -> {
					if (raceLeaderboard.shouldRender()) {
						raceLeaderboard.render(context, 0.0f);
					}
					if (qualiLeaderboard.shouldRender()) {
						qualiLeaderboard.render(context, 0.0f);
					}
					if (splitTime.shouldRender()) {
						splitTime.render(context, 0.0f);
					}
					if (bestLap.shouldRender()) {
						bestLap.render(context, 0.0f);
					}
					if (timerHud.shouldRender()) {
						timerHud.render(context, 0.0f);
					}
					if (eventsQueue.shouldRender()) {
						eventsQueue.render(context, 0.0f);
					}
					if (disconnectBanner.shouldRender()) {
						disconnectBanner.render(context, 0.0f);
					}
					if (statusHud.shouldRender()) {
						statusHud.render(context, 0.0f);
					}
				}
		);

		WorldRenderEvents.LAST.register(trackBuilderRenderer);
	}

	public static TrackBuilder getTrackBuilder() {
		return trackBuilder;
	}

	public static void setTrackBuilder(TrackBuilder trackBuilder) {
		SWRC.trackBuilder = trackBuilder;
	}

	public static Race getRace() {
		return race;
	}

	public static void setRace(Race race) {
		SWRC.race = race;
	}

	public static String getRaceName() {
		return race.getTrackName();
	}
}