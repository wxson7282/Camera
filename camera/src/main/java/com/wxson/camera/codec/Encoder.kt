package com.wxson.camera.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.preference.PreferenceManager
import com.wxson.camera.MyApplication
import com.wxson.camera_comm.H264Format
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
class Encoder(imageSize: Size, mediaCodecCallback: MediaCodecCallback) {
    private val tag = this.javaClass.simpleName
    private val mediaCodec: MediaCodec
    val encoderInputSurface: Surface

    init {
        Log.i(tag, "init")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.context)
        val videoCodecMime = sharedPreferences.getString("format_list", MediaFormat.MIMETYPE_VIDEO_AVC)
        mediaCodec = MediaCodec.createEncoderByType(videoCodecMime ?: MediaFormat.MIMETYPE_VIDEO_AVC)
        // Set up Callback for the Encoder
        mediaCodec.setCallback(mediaCodecCallback)
        //set up output mediaFormat
        val codecFormat =
            if (videoCodecMime == MediaFormat.MIMETYPE_VIDEO_HEVC)
                H265Format.getEncodeFormat(imageSize)
            else
                H264Format.getEncodeFormat(imageSize)
        // configure mediaCodec
        mediaCodec.configure(codecFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        // Set up Surface for the Encoder
        encoderInputSurface = MediaCodec.createPersistentInputSurface()
        mediaCodec.setInputSurface(encoderInputSurface)
    }

    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
    }

    fun start() {
        // 启动编码器
        mediaCodec.start()
        Log.i(tag, "mediaCodec started")
    }
}