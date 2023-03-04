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
class MediaCodecCallback(
    private val mime: String,
    private val imageSize: Size,
    private var lensFacing: Int,
    private val imageDataChannel: Channel<ImageData>) :
    MediaCodec.Callback() {
    private val tag = this.javaClass.simpleName
    private lateinit var firstFrameCsd: ByteArray

    //region attributes
    var isClientConnected = false
    //private var lensFacing: Int = CameraCharacteristics.LENS_FACING_BACK
    //fun setLensFacing(facing: Int) {
    //    lensFacing = facing
    //}
    //endregion

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        Log.i(tag, "onInputBufferAvailable")
    }

    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        //Log.i(tag, "onOutputBufferAvailable")
        val outputBuffer = codec.getOutputBuffer(index)
        if (outputBuffer != null) {
            //csd only in first image data
            getCsd(outputBuffer)?.let { csd -> firstFrameCsd = csd }
            if (this::firstFrameCsd.isInitialized && isClientConnected) {
                val imageData = ImageData()
                imageData.lensFacing = lensFacing
                imageData.csd = firstFrameCsd
                imageData.mime = mime.toByteArray()
                //imageSize
                imageData.width = imageSize.width
                imageData.height = imageSize.height
                //从imageData中取出byte[]
                val bytes = ByteArray(outputBuffer.remaining())
                outputBuffer.get(bytes)
                imageData.byteArray = bytes
                imageData.bufferInfoFlags = info.flags
                imageData.bufferInfoOffset = info.offset
                imageData.bufferInfoPresentationTimeUs = info.presentationTimeUs
                imageData.bufferInfoSize = info.size
                // send byteBufferTransfer to ServerRunnable by imageDataChannel
                CoroutineScope(Job()).launch {
                    //启动帧数据传输
                    imageDataChannel.send(imageData)
                }
            }
        }
        //outputBuffer?.let {
        //    //csd only in first image data
        //    getCsd(it)?.let { csd -> firstFrameCsd = csd }
        //    if (this::firstFrameCsd.isInitialized && isClientConnected) {
        //        val imageData = ImageData()
        //        imageData.lensFacing = lensFacing
        //        imageData.csd = firstFrameCsd
        //        imageData.mime = mime.toByteArray()
        //        //imageSize
        //        imageData.width = imageSize.width
        //        imageData.height = imageSize.height
        //        //从imageData中取出byte[]
        //        val bytes = ByteArray(it.remaining())
        //        it.get(bytes)
        //        imageData.byteArray = bytes
        //        imageData.bufferInfoFlags = info.flags
        //        imageData.bufferInfoOffset = info.offset
        //        imageData.bufferInfoPresentationTimeUs = info.presentationTimeUs
        //        imageData.bufferInfoSize = info.size
        //        // send byteBufferTransfer to ServerRunnable by imageDataChannel
        //        CoroutineScope(Job()).launch {
        //            //启动帧数据传输
        //            imageDataChannel.send(imageData)
        //        }
        //    }
        //}
        codec.releaseOutputBuffer(index, false)
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.i(tag, "onError")
        codec.reset()
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.i(tag, "onOutputFormatChanged")
    }
}