package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.dungeon.DragPrioConfig
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.EnumParticleTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

class DungeonDragonPriority {

    private val config get() = SkyHanniMod.feature.dungeon.dragPrio

    private val startPattern by RepoPattern.pattern(
        "dungeons.startphase5",
        "(.+)§r§a picked the §r§cCorrupted Blue Relic§r§a!"
    )

    enum class DragonInfo(val color: String, var hasSpawned: Boolean, val isEasy: Boolean, val priority: IntArray) {
        POWER("Red", false, false, intArrayOf(1, 3)),
        FLAME("Orange", false, true, intArrayOf(2, 1)),
        ICE("Blue", false, false, intArrayOf(3, 4)),
        SOUL("Purple", false, true, intArrayOf(4, 5)),
        APEX("Green", false, true, intArrayOf(5, 2)),
        NONE("None", false, false, intArrayOf(0, 0));

        companion object {
            fun clearSpawned() {
                entries.forEach{ it.hasSpawned = false }
            }
        }
    }

    private var inBerserkTeam = false
    private var inArcherTeam = false
    private var isHealer = false
    private var isTank = false

    private var isSearching = false
    private val particleList = mutableListOf<String>()

    private var dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (!startPattern.matches(event.message)) return
        reset()
        ChatUtils.debug("starting p5")
        startP5()
    }

    @SubscribeEvent
    fun onDungeon(event: DungeonStartEvent) {
        if (DungeonAPI.dungeonFloor != "F1") return
        reset()
        ChatUtils.chat("searching")
        isSearching = true
    }

    private fun startP5() {
        if (!config.saySplit) return
        val currentClass = DungeonAPI.playerClass
        when (currentClass) {
            DungeonAPI.DungeonClass.MAGE -> inBerserkTeam = true
            DungeonAPI.DungeonClass.BERSERK -> inBerserkTeam = true
            DungeonAPI.DungeonClass.ARCHER -> inArcherTeam = true
            DungeonAPI.DungeonClass.TANK -> {
                inArcherTeam = true
                isTank = true
            }

            DungeonAPI.DungeonClass.HEALER -> isHealer = true
            else -> return
        }
        DelayedRun.runDelayed(2000.milliseconds) {
            val currentPower = getPower()
            when {
                currentPower >= config.splitPower -> {
                    ChatUtils.chat("Power: $currentPower | Split on all drags!")
                    return@runDelayed
                }

                currentPower >= config.easyPower -> {
                    ChatUtils.chat("Power: $currentPower | Split on easy drags!")
                    return@runDelayed
                }

                else -> {
                    ChatUtils.chat("Power: $currentPower | No split!")
                    return@runDelayed
                }
            }
        }
        ChatUtils.chat("searching")
        isSearching = true
    }

    private fun reset() {
        DragonInfo.clearSpawned()
        dragonOrder = arrayOf(DragonInfo.NONE, DragonInfo.NONE)
        inArcherTeam = false
        inBerserkTeam = false
        isHealer = false
        isTank = false
        particleList.clear()
        particleList.add("```")
    }

    private fun checkCoordinates(particle: Packet<*>) {
        if (particle !is S2APacketParticles) return
        val vec = particle.toLorenzVec()
        val x = vec.x.toInt()
        val y = vec.y.toInt()
        val z = vec.z.toInt()
        if (y !in 14..19) return
        when (x) {
            in 27..32 -> {
                when (z) {
                    in 54..64 -> trySpawnDragon(DragonInfo.POWER)
                    in 89..99 -> trySpawnDragon(DragonInfo.APEX)
                }
            }
            in 79..85 -> {
                when (z) {
                    in 54..64 -> trySpawnDragon(DragonInfo.FLAME)
                    in 89..99 -> trySpawnDragon(DragonInfo.ICE)
                }
            }
            in 51..61 -> trySpawnDragon(DragonInfo.SOUL)
        }
    }

    private fun trySpawnDragon(dragon: DragonInfo) {
        if (dragon.hasSpawned) return
        ChatUtils.chat("try spawning ${dragon.name}")
        dragon.hasSpawned = true
        assignDrag(dragon)
        DelayedRun.runDelayed(8000.milliseconds) {
            dragon.hasSpawned = false
        }
    }

    private fun assignDrag(dragon: DragonInfo) {
        when (DragonInfo.NONE) {
            dragonOrder[0] -> {
                ChatUtils.chat("${dragon.name} is now dragon0")
                dragonOrder[0] = dragon
            }
            dragonOrder[1] -> {
                ChatUtils.chat("${dragon.name} is now dragon1")
                dragonOrder[1] = dragon
                isSearching = false
                determinePriority()
            }
            else -> return
        }
        if (config.showSingleDragons) {
            ChatUtils.chat("${dragon.color} is spawning")
        }
    }

    private fun determinePriority() {
        val normalDrag = if (dragonOrder[0].priority[0] < dragonOrder[1].priority[0]) {
            dragonOrder[0]
        } else dragonOrder[1]
        val power = getPower()
        var split = 0
        val isEasy: Boolean = dragonOrder[0].isEasy && dragonOrder[1].isEasy
        ChatUtils.chat("isEasy: $isEasy, ${dragonOrder[0].isEasy}, ${dragonOrder[1].isEasy}")
        when {
            power >= config.splitPower -> split = 1
            isEasy && power >= config.easyPower -> split = 1
        }
        val berserkDragon: DragonInfo
        val archerDragon: DragonInfo
        if (dragonOrder[0].priority[split] < dragonOrder[1].priority[split]) {
            berserkDragon = dragonOrder[0]
            archerDragon = dragonOrder[1]
        } else {
            berserkDragon = dragonOrder[1]
            archerDragon = dragonOrder[0]
        }
        displayDragons(berserkDragon, archerDragon, normalDrag, split)
        ChatUtils.chat("${berserkDragon.name}, ${archerDragon.name}, ${normalDrag.name}, $split") //remove later
    }

    private fun displayDragons(
        berserkDragon: DragonInfo,
        archerDragon: DragonInfo,
        normalDrag: DragonInfo,
        split: Int
    ) {
        val purple = DragonInfo.SOUL in dragonOrder
        if (split == 1) {
            ChatUtils.chat("Berserk Team: ${berserkDragon.color}")
            ChatUtils.chat("Archer Team: ${archerDragon.color}")
        }
        if (split == 0) {
            ChatUtils.chat("${normalDrag.color} is spawning!")
        } else {
            if (inBerserkTeam || (purple && (
                        (isHealer && config.healerPurple == DragPrioConfig.HealerPurpleValue.BERSERK)
                                || (isTank && config.tankPurple == DragPrioConfig.TankPurpleValue.BERSERK)))
            ) {
                ChatUtils.chat("$berserkDragon is Spawning!!")
            } else {
                ChatUtils.chat("$archerDragon is Spawning!!")
            }
        }
    }

    @SubscribeEvent
    fun onDungeonEnd(event: DungeonCompleteEvent) {
        if (isSearching) ChatUtils.chat("no longer searching")
        isSearching = false
        particleList.add("```")
        val output = particleList.joinToString ("\n")
        ChatUtils.clickableChat("click here to give me all the particles",
            { OSUtils.copyToClipboard(output); ChatUtils.chat("copied")} )
    }

    @SubscribeEvent
    fun onParticle(event: PacketEvent.ReceiveEvent) {
        if (event.packet !is S2APacketParticles) return
        if (!isSearching) return
        if (!DungeonAPI.inDungeon()) return
        if (DungeonAPI.dungeonFloor != "M7") return
        if (event.packet.particleType != EnumParticleTypes.ENCHANTMENT_TABLE) return
        particleList.add("${event.packet.toLorenzVec()}, type: ${event.packet.particleType}")
        checkCoordinates(event.packet)
    }

    @SubscribeEvent
    fun onDebugCollect(event: DebugDataCollectEvent) {
        event.title("DragPrio")
        if (!LorenzUtils.inDungeons) {
            event.addIrrelevant("not in dungeons")
            return
        }
        if (DungeonAPI.dungeonFloor != "M7") {
            event.addIrrelevant("not in m7")
            return
        }

        event.addData {
            add("Power: ${getPower()}")
            add("inArchTeam: $inArcherTeam")
            add("inBersTeam: $inBerserkTeam")
            add("isHealer: $isHealer")
            add("isTank: $isTank")
            add("isSearching: $isSearching")
            add("dragon0: ${dragonOrder[0].name}")
            add("dragon1: ${dragonOrder[1].name}")
        }
    }

    private fun getPower(): Double = DungeonAPI.DungeonBlessings.valueOf("POWER").power +
            (DungeonAPI.DungeonBlessings.valueOf("TIME").power.toDouble() / 2)
}