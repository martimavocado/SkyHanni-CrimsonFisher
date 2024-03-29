package at.hannibal2.skyhanni.config.features.fishing;

import at.hannibal2.skyhanni.config.FeatureToggle;
import at.hannibal2.skyhanni.config.core.config.Position;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorSlider;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class SeaCreatureTrackerConfig {

    @Expose
    @ConfigOption(name = "Enabled", desc = "Count the different sea creatures you catch.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean enabled = false;

    @Expose
    public Position position = new Position(20, 20, false, true);

    @Expose
    @ConfigOption(name = "Show Percentage", desc = "Show percentage how often what sea creature got caught.")
    @ConfigEditorBoolean
    public Property<Boolean> showPercentage = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Since", desc = "Shows how many mobs were caught since the last one." + "\nOnly applies to rare Sea Creatures.")
    @ConfigEditorBoolean
    public Property<Boolean> showSince = Property.of(false);

    @Expose
    @ConfigOption(name = "Show Since Threshold", desc = "Customize the minimum threshold for the previous option to show.")
    @ConfigEditorSlider(minValue = 0.0F, maxValue = 100, minStep = 1)
    public Property<Integer> showSinceThreshold = Property.of(0);

    @Expose
    @ConfigOption(name = "Hide Chat", desc = "Hide the chat messages when catching a sea creature.")
    @ConfigEditorBoolean
    public boolean hideChat = false;

    @Expose
    @ConfigOption(name = "Count Double", desc = "Count double hook catches as two catches.")
    @ConfigEditorBoolean
    public boolean countDouble = true;
}
