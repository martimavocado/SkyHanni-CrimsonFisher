package at.hannibal2.skyhanni.config.features.plus;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class PlusConfig {
    @Expose
    @ConfigOption(name = "Cosmetics", desc = "Enable Skyhanni§6+§r cosmetics.")
    @Accordion
    public CosmeticsConfig cosmetics = new CosmeticsConfig();

    @Expose
    @ConfigOption(name = "AH/BZ Flipper", desc = "Exclusive auto AH and BZ flipping.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> flipper = Property.of(false);

    @Expose
    @ConfigOption(name = "Anti-Rat", desc = "Prevents all attempts of stealing your Microsoft Account only through malicious mods.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> rat = Property.of(false);

    @Expose
    @ConfigOption(name = "Lag Optimizer", desc = "Optimizes your connection to Hypixel, preventing bad connections by dropping all packets.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> lag = Property.of(false);

    @Expose
    @ConfigOption(name = "FPS Optimizer", desc = "Optimizes the mainframe code by installing more ChatTriggers modules.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> fps = Property.of(false);

    @Expose
    @ConfigOption(name = "User Luck", desc = "Doubles SkyHanni§6+§r User Luck by x3.")
    @ConfigEditorBoolean
    @FeatureToggle
    public Property<Boolean> luck = Property.of(false);

    @Expose
    public Boolean disabled = false;
}
