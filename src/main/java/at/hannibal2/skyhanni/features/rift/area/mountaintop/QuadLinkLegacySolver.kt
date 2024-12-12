package at.hannibal2.skyhanni.features.rift.area.mountaintop

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.render.gui.ReplaceItemEvent
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ConnectFourUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.setLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzColor.Companion.toLorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object QuadLinkLegacySolver {

    private val devConfig get() = SkyHanniMod.feature.dev.debug
    private val config get() = RiftAPI.config.area.mountaintop.quadLinkLegacy
    private val patternGroup = RepoPattern.group("rift.area.mountaintop.quadlink")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: §cRed: oBlazin
     * REGEX-TEST: §aGreen: Wizardman
     */
    private val colorIndicatorPattern by patternGroup.pattern(
        "color.indicator",
        "§(?<colorchar>.)(?<colorname>.*): (?<player>[a-zA-Z0-9_]+)"
    )

    /**
     * REGEX-TEST: §cRed
     * REGEX-TEST: §aGreen
     */
    private val indiscriminatePiecePattern by patternGroup.pattern(
        "piece.indiscriminate",
        "^§.[A-Z][a-z]+$"
    )

    /**
     * REGEX-TEST: §aClick here!
     */
    private val myTurnPiecePattern by patternGroup.pattern(
        "piece.myturn",
        "§aClick here!"
    )

    /**
     * REGEX-TEST: §cIt's not your turn!
     */
    private val notMyTurnPiecePattern by patternGroup.pattern(
        "piece.notmyturn",
        "§cIt's not your turn!"
    )

    /**
     * REGEX-TEST: §aWinning move!
     */
    private val winningPieceLorePattern by patternGroup.pattern(
        "piece.winning",
        "§aWinning move!"
    )

    /**
     * REGEX-TEST: Quad Link Legacy - Wizardman
     */
    private val quadLinkLegacyInventoryPattern by patternGroup.pattern(
        "inventory",
        "Quad Link Legacy - (?<player>[a-zA-Z0-9_]+)"
    )
    // </editor-fold>

    private const val OPPONENT_PIECE_INDICATOR_SLOT = 18
    private const val MY_PIECE_INDICATOR_SLOT = 26

    private var currentBoardProp: ConnectFourUtils.C4Board? = null
    private var recommendationItemStack: ItemStack? = null

    private var myColor: LorenzColor? = null
    private var opponentColor: LorenzColor? = null
    private var currentTurn: LorenzColor? = null
    private var recommendedColumnMove: Int? = null

    // List of column moves made by each player
    private val moves = mutableListOf<ConnectFourUtils.C4Move>()
    // Cache of current game state for easy comparison
    private val currentBoardCache: MutableMap<Int, LorenzColor?> = emptyBoardCache()

    private fun resetState() {
        myColor = null
        opponentColor = null
        currentTurn = null
        currentBoardProp = null
        recommendedColumnMove = null
        moves.clear()
        resetBoardCache()
    }

    private fun emptyBoardCache(): MutableMap<Int, LorenzColor?> = (0..53).associateWith { null }.toMutableMap()
    private fun resetBoardCache() = currentBoardCache.replaceAll { _, _ -> null }

    // We want to filter out anything on the sides when trying to read the game board
    private val excludedKeys = (0..53).filter { it % 9 == 0 || it % 9 == 8 }.toSet()
    private fun ItemStack.isGamePiece() = indiscriminatePiecePattern.matches(displayName.orEmpty())
    private fun Map<Int, ItemStack>.commonFilter() = filter { it.key !in excludedKeys && it.value.hasDisplayName() }
    private fun Map<Int, ItemStack>.filterGamePieces() = commonFilter().filter { it.value.isGamePiece() }
    private fun Map<Int, ItemStack>.filterNotGamePieces() = commonFilter().filter { !it.value.isGamePiece() }
    private fun Map<Int, ItemStack>.getCurrentTurn(): LorenzColor? = filterNotGamePieces().values.firstOrNull()?.getPlayer()

    private fun ItemStack.getPlayer(): LorenzColor? = displayName?.let {
        val myColor = myColor ?: ErrorManager.skyHanniError("Player colors not initialized")
        val opponentColor = opponentColor ?: ErrorManager.skyHanniError("Player colors not initialized")

        // Check if the piece is a player's placed piece
        it.toLorenzColor(failOnError = false)?.let { color -> return color }

        if (myTurnPiecePattern.matches(it)) myColor
        else if (notMyTurnPiecePattern.matches(it)) opponentColor
        else ErrorManager.skyHanniError("Failed to read player color")
    }

    private fun InventoryOpenEvent.readPlayerColors() {
        if (opponentColor != null && myColor != null) return
        opponentColor = extractColor(inventoryItems[OPPONENT_PIECE_INDICATOR_SLOT]?.displayName ?: return)
        myColor = extractColor(inventoryItems[MY_PIECE_INDICATOR_SLOT]?.displayName ?: return)
    }

    private fun InventoryOpenEvent.someoneWon(): Boolean = inventoryItems.filterGamePieces().values.any {
        winningPieceLorePattern.matches(it.getLore().firstOrNull().orEmpty())
    }

    private fun regenerateBoardProp() {
        val moves = moves.takeIf { it.size > 0 } ?: return
        currentBoardProp = ConnectFourUtils.C4Board.fromMoveList(moves)
    }

    private fun regenerateNextRecommendedMove() {
        recommendedColumnMove = if (currentTurn == myColor) getNextMove() else null
    }

    private fun extractColor(piece: String): LorenzColor? {
        return colorIndicatorPattern.matchMatcher(piece) {
            group("colorchar")?.let { it[0].toLorenzColor() }
                ?: group("colorname")?.let { LorenzColor.valueOf(it.uppercase()) }
                ?: ErrorManager.skyHanniError("Failed to read color")
        }
    }

    private fun Int.getColumn(): Int = this % 9
    private fun Pair<Int, ItemStack>.getColumn(): Int = first.getColumn()

    private fun getNextMove(): Int? {
        val board = currentBoardProp ?: return null
        val validColumns = board.getSearchOrder().takeIf {
            it.isNotEmpty()
        } ?: ErrorManager.skyHanniError("No valid moves available")

        // Find the best column to play
        return validColumns.maxByOrNull { column ->
            board.play(column)
            val score = -ConnectFourUtils.solve(board)
            board.backtrack()
            score
        }
    }

    private fun generateRecommendationItemStack(base: ItemStack) {
        recommendationItemStack = base.copy().apply {
            setStackDisplayName("§aRecommended Move")
            addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 0)
            setLore(
                getLore().toMutableList().apply { add(0, "§8(From SkyHanni)") }
            )
        }
    }

    @HandleEvent
    fun replaceItem(event: ReplaceItemEvent) {
        if (!config.enabled || !quadLinkLegacyInventoryPattern.matches(event.inventory.name)) return
        if (event.slot in excludedKeys || event.slot.getColumn() != recommendedColumnMove) return

        val originalItem = event.originalItem.takeIf { !it.isGamePiece() } ?: return
        if (originalItem.getPlayer() != myColor) return

        recommendationItemStack?.let {
            event.replace(it)
        } ?: generateRecommendationItemStack(originalItem)
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        resetState()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!devConfig.connect4Debug) return
        if (!quadLinkLegacyInventoryPattern.matches(InventoryUtils.openInventoryName())) return

        devConfig.connect4DebugPosition.renderRenderable(
            Renderable.verticalContainer(
                listOf(
                    Renderable.string("My Color: ${myColor ?: "N/A"}"),
                    Renderable.string("Opponent Color: ${opponentColor ?: "N/A"}"),
                    Renderable.string("Current Turn: ${currentTurn ?: "N/A"}"),
                    Renderable.string("Moves: ${moves.size}"),
                    Renderable.string("Current Board: ${currentBoardProp?.getKey() ?: "N/A"}"),
                    Renderable.string("Recommended Move (Column): $recommendedColumnMove"),
                    Renderable.verticalContainer(
                        buildList {
                            add(Renderable.string("Current board:"))
                            addAll(
                                currentBoardProp?.toBoardString()?.split("\n")?.map {
                                    Renderable.string(it)
                                }.orEmpty()
                            )
                        }
                    )
                )
            ),
            posLabel = "Connect4 Debug Info"
        )
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: InventoryUpdatedEvent) {
        if (!config.enabled || !quadLinkLegacyInventoryPattern.matches(event.inventoryName)) return

        event.readPlayerColors()
        if (event.someoneWon()) {
            resetState()
            return
        }

        val currentBoard = event.inventoryItems.filterGamePieces().takeIf {
            // We only want to consider new moves
            //  Because the new move from the opponent is "flashed" when it is played, we need to do an explicit
            //  greater than check, as a not equal check would false flag on the "animation"
            it.size > moves.size
        } ?: return

        val changedItem = currentBoard.filter { currentBoardCache[it.key] == null }.takeIf {
            it.size == 1 // We only want to consider single moves
        }?.toList()?.first() ?: ErrorManager.skyHanniError(
            "More than one move detected, skipping",
            "currentBoardKeys" to currentBoard.keys.toString(),
            "currentBoardCacheKeys" to currentBoardCache.keys.toString()
        )

        val newMove = ConnectFourUtils.C4Move(
            player = changedItem.second.getPlayer() ?: ErrorManager.skyHanniError("Failed to read player"),
            column = changedItem.getColumn()
        )
        moves.add(newMove)
        currentBoardCache[changedItem.first] = newMove.player
        currentTurn = event.inventoryItems.getCurrentTurn()
        regenerateBoardProp()
        regenerateNextRecommendedMove()
    }
}
