package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.test.command.ErrorManager
import java.util.LinkedHashMap
import kotlin.math.abs

object ConnectFourUtils {

    class LRUCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > maxSize
        }
    }

    data class TTEntry(val value: Int, val upperBound: Boolean = false, val lowerBound: Boolean = false)

    data class C4Move(val player: LorenzColor, val column: Int)

    class C4Board(val width: Int = 7, val height: Int = 6) {
        private val boardState = LongArray(2)
        private val colHeights = IntArray(width) { it * (height + 1) }
        private val history = mutableListOf<Int>()
        var moves = 0
            private set

        private val bitShifts = listOf(1, height, height + 1, height + 2)
        private val baseSearchOrder = (0 until width).sortedBy { abs(width / 2 - it) }

        private fun getCurrentPlayer(): Int = moves and 1

        private fun getMask(): Long = boardState[0] or boardState[1]

        fun getKey(): Long = getMask() + boardState[getCurrentPlayer()]

        // If top cell for this column is known as (col*(height+1) + height - 1)
        private fun canPlay(col: Int): Boolean {
            val topCellBitIndex = col * (height + 1) + (height - 1)
            return (getMask() and (1L shl topCellBitIndex)) == 0L
        }

        fun play(col: Int) {
            val player = getCurrentPlayer()
            val move = 1L shl colHeights[col]
            boardState[player] = boardState[player] or move
            colHeights[col]++
            history.add(col)
            moves++
        }

        fun backtrack() {
            val col = history.removeAt(history.lastIndex)
            colHeights[col]--
            val move = 1L shl colHeights[col]
            boardState[getCurrentPlayer()] = boardState[getCurrentPlayer()] xor move
            moves--
        }

        fun winningBoardState(): Boolean {
            val playerState = boardState[getCurrentPlayer() xor 1]
            for (shift in bitShifts) {
                val test = playerState and (playerState shr shift)
                if (test and (test shr (2 * shift)) != 0L) return true
            }
            return false
        }

        fun getSearchOrder(): List<Int> = baseSearchOrder.filter { canPlay(it) }

        fun getScore(): Int = -(width * height + 1 - moves) / 2

        fun toBoardString(colored: Boolean = true): String {
            val board = StringBuilder()

            // Initialize a map from each column to its pieces (from bottom to top)
            val columnMap = mutableMapOf<Int, MutableList<LorenzColor?>>()
            for (col in 0 until width) {
                columnMap[col] = MutableList(height) { null }
            }

            // We'll track how many pieces have been placed in each column using a simple counter
            val pieceCountPerColumn = IntArray(width) { 0 }

            // Fill in the placed pieces from the history
            // Even index = Red, odd index = Yellow (based on your rules)
            for ((index, col) in history.withIndex()) {
                val player = if (index % 2 == 0) LorenzColor.RED else LorenzColor.YELLOW
                // Place the piece at the bottom-most available slot in that column
                // pieceCountPerColumn[col] gives the next free row (starting from 0 at the bottom)
                val placedHeight = pieceCountPerColumn[col]
                if (placedHeight >= height) {
                    // This would indicate an overfilled column, which should never happen if the game is valid.
                    ErrorManager.skyHanniError("Column $col is overfilled!")
                }
                columnMap.getOrPut(col) { mutableListOf() }
                columnMap[col]?.let {
                    it[placedHeight] = player
                }
                pieceCountPerColumn[col]++
            }

            // Build the visual board from top to bottom
            for (row in height - 1 downTo 0) {
                for (col in 0 until width) {
                    val cell = columnMap[col]?.get(row)
                    val char = when (cell) {
                        LorenzColor.RED -> 'R'
                        LorenzColor.YELLOW -> 'Y'
                        else -> '.'
                    }
                    val appendString = if (colored) "§${cell?.chatColorCode ?: "f"}$char" else "$char"
                    board.append(appendString).append(' ')
                }
                board.append('\n')
            }

            return board.toString().trimEnd()
        }

        companion object {
            fun fromMoveList(
                moves: List<C4Move>,
                width: Int = 7,
                height: Int = 6,
            ): C4Board = C4Board(width = width, height = height).also {
                moves.forEach { move -> it.play(move.column) }
            }.also { return it }
        }
    }

    fun solve(board: C4Board): Int {
        val lruCache = LRUCache<Long, TTEntry>(4096)

        fun recurse(alpha: Int, beta: Int): Int {
            var localAlpha = alpha
            var mBeta = beta

            // Transposition table lookup
            lruCache[board.getKey()]?.let { entry ->
                if (entry.lowerBound) localAlpha = maxOf(localAlpha, entry.value)
                if (entry.upperBound) mBeta = minOf(mBeta, entry.value)
                if (localAlpha >= mBeta) return entry.value
            }

            // Base cases
            if (board.winningBoardState()) return board.getScore()
            if (board.moves == board.width * board.height) return 0

            var value = -board.width * board.height
            for (col in board.getSearchOrder()) {
                board.play(col)
                value = maxOf(value, -recurse(-mBeta, -localAlpha))
                board.backtrack()
                localAlpha = maxOf(localAlpha, value)
                if (localAlpha >= mBeta) break
            }

            // Transposition table storage
            lruCache[board.getKey()] = when {
                value <= alpha -> TTEntry(value, upperBound = true)
                value >= mBeta -> TTEntry(value, lowerBound = true)
                else -> TTEntry(value)
            }

            return value
        }

        return recurse(Int.MIN_VALUE, Int.MAX_VALUE)
    }
}
