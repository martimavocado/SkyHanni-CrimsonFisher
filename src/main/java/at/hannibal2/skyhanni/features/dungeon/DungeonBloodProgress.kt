package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonBloodProgress {
    private val patternGroup = RepoPattern.group("dungeons.blood.spawn")
    private val spawn1 by patternGroup.pattern(
        "message1",
        "§c\\[BOSS] The Watcher§r§f: This guy looks like a fighter."
    )
    private val spawn2 by patternGroup.pattern(
        "message2",
        "§c\\[BOSS] The Watcher§r§f: Hmmm... this one!*"
    )
    private val spawn3 by patternGroup.pattern(
        "message3",
        "§c\\[BOSS] The Watcher§r§f: You'll do."
    )
    private val spawn4 by patternGroup.pattern(
        "message4",
        "§c\\[BOSS] The Watcher§r§f: Go, fight!"
    )
    private val spawn5 by patternGroup.pattern(
        "message5",
        "§c\\[BOSS] The Watcher§r§f: Go and live again!"
    )
    private val spawn6 by patternGroup.pattern(
        "message6",
        "§c\\[BOSS] The Watcher§r§f: Let's see how you can handle this."
    )

    private var bloodMobCount = 0
    private var floorType = ""

    private val config get() = SkyHanniMod.feature.dungeon.bloodProgress

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        when {
            spawn1.matches(event.message) -> addBloodMob(event.message)
            spawn2.matches(event.message) -> addBloodMob(event.message)
            spawn3.matches(event.message) -> addBloodMob(event.message)
            spawn4.matches(event.message) -> addBloodMob(event.message)
            spawn5.matches(event.message) -> addBloodMob(event.message)
            spawn6.matches(event.message) -> addBloodMob(event.message)
        }
    }

    private fun addBloodMob(message: String) {
        println("Used message $message, from $bloodMobCount to ${bloodMobCount + 1}")
        bloodMobCount++
        floorType = DungeonAPI.dungeonFloor.toString()
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (bloodMobCount == 0) return
        config.position.renderString("Blood Mob Count: $bloodMobCount", posLabel = "Blood Progress")
    }

    @SubscribeEvent
    fun onWorldSwitch(event: LorenzWorldChangeEvent) {
        if (bloodMobCount != 0) {
            ChatUtils.chat("Counted $bloodMobCount spawning messages in $floorType")
            bloodMobCount = 0
        }
    }
}