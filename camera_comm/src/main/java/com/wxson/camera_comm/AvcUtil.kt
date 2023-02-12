package com.wxson.camera_comm

import android.util.Log
import com.wxson.camera_comm.CommonTools.byteBufferToByteArray
import com.wxson.camera_comm.CommonTools.bytesToHex
import java.nio.ByteBuffer

/**
 * @author wxson
 * @date 2022/10/23
 * @apiNote
 */
object AvcUtil {
    private val tag = this.javaClass.simpleName
    private const val START_PREFIX_CODE = 0x00000001
    //private const val START_PREFIX_LENGTH = 4
    //private const val NAL_UNIT_HEADER_LENGTH = 1
    //private const val NAL_TYPE_CODED_SLICE = 0x01
    //private const val NAL_TYPE_CODED_SLICE_IDR = 0x05
    //private const val NAL_TYPE_SEI = 0x06
    private const val NAL_TYPE_SPS = 0x07
    //private const val NAL_TYPE_PPS = 0x08
    //private const val NAL_TYPE_SUBSET_SPS = 0x0f

    fun goToPrefix(buffer: ByteBuffer): Boolean {
        var presudoPrefix = 0x0fffffff
        while (buffer.hasRemaining()) { // 在buffer内寻找START_PREFIX_CODE(起始码)
            presudoPrefix = (presudoPrefix shl 8) or (buffer.get().toInt() and 0xff)
            if (presudoPrefix == START_PREFIX_CODE) {
                return true     // 找到START_PREFIX_CODE
            }
        }
        return false            // 未找到START_PREFIX_CODE
    }

    //private fun getNalType(buffer: ByteBuffer): Int {
    //    return buffer.get().toInt() and 0x1f
    //}
    //
    //private fun getGolombUE(bitBufferLite: BitBufferLite): Int {
    //    var leadingZeroBits = 0
    //    while (!bitBufferLite.getBit()) {
    //        leadingZeroBits++
    //    }
    //    val suffix: Int = bitBufferLite.getBits(leadingZeroBits)
    //    val minimum = (1 shl leadingZeroBits) - 1
    //    return minimum + suffix
    //}

    ////TODO: need support extra profile_idc and pic_order_cnt_type
    ////usage: Int[] width = new Int[1];
    ////sps should contains 00 00 00 01 67 ......
    //fun parseSPS(
    //    sps: ByteArray,     // in
    //    width: IntArray,    // out
    //    height: IntArray    // out
    //) //sps buffer doesn't include nal-type byte
    //{
    //    val byteBuffer = ByteBuffer.wrap(sps)
    //    if (!goToPrefix(byteBuffer) || NAL_TYPE_SPS != getNalType(byteBuffer))
    //        return      // byteBuffer内未找到NAL_TYPE_SPS
    //    val bitBufferLite = BitBufferLite(byteBuffer)
    //    val profileIdc = bitBufferLite.getBits(8) //profile idc
    //    bitBufferLite.getBits(16) //constraint_set0...,
    //    getGolombUE(bitBufferLite)
    //    if (profileIdc == 100 || profileIdc == 110 || profileIdc == 122 || profileIdc == 244 ||
    //        profileIdc == 44 || profileIdc == 83 || profileIdc == 86 || profileIdc == 118 || profileIdc == 128) {
    //        Log.e("AvcUtils", "SPS parsing do not support such profile idc, $profileIdc")
    //        throw UnsupportedOperationException("Profile idc NOT supported yet.")
    //    }
    //    val log2_max_frame_num_minus4: Int = getGolombUE(bitBufferLite)
    //    val picOrderCntType: Int = getGolombUE(bitBufferLite)
    //    if (picOrderCntType == 0) {
    //        val log2_max_pic_order_cnt_lsb_minus4: Int = getGolombUE(bitBufferLite)
    //    } else if (picOrderCntType == 1) {
    //        Log.e("AvcUtils", "SPS parsing do not support such pic_order_cnt_type, $picOrderCntType")
    //        throw UnsupportedOperationException("pic_order_cnt_type NOT supported yet.")
    //    } else {
    //        //pic_order_cnt_type shall be "2", do nothing
    //    }
    //    val num_ref_frames: Int = getGolombUE(bitBufferLite)
    //    val gaps_in_frame_num_value_allowed_flag = bitBufferLite.getBits(1) //1 bit
    //
    //    //KEY POINT
    //    val picWidthInMbsMinus1: Int = getGolombUE(bitBufferLite)
    //    width[0] = (picWidthInMbsMinus1 + 1) * 16
    //    val picHeightInMapUnitsMinus1: Int = getGolombUE(bitBufferLite)
    //    height[0] = (picHeightInMapUnitsMinus1 + 1) * 16
    //
    //    //over
    //    return
    //}


    fun getSps(buffer: ByteBuffer): ByteArray {
        buffer.clear()  //重置byteBuffer各标志，并非清除内容。
        var i = 5
        while (i < buffer.capacity()) {
            if (buffer[i].toInt() and 0xff == 0) {    //sps head以后遇到0结束
                break
            }
            i++
        }
        val returnValue = ByteArray(i)
        buffer.get(returnValue, 0, i)
        return returnValue
    }

    fun getPps(buffer: ByteBuffer): ByteArray {
        val len = buffer.capacity() - buffer.position()
        val returnValue = ByteArray(len)
        buffer.get(returnValue)
        return returnValue
    }

    /**
     * get csd from video ByteBuffer for decode
     * @param byteBuffer video ByteBuffer
     * @return ByteArray? csd
     */
    fun getCsd(byteBuffer: ByteBuffer): ByteArray? {
        val csd: ByteArray?
        // ************* debug *************************
        //val byteArray = byteBufferToByteArray(byteBuffer)
        //val hexString = bytesToHex(byteArray)
        //Log.i(tag, "ByteBuffer: {$hexString}")
        // ************* debug *************************
        // for h264
        if ((byteBuffer[4].toInt() and 0x1f) == NAL_TYPE_SPS) { //如果byteBuffer第四个字节的后5位等于7
            csd = ByteArray(byteBuffer.remaining())
            byteBuffer.get(csd)
            Log.i(tag, "getCsd for h264 csd=" + bytesToHex(csd))
        } else {
            //for h265
            val nalType: Int = (byteBuffer[4].toInt() shr 1) and 0x3f
            if (nalType == 32) {
                csd = ByteArray(byteBuffer.remaining())
                byteBuffer.get(csd)
                Log.i(tag, "getCsd for h265 csd=" + bytesToHex(csd))
            } else {
                csd = null
                //Log.i(tag, "csd= null")
            }
        }
        return csd
    }
}