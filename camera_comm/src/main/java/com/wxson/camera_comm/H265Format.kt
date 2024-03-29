package com.wxson.camera_comm

import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Size
import java.nio.ByteBuffer

/**
 * @author wxson
 * @date 2022/10/22
 * @apiNote
 */
object H265Format {
    private const val mime = MediaFormat.MIMETYPE_VIDEO_HEVC

    fun getEncodeFormat(imageSize: Size) : MediaFormat {
        val width = imageSize.width
        val height = imageSize.height
        val frameRate = 30
        val frameInterval = 0   //每一帧都是关键帧
        val bitRateFactor = 10
        val encodeFormat = MediaFormat.createVideoFormat(mime, width, height)
        encodeFormat.apply {
            setInteger(MediaFormat.KEY_BIT_RATE, width * height * bitRateFactor)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, frameInterval)
        }
        return encodeFormat
    }

    fun getDecodeFormat(imageSize: Size, csd: ByteArray): MediaFormat {
        val width = imageSize.width
        val height = imageSize.height
        val decodeFormat = MediaFormat.createVideoFormat(mime, width, height)
        decodeFormat.apply {
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, width * height)
            setByteBuffer("csd-0", ByteBuffer.wrap(csd))
        }
        return decodeFormat
    }
}