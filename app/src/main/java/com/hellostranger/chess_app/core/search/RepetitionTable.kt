package com.hellostranger.chess_app.core.search

import com.hellostranger.chess_app.core.board.Board

@ExperimentalUnsignedTypes
class RepetitionTable {
    private val hashes : ULongArray = ULongArray(256)
    private val startIndices : IntArray = IntArray(hashes.size + 1)
    private var count : Int = 0

    fun initialize(board: Board) {
        val initialHashes : ULongArray = board.repetitionPositionHistory!!.reversed().toULongArray()
        count = initialHashes.size

        for (i in 0 until count) {
            hashes[i] = initialHashes[i]
            startIndices[i] = 0
        }
        startIndices[count] = 0
    }

    fun push(hash : ULong, reset : Boolean) {
        if (count < hashes.size) {
            hashes[count] = hash
            startIndices[count + 1] = if (reset) count else startIndices[count]
        }
        count++
    }

    fun tryPop() {
        count = (count - 1).coerceAtLeast(0)
    }

    fun contains(h : ULong) : Boolean {
        val s = startIndices[count]
        for (i in s until count -1 ) {
            if (hashes[i] == h) {
                return true
            }
        }
        return false
    }


}