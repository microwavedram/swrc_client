package uk.cloudmc.swrc;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SWRCModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            SWRCConfig config = SWRCConfig.getInstance();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.of("SWRC Configuration"))
                    .setSavingRunnable(config::save);

            ConfigCategory Category = builder.getOrCreateCategory(Text.of("Settings"));
            ConfigEntryBuilder packet = builder.entryBuilder();

            Category.addEntry(packet.startStrField(Text.of("SWRC Key"), config.swrc_key)
                    .setDefaultValue("")
                    .setSaveConsumer(newValue -> config.swrc_key = newValue)
                    .build());

            Category.addEntry(packet.startStrField(Text.of("Race Key"), config.race_key)
                    .setDefaultValue("")
                    .setSaveConsumer(newValue -> config.race_key = newValue)
                    .build());

            Category.addEntry(packet.startBooleanToggle(Text.of("Toggle Text Shadow"), config.leaderboard_shadow)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> config.leaderboard_shadow = newValue)
                    .build());

            Category.addEntry(packet.startBooleanToggle(Text.of("Render split times above player heads"), config.renderLapTimesAboveHeads)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> config.renderLapTimesAboveHeads = newValue)
                    .build());

            Category.addEntry(packet.startBooleanToggle(Text.of("Render the event feed"), config.renderEventFeed)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> config.renderEventFeed = newValue)
                    .build());

            Category.addEntry(packet.startDoubleField(Text.of("Split hud X"), config.split_hud_x)
                    .setDefaultValue(0.5)
                    .setSaveConsumer(newValue -> config.split_hud_x = newValue)
                    .build());

            Category.addEntry(packet.startDoubleField(Text.of("Split hud Y"), config.split_hud_y)
                    .setDefaultValue(0.6)
                    .setSaveConsumer(newValue -> config.split_hud_y = newValue)
                    .build());

            Category.addEntry(packet.startDoubleField(Text.of("Leaderboard X"), config.leaderboard_x)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> config.leaderboard_x = newValue)
                    .build());

            Category.addEntry(packet.startDoubleField(Text.of("Leaderboard Y"), config.leaderboard_y)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> config.leaderboard_y = newValue)
                    .build());

            Category.addEntry(packet.startDoubleField(Text.of("Leaderboard Scale"), config.leaderboard_scale)
                    .setDefaultValue(1.0)
                    .setSaveConsumer(newValue -> config.leaderboard_scale = newValue)
                    .build());

            Category.addEntry(packet.startTextField(Text.of("Default Server"), config.default_server)
                    .setDefaultValue("wss://swrc.cloudmc.uk/realtime/")
                    .setSaveConsumer(newValue -> config.default_server = newValue)
                    .build());

            Category.addEntry(packet.startTextField(Text.of("NTP Server"), config.ntp_server)
                    .setDefaultValue("pool.ntp.org")
                    .setSaveConsumer(newValue -> config.ntp_server = newValue)
                    .build());

            return builder.build();
        };
    }
}
