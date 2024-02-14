package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.WorldClickEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class EnderPearlCooldown {
    private val config get() = SkyHanniMod.feature.misc
    private var lastThrowTime = SimpleTimeMark.farPast()
    private val itemID = "ENDER_PEARL".asInternalName()

    @SubscribeEvent
    fun onClick(event: WorldClickEvent) {
//         if (!IslandType.THE_END.isInIsland()) return
        if (config.enderPearlCooldown == 0.0f) return
        if (event.itemInHand == null) return
        if (event.clickType != ClickType.RIGHT_CLICK) return
        if (event.itemInHand.getInternalName() != itemID) return

        if (lastThrowTime.passedSince() > config.enderPearlCooldown.toDouble().seconds) lastThrowTime = SimpleTimeMark.now()
        else {
            ChatUtils.debug("canceling")
            event.cancel()
        }
    }
}
