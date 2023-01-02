package com.wxson.camera_comm

import java.nio.ByteBuffer

/**
 * @author wxson
 * @date 2022/10/24
 * @apiNote
 */
class BitBufferLite(private var buffer: ByteBuffer) {
    private var availableBits = 0
    private var restBits = 0

    fun getBit(): Boolean {
        return getBits(1) != 0
    }

    fun getBits(nBits: Int): Int {
        require(!(nBits < 0 || nBits > 32))
        if (nBits == 0) {
            return 0
        }
        var bits = restBits.toLong()
        var collected = availableBits
        while (collected < nBits) {
            bits = (bits shl 8) or (buffer.get().toLong() and 0xFF)
            collected += 8
        }
        availableBits = collected - nBits
        assert(availableBits < 8)
        val result = (bits shr availableBits).toInt()
        restBits = (bits and ((1 shl availableBits) - 1).toLong()).toInt()
        return result
    }
}