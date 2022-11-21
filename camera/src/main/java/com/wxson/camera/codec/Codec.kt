package com.wxson.camera.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.wxson.camera_comm.H265Format

/**
 * @author wxson
 * @date 2022/10/21
 * @apiNote
 *     创建H265格式视频编码器
 *     实例化MediaCodecCallback
 *     格式化编码器
 *     启动编码器
 */
class Codec(previewSize: Size, mediaCodecCallback: MediaCodecCallback) {
    private val tag = this.javaClass.simpleName
    private val videoCodecMime = MediaFormat.MIMETYPE_VIDEO_HEVC
    private val mediaCodec = MediaCodec.createEncoderByType(videoCodecMime) // 创建H265格式视频编码器
    val encoderInputSurface: Surface

    init {
        Log.i(tag, "init")
        // Set up Callback for the Encoder
        //mediaCodec.setCallback(MediaCodecCallback(videoCodecMime, previewSize))
        mediaCodec.setCallback(mediaCodecCallback)
        //set up output mediaFormat
        val codecFormat = H265Format.getEncodeFormat(previewSize)
        // configure mediaCodec
        mediaCodec.configure(codecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // Set up Surface for the Encoder
        encoderInputSurface = MediaCodec.createPersistentInputSurface()
        mediaCodec.setInputSurface(encoderInputSurface)
        // 启动编码器
        mediaCodec.start()
        Log.i(tag, "mediaCodec started")
    }
}