package at.hannibal2.skyhanni.config.features.plus;

import com.google.gson.annotations.Expose;
import io.github.moulberry.moulconfig.annotations.ConfigEditorBoolean;
import io.github.moulberry.moulconfig.annotations.ConfigEditorDropdown;
import io.github.moulberry.moulconfig.annotations.ConfigOption;
import io.github.moulberry.moulconfig.observer.Property;

public class CosmeticsConfig {
    @Expose
    @ConfigOption(name = "Cloak Type", desc = "Enable a very special cloak viewable by all Hypixel Staff.")
    @ConfigEditorDropdown
    public CloakType cloak = CloakType.OFF;

    @Expose
    @ConfigOption(name = "Highlight Name", desc = "Highlights your in-game name to all SkyHanni§c-§r users in tab.")
    @ConfigEditorBoolean
    public Property<Boolean> highlight = Property.of(false);

    public enum CloakType {
        CHROMA("Chroma"),
        GARDEN("Garden"),
        DUNGEON("Dungeon"),
        KUUDRA("Kuudra"),
        MINING("Mining"),
        SCATHA("Scatha"),
        CUSTOM("Custom"),
        OFF("Off");
        private final String str;

        CloakType(String str) {
            this.str = str;
        }

        @Override
        public String toString() {
            return str;
        }
    }
}
