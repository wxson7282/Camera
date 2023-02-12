package com.wxson.controller.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import com.wxson.camera_comm.H264Format
import com.wxson.camera_comm.H265Format
import com.wxson.camera_comm.ImageData
import kotlinx.coroutines.channels.Channel

/**
 * @author wxson
 * @date 2022/12/4
 * @apiNote Lazy singleton mode, prevent generating multiple instance.
 */
class Decoder private constructor(private val mediaCodecCallback: MediaCodecCallback) {
    private val tag = this.javaClass.simpleName
    private lateinit var mediaCodec: MediaCodec

    //init {
    //    Log.i(tag, "init")
    //}
    //
    //fun configure(mime: String, imageSize: Size, csd: ByteArray, surface: Surface) {
    //    //set up input mediaFormat
    //    mediaCodec = MediaCodec.createDecoderByType(mime)
    //    mediaCodec.setCallback(mediaCodecCallback)      // Set up Callback for the decoder
    //    val mediaFormat =
    //        if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC)
    //            H265Format.getDecodeFormat(imageSize, csd)
    //        else
    //            H264Format.getDecodeFormat(imageSize, csd)
    //    // configure mediaCodec
    //    mediaCodec.configure(mediaFormat, surface, null, 0)
    //}
    //
    //fun start() {
    //    // 启动解码器
    //    mediaCodec.start()
    //    Log.i(tag, "mediaCodec started")
    //}
    //
    //fun Release() {
    //    mediaCodec.stop()
    //    mediaCodec.release()
    //}

    companion object {
        @Volatile
        private var instance: Decoder? = null
        fun getInstance(mediaCodecCallback: MediaCodecCallback) =
            instance ?: synchronized(this) {
                instance ?: Decoder(mediaCodecCallback).also { instance = it }
            }

        fun prepareDecoder(surface: Surface, imageData: ImageData, imageDataChannel: Channel<ImageData>) : MediaCodec? {
            try {
                val mime = String(imageData.mime)
                val imageSize = Size(imageData.width, imageData.height)
                val csd = imageData.csd
                val mediaCodec = MediaCodec.createDecoderByType(mime)
                val mediaCodecCallback = MediaCodecCallback(imageDataChannel)
                mediaCodec.setCallback(mediaCodecCallback)
                val mediaFormat =
                    if (mime == MediaFormat.MIMETYPE_VIDEO_HEVC)
                        H265Format.getDecodeFormat(imageSize, csd)
                    else
                        H264Format.getDecodeFormat(imageSize, csd)
                // configure mediaCodec
                mediaCodec.configure(mediaFormat, surface, null, 0)
                Log.i("Decoder.companion", "prepareDecoder: mediaCodec configured")
                return mediaCodec
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }

        }
    }
}