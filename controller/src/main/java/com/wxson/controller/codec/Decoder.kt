package com.wxson.controller.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.wxson.camera_comm.H265Format

/**
 * @author wxson
 * @date 2022/12/4
 * @apiNote
 */
class Decoder private constructor(mediaCodecCallback: MediaCodecCallback) {
    private val tag = this.javaClass.simpleName
    private val videoCodecMime = MediaFormat.MIMETYPE_VIDEO_HEVC
    private val mediaCodec = MediaCodec.createDecoderByType(videoCodecMime) // 创建H265格式视频解码器

    init {
        Log.i(tag, "init")
        // Set up Callback for the decoder
        mediaCodec.setCallback(mediaCodecCallback)
    }

    fun configure(imageSize: Size, csd: ByteArray, surface: Surface) {
        //set up input mediaFormat
        val codecFormat = H265Format.getDecodeFormat(imageSize, csd)
        // configure mediaCodec
        mediaCodec.configure(codecFormat, surface, null, 0)
    }

    fun start() {
        // 启动解码器
        mediaCodec.start()
        Log.i(tag, "mediaCodec started")
    }

    fun Release() {
        mediaCodec.stop()
        mediaCodec.release()
    }

    companion object {
        @Volatile private var instance: Decoder? = null
        fun getInstance(mediaCodecCallback: MediaCodecCallback) =
            instance ?: synchronized(this) {
                instance ?: Decoder(mediaCodecCallback).also { instance = it }
            }
    }
}