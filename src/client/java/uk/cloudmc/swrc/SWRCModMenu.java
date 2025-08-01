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

            Category.addEntry(packet.startStrField(Text.of("Default server"), config.default_server)
                    .setDefaultValue("wss://swrc.cloudmc.uk/realtime/")
                    .setSaveConsumer(newValue -> config.default_server = newValue)
                    .build());

            Category.addEntry(packet.startStrField(Text.of("SWRC Key"), config.swrc_key)
                    .setDefaultValue("")
                    .setSaveConsumer(newValue -> config.swrc_key = newValue)
                    .build());

            Category.addEntry(packet.startStrField(Text.of("Race Key"), config.race_key)
                    .setDefaultValue("")
                    .setSaveConsumer(newValue -> config.race_key = newValue)
                    .build());

            Category.addEntry(packet.startStrField(Text.of("Race Header Text"), config.header_text)
                    .setDefaultValue("S2 @ %s")
                    .setSaveConsumer(newValue -> config.header_text = newValue)
                    .build());

            Category.addEntry(packet.startBooleanToggle(Text.of("Toggle Position Tracking"), config.pos_tracking)
                    .setDefaultValue(false)
                    .setSaveConsumer(newValue -> config.pos_tracking = newValue)
                    .build());

            Category.addEntry(packet.startBooleanToggle(Text.of("Toggle Text Shadow"), config.leaderboard_shadow)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> config.leaderboard_shadow = newValue)
                    .build());

            return builder.build();
        };
    }
}
