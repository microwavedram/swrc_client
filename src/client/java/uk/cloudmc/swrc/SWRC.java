package uk.cloudmc.swrc;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleResourceReloadListener;
import net.fabricmc.fabric.impl.resource.loader.ResourceManagerHelperImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.cloudmc.swrc.command.RaceCommand;
import uk.cloudmc.swrc.command.RootCommand;
import uk.cloudmc.swrc.hud.*;
import uk.cloudmc.swrc.render.TrackBuilderRenderer;
import uk.cloudmc.swrc.track.TrackBuilder;
import uk.cloudmc.swrc.tt.TimeTrial;
import uk.cloudmc.swrc.util.NTPTimeSync;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SWRC implements ClientModInitializer {

	public static Logger LOGGER = LoggerFactory.getLogger("SWRC");
	public static final String NAMESPACE = "swrc";
	public static final String VERSION = FabricLoader.getInstance().getModContainer(NAMESPACE).orElseThrow().getMetadata().getVersion().toString();

	public static final MinecraftClient minecraftClient = MinecraftClient.getInstance();

	private static Race race;
	private static TimeTrial timeTrial;
	private static TrackBuilder trackBuilder;

	public static final TrackBuilderRenderer trackBuilderRenderer = new TrackBuilderRenderer();
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

		try {
			NTPTimeSync.attemptTimeSync();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

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
			RootCommand rootCommand = new RootCommand();
			dispatcher.register(rootCommand.command());
			dispatcher.register(rootCommand.raceCommand.command());
			dispatcher.register(rootCommand.trackBuilderCommand.command());
		});

		ClientLifecycleEvents.CLIENT_STARTED.register(minecraftClient -> {
			SplitTime.initColors();
		});

		ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleResourceReloadListener<>() {

			@Override
			public Identifier getFabricId() {
				return Identifier.of(NAMESPACE, "resource_reload");
			}

			@Override
			public CompletableFuture<Object> load(ResourceManager resourceManager, Executor executor) {
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public CompletableFuture<Void> apply(Object o, ResourceManager resourceManager, Executor executor) {
				return CompletableFuture.runAsync(SplitTime::initColors, executor);
			}
		});


		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (client.world == null) return;

			if (timeTrial != null) {
				timeTrial.update();
			}

			if (race == null) return;
			if (!WebsocketManager.rcSocketAvalible()) return;

			race.update();
		});

		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_disconnect_banner"), disconnectBanner);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_events_queue"), eventsQueue);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_timer"), timerHud);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_quali_leaderboard"), qualiLeaderboard);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_race_leaderboard"), raceLeaderboard);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_split_time"), splitTime);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_status"), statusHud);
		HudElementRegistry.addFirst(Identifier.of(NAMESPACE, "hud_best_lap"), bestLap);

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

	public static TimeTrial getTimeTrial() {
		return timeTrial;
	}

	public static void setTimeTrial(TimeTrial timeTrial) {
		SWRC.timeTrial = timeTrial;
	}
}