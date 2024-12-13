package at.hannibal2.skyhanni.utils

import java.util.LinkedHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * This entire file is a Kotlin port of a python implementation by lhorrell99
 * https://github.com/lhorrell99/connect-4-solver/tree/master
 */

object ConnectFourUtils {

    data class LRUCache<K, V>(private val maxSize: Int) : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
            return size > maxSize
        }
    }

    data class TTEntry(val value: Int, val upperBound: Boolean = false, val lowerBound: Boolean = false)

    data class C4Move(val player: LorenzColor, val column: Int)

    data class C4Board(val boardWidth: Int = 7, val boardHeight: Int = 6) {
        val boardSize = boardWidth * boardHeight
        private val boardState = LongArray(2)
        private val columnHeights = IntArray(boardWidth) { it * (boardHeight + 1) }
        val history = mutableListOf<Int>()
        val moves get() = history.size
        private val baseSearchOrder =  (0 until boardWidth).sortedBy { abs(boardWidth / 2 - it) }
        private val bitShifts = listOf(
            1, /** | Vertical */
            boardHeight, /** \ Diagonal */
            boardHeight + 1, /** - Horizontal */
            boardHeight + 2 /** / Diagonal */
        )

        // Returns current player: 0 or 1 (0 always plays first)
        private fun getCurrentPlayer(): Int = moves and 1

        // Returns opponent to current player: 0 or 1
        private fun getOpponentPlayer(): Int = (moves + 1) and 1

        // Returns column search order containing playable columns only
        fun getSearchOrder(): List<Int> = baseSearchOrder.filter {
            canPlay(it)
        }.sortedBy { colSort(it) }.reversed()

        // Returns bitstring of all occupied positions
        private fun getMask(): Long = boardState[0] or boardState[1]

        // Returns unique game state identifier
        fun getKey(): Long = getMask() + boardState[getCurrentPlayer()]

        // Returns true if col (zero indexed) is playable
        private fun canPlay(col: Int): Boolean {
            val topCellBitIndex = (boardHeight + 1) * col + (boardHeight - 1)
            return (getMask() and (1L shl topCellBitIndex)) == 0L
        }

        // Play a move in col (zero indexed)
        fun play(col: Int) {
            val player = getCurrentPlayer()
            val move = 1L shl columnHeights[col]
            columnHeights[col]++
            boardState[player] = boardState[player] or move
            history.add(col)
        }

        // Backtrack one move
        fun backtrack() {
            val opponent = getOpponentPlayer()
            val col = history.removeLast()
            columnHeights[col]--
            val move = 1L shl columnHeights[col]
            boardState[opponent] = boardState[opponent] xor move
        }

        // Returns true if last played column creates winning alignment
        fun winningBoardState(): Boolean {
            val opponent = getOpponentPlayer()
            for (shift in bitShifts) {
                val test = boardState[opponent] and boardState[opponent] shr shift
                if (test and (test shr (2 * shift)) != 0L) return true
            }
            return false
        }

        // Returns score of complete game (evaluated for winning opponent)
        fun getScore(): Int = -((boardWidth * boardHeight) + 1 - moves) / 2

        // Returns heuristic value for column
        private fun colSort(col: Int): Int {
            val player = getCurrentPlayer()
            val move = 1L shl columnHeights[col]
            var count = 0
            val state = boardState[player] or move

            for (shift in bitShifts) {
                val test = state and (state shr shift) and (state shr (2 * shift))
                if (test != 0L) count += test.countOneBits()
            }

            return count
        }

        companion object {
            fun fromMoveList(
                moves: List<C4Move>,
                width: Int = 7,
                height: Int = 6,
            ): C4Board {
                val board = C4Board(width, height)
                moves.forEach { board.play(it.column) }
                return board
            }
        }
    }

    fun solve(board: C4Board): Int {
        val lruCache = LRUCache<Long, TTEntry>(4096)

        fun recurse(alpha: Int, beta: Int): Int {
            var localAlpha = alpha
            var localBeta = beta

            // Transposition table lookup
            lruCache[board.getKey()]?.let { entry ->
                if (entry.lowerBound) localAlpha = max(localAlpha, entry.value) // Lower bound (TT)
                else if (entry.upperBound) localBeta = min(localBeta, entry.value) // Upper bound (TT)
                else return entry.value // Exact value (TT)

                if (localAlpha >= localBeta) return entry.value // Cut off (TT)
            }

            // Negamax implementation
            if (board.winningBoardState()) return board.getScore() // Winning alignment
            else if (board.moves == board.boardSize) return 0 // Drawn game

            var value = -(board.boardSize)
            for (column in board.getSearchOrder()) {
                board.play(column)
                value = max(value, -recurse(-localBeta, -localAlpha))
                board.backtrack()
                localAlpha = max(localAlpha, value)
                if (localAlpha >= localBeta) break // Alpha cut-off
            }

            // Transposition table storage
            lruCache[board.getKey()] = when {
                value <= alpha -> TTEntry(value, upperBound = true)
                value >= localBeta -> TTEntry(value, lowerBound = true)
                else -> TTEntry(value)
            }

            return value
        }

        return recurse(Int.MIN_VALUE, Int.MAX_VALUE)
    }
}
