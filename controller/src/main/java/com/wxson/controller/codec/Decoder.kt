package com.wxson.controller.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.wxson.camera_comm.H264Format
import com.wxson.camera_comm.H265Format

/**
 * @author wxson
 * @date 2022/12/4
 * @apiNote
 */
class Decoder(private val mediaCodecCallback: MediaCodecCallback, private val surface: Surface) {
    private val tag = this.javaClass.simpleName
    private var mediaCodec: MediaCodec? = null

    fun configure(mime: String, imageSize: Size, csd: ByteArray) {
        //set up input mediaFormat
        mediaCodec = MediaCodec.createDecoderByType(mime)
        mediaCodec?.setCallback(mediaCodecCallback)      // Set up Callback for the decoder
        val mediaFormat =
            if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC)
                H265Format.getDecodeFormat(imageSize, csd)
            else
                H264Format.getDecodeFormat(imageSize, csd)
        // configure mediaCodec
        mediaCodec?.configure(mediaFormat, surface, null, 0)
    }

    fun start() {
        // 启动解码器
        mediaCodec?.start()
        Log.i(tag, "mediaCodec started")
    }

    fun release() {
        mediaCodec?.stop()
        mediaCodec?.release()
        mediaCodec = null
    }
}