package com.wxson.camera.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import com.wxson.camera_comm.AvcUtil.getCsd
import com.wxson.camera_comm.ImageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * @author wxson
 * @date 2022/10/22
 * @apiNote
 */
class MediaCodecCallback(private val mime: String, private val imageSize: Size, private val coroutineChannel: Channel<ImageData>) : MediaCodec.Callback() {
    private val tag = this.javaClass.simpleName
    private lateinit var firstFrameCsd: ByteArray
    var isClientConnected = false

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        Log.i(tag, "onInputBufferAvailable")
    }

    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        Log.i(tag, "onOutputBufferAvailable")
        if (isClientConnected) {
            val outputBuffer = codec.getOutputBuffer(index)
            //var csd: ByteArray?
            val imageData = ImageData()
            outputBuffer?.let {
                //csd only in first frame video data
                getCsd(it)?.let { csd -> firstFrameCsd = csd }
                imageData.csd = firstFrameCsd
                imageData.mime = mime.toByteArray()
                //imageSize
                imageData.width = imageSize.width
                imageData.height = imageSize.height
                //从imageData中取出byte[]
                val bytes = ByteArray(it.remaining())
                it.get(bytes)
                imageData.byteArray = bytes
                imageData.bufferInfoFlags = info.flags
                imageData.bufferInfoOffset = info.offset
                imageData.bufferInfoPresentationTimeUs = info.presentationTimeUs
                imageData.bufferInfoSize = info.size
                // send byteBufferTransfer to ServerRunnable by coroutineChannel
                CoroutineScope(Job()).launch {
                    //启动帧数据传输
                    coroutineChannel.send(imageData)
                }
            }
        }
        codec.releaseOutputBuffer(index, false)
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.i(tag, "onError")
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.i(tag, "onOutputFormatChanged")
    }
}