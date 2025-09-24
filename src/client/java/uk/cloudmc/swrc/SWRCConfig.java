package uk.cloudmc.swrc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.shedaniel.autoconfig.ConfigData;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class SWRCConfig implements ConfigData {
    private static final Path file = FabricLoader.getInstance().getConfigDir().resolve(SWRC.NAMESPACE).resolve("config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SWRCConfig instance;

    public String swrc_key = "";
    public String race_key = "";
    public String header_text = "S3 @ %s";
    public boolean pos_tracking = true;
    public boolean leaderboard_shadow = true;
    public boolean render_lap_times_above_heads = true;
    public boolean interpolation_compat = true;

    public String default_server = "wss://swrc.cloudmc.uk/realtime/";

    public static SWRCConfig getInstance() {
        if (instance == null) {
            try {
                instance = GSON.fromJson(Files.readString(file), SWRCConfig.class);
            } catch (IOException exception) {
                SWRC.LOGGER.warn("SWRC couldn't load the config, using defaults.");
                instance = new SWRCConfig();
            }
        }

        return instance;
    }

    public void save() {
        try {
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException e) {
            SWRC.LOGGER.error("SWRC could not save the config.");
            throw new RuntimeException(e);
        }
    }
}
