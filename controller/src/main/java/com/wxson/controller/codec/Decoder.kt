package com.wxson.controller.codec

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import com.wxson.camera_comm.H264Format
import com.wxson.camera_comm.H265Format

/**
 * @author wxson
 * @date 2022/12/4
 * @apiNote Lazy singleton mode, prevent generating multiple instance.
 */
class Decoder private constructor(private val mediaCodecCallback: MediaCodecCallback) {
    private val tag = this.javaClass.simpleName
    private lateinit var mediaCodec: MediaCodec
    private lateinit var surface: Surface

    fun configure(mime: String, imageSize: Size, csd: ByteArray) {
        //set up input mediaFormat
        mediaCodec = MediaCodec.createDecoderByType(mime)
        mediaCodec.setCallback(mediaCodecCallback)      // Set up Callback for the decoder
        val mediaFormat =
            if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC)
                H265Format.getDecodeFormat(imageSize, csd)
            else
                H264Format.getDecodeFormat(imageSize, csd)
        // configure mediaCodec
        mediaCodec.configure(mediaFormat, surface, null, 0)
    }

    fun start() {
        // 启动解码器
        mediaCodec.start()
        Log.i(tag, "mediaCodec started")
    }

    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
    }

    companion object {
        @Volatile
        private var instance: Decoder? = null
        fun getInstance(mediaCodecCallback: MediaCodecCallback) =
            instance ?: synchronized(this) {
                instance ?: Decoder(mediaCodecCallback).also { instance = it }
            }
    }

    //region attributes
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
            Log.i(tag, "onSurfaceTextureAvailable")
            // get surface from TextureView.SurfaceTexture
            surface = Surface(surfaceTexture)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Log.i(tag, "onSurfaceTextureSizeChanged width=$width height=$height")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.i(tag, "onSurfaceTextureDestroyed")
            //stop and release mediaCodec
            release()
            return true
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            //Log.i(tag, "onSurfaceTextureUpdated")
        }
    }

    fun getSurfaceTextureListener(): TextureView.SurfaceTextureListener {
        return surfaceTextureListener
    }
    //endregion
}