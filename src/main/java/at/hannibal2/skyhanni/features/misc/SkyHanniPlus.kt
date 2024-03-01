package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.minecraft.client.Minecraft
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.random.Random

class SkyHanniPlus {
    private var plusSubscription = false
    private val delay: Long = 2000

    private val config get() = SkyHanniMod.feature.plus
    private var isBusy = false

    private val message1 = "§cA kick occurred in your connection, so you have been routed to limbo!"
    private val message2 = "Illegal characters in chat"

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.flipper.afterChange { enable() }
        config.rat.afterChange { enable() }
        config.lag.afterChange { enable() }
        config.fps.afterChange { enable() }
        config.luck.afterChange { enable() }
        config.cosmetics.highlight.afterChange { enable() }
    }

    @SubscribeEvent
    fun onLogin(event: HypixelJoinEvent) {
        if (!plusSubscription) return
        if (Random.nextInt(1, 5) > 1) return
        SkyHanniMod.coroutineScope.launch {
            isBusy = true
//             delay(delay + Random.nextInt(-500, 500 // i want to send to limbo to fake a staff ban, but auto limbo is maybe not a good idea?
//             ChatUtils.sendMessageToServer("§")
            delay(delay + Random.nextInt(-500, 500))
            showBan() //is auto disconnect a good idea??
            isBusy = false
        }
    }

    @SubscribeEvent
    fun onChatReceive(event: LorenzChatEvent) {
        if (!isBusy) return
        when (event.message) {
            message1 -> event.cancel()
            message2 -> event.cancel()
        }
        return
    }

    private fun enable() {
        if (config.disabled || plusSubscription) return
        plusSubscription = true
    }

    private fun showBan() {
        plusSubscription = false
        config.disabled = true
        val banID = generate()
//      copied from NEU!!!
        val component = ChatComponentText("\u00a7cYou are temporarily banned for \u00a7f29d 23h 59m 59s\u00a7c from this server!")
        component.appendText("\n")
        component.appendText("\n\u00a77Reason: \u00a7rCheating through the use of unfair game advantages")
        component.appendText("\n\u00a77Find out more: \u00a7b\u00a7nhttps://www.hypixel.net/appeal")
        component.appendText("\n")
        component.appendText("\n\u00a77Ban ID: \u00a7r#$banID")
        component.appendText("\n\u00a77Sharing your Ban ID may affect the processing of your appeal!")
        Minecraft.getMinecraft().netHandler.networkManager.closeChannel(component)
        return
    }

    private fun generate(): String {
        val charPool : List<Char> = ('A'..'Z') + ('0'..'9')
        return (1..8)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }
}
