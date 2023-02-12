package com.wxson.camera_comm

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.wxson.camera_comm.AvcUtil.getPps
import com.wxson.camera_comm.AvcUtil.getSps
import com.wxson.camera_comm.AvcUtil.goToPrefix
import java.nio.ByteBuffer

/**
 * @author wxson
 * @date 2022/12/30
 * @apiNote
 */
object H264Format {

    private val tag = this.javaClass.simpleName
    private const val mime = MediaFormat.MIMETYPE_VIDEO_AVC

    fun getEncodeFormat(imageSize: Size) : MediaFormat {
        val width = imageSize.width
        val height = imageSize.height
        val frameRate = 30
        val frameInterval = 0   //每一帧都是关键帧
        val bitRateFactor = 14
        val encodeFormat = MediaFormat.createVideoFormat(mime, width, height)
        encodeFormat.apply {
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * bitRateFactor)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval)
        }
        return encodeFormat
    }

    fun getDecodeFormat(imageSize: Size, csd: ByteArray): MediaFormat? {
        val width = imageSize.width
        val height = imageSize.height
        //分割spsPps
        val csdByteBuffer = ByteBuffer.wrap(csd)
        //if (csdByteBuffer == null) {
        //    Log.e(tag, "getDecodeFormat csd is null")
        //    return null
        //}
        csdByteBuffer.clear()
        if (!goToPrefix(csdByteBuffer)) {
            // 未找到 Prefix 0x01
            Log.e(tag, "getDecodeFormat Prefix error")
            return null
        }
        val headerSps: ByteArray = getSps(csdByteBuffer)
        val headerPps: ByteArray = getPps(csdByteBuffer)

        val decodeFormat = MediaFormat.createVideoFormat(mime, width, height)
        decodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height)
        decodeFormat.setByteBuffer("csd-0", ByteBuffer.wrap(headerSps))
        decodeFormat.setByteBuffer("csd-1", ByteBuffer.wrap(headerPps))

        return decodeFormat

    }
}