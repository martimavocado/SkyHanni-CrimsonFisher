package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NEUPetsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.PetSpecificData
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object PetAPI {
    // stores a list of the xp required for each level, i don't know how to word this better
    private var xpLeveling: List<Int>? = null
    private var petSpecificData: Map<String, PetSpecificData>? = null

    private val patternGroup = RepoPattern.group("misc.pet")
    private val petMenuPattern by patternGroup.pattern(
        "menu.title",
        "Pets(?: \\(\\d+/\\d+\\) )?",
    )
    /**
     * REGEX-TEST: §e⭐ §7[Lvl 200] §6Golden Dragon§d ✦
     * REGEX-TEST: ⭐ [Lvl 100] Black Cat ✦
     */
    private val petItemName by patternGroup.pattern(
        "item.name",
        "(?<favorite>(?:§.)*⭐ )?(?:§.)*\\[Lvl (?<level>\\d+)] (?<name>.*)",
    )
    private val neuRepoPetItemName by patternGroup.pattern(
        "item.name.neu.format",
        "(?:§f§f)?§7\\[Lvl (?:1➡(?:100|200)|\\{LVL})] (?<name>.*)",
    )

    /**
     * REGEX-TEST: §7To Select Process (Slot #2)
     * REGEX-TEST: §7To Select Process (Slot #4)
     * REGEX-TEST: §7To Select Process (Slot #7)
     */
    private val forgeBackMenuPattern by patternGroup.pattern(
        "menu.forge.goback",
        "§7To Select Process \\(Slot #\\d\\)",
    )

    private val ignoredPetStrings = listOf(
        "Archer",
        "Berserk",
        "Mage",
        "Tank",
        "Healer",
        "➡",
    )

    fun isPetMenu(inventoryTitle: String, inventoryItems: Map<Int, ItemStack>): Boolean {
        if (!petMenuPattern.matches(inventoryTitle)) return false

        // Checks if the menu is not the Pets submenu in the forge
        val isForgeMenu = inventoryItems[48]?.getLore().orEmpty().any { forgeBackMenuPattern.matches(it) }
        return !isForgeMenu
    }

    // goes unused and might be unfinished, will replace currentPet when ready
    var newCurrentPet: PetData?
        get() = ProfileStorageData.profileSpecific?.newCurrentPet
        set(value) {
            ProfileStorageData.profileSpecific?.newCurrentPet = value
        }

    // format "§aEnderman"
    // Contains color code + name and for older SkyHanni users maybe also the pet level
    var currentPet: String?
        get() = ProfileStorageData.profileSpecific?.currentPet?.takeIf { it.isNotEmpty() }
        set(value) {
            ProfileStorageData.profileSpecific?.currentPet = value
        }

    fun isCurrentPet(petName: String): Boolean = currentPet?.contains(petName) ?: false

    fun getCleanName(nameWithLevel: String): String? {
        petItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }
        neuRepoPetItemName.matchMatcher(nameWithLevel) {
            return group("name")
        }

        return null
    }

    fun getPetLevel(nameWithLevel: String): Int? = petItemName.matchMatcher(nameWithLevel) {
        group("level").toInt()
    }

    fun hasPetName(name: String): Boolean = petItemName.matches(name) && !ignoredPetStrings.any { name.contains(it) }

    @SubscribeEvent
    fun onNEURepoReload(event: NeuRepositoryReloadEvent) {
        val data = event.getConstant<NEUPetsJson>("pets")
        xpLeveling = data.petLevels
        petSpecificData = data.customPetLeveling
    }
}
