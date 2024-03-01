package at.hannibal2.skyhanni.config.features.plus;

import at.hannibal2.skyhanni.config.FeatureToggle;
import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.Accordion;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigOption;

public class PlusConfig {
    @Expose
    @ConfigOption(name = "Cosmetics", desc = "Enable Skyhanni§6+§r cosmetics.")
    @Accordion
    public CosmeticsConfig cosmetics = new CosmeticsConfig();

    @Expose
    @ConfigOption(name = "AH/BZ Flipper", desc = "Exclusive auto AH and BZ flipping.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean flipper = false;

    @Expose
    @ConfigOption(name = "Anti-Rat", desc = "Prevents all attempts of stealing your Microsoft Account only through malicious mods.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean antiRat = true;

    @Expose
    @ConfigOption(name = "Lag Optimizer", desc = "Optimizes your connection to Hypixel, preventing bad connections by dropping all packets.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean lag = true;

    @Expose
    @ConfigOption(name = "FPS Optimizer", desc = "Optimizes the mainframe code by installing more ChatTriggers modules.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean fps = true;

    @Expose
    @ConfigOption(name = "User Luck", desc = "Doubles SkyHanni§6+§r User Luck by x3.")
    @ConfigEditorBoolean
    @FeatureToggle
    public boolean luck = true;
}
