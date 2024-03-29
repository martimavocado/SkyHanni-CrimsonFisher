package at.hannibal2.skyhanni.config.features.event.diana;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class MythologicalMobTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Counts the different mythological mobs you have dug up.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    public Position position = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Show percentage how often what mob spawned.")
    @ConfigEditorBoolean
    public Property<Boolean> showPercentage = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Since", desc = "Shows how many mobs were spawned since the last Inquisitor.")
    @ConfigEditorBoolean
    public Property<Boolean> showSince = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Since Threshold", desc = "Customize the minimum threshold for the previous option to show.")
    @ConfigEditorSlider(minValue = 0.0F, maxValue = 100, minStep = 1)
    public Property<Integer> showSinceThreshold = Property.of(0);

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat messages when digging up a mythological mob.")
    @ConfigEditorBoolean
    public boolean hideChat = false;
}
