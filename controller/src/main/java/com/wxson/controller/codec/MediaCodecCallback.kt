package com.wxson.controller.codec

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import com.wxson.camera_comm.ImageData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

/**
 * @author wxson
 * @date 2022/12/4
 * @apiNote
 */
class MediaCodecCallback(private val imageDataChannel: Channel<ImageData>) : MediaCodec.Callback() {
    private val tag = this.javaClass.simpleName

    override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
        CoroutineScope(Job()).launch {
            try {
                val inputBuffer: ByteBuffer? = codec.getInputBuffer(index)
                inputBuffer?.let {
                    it.clear()
                    val imageData = imageDataChannel.receive()
                    val length = imageData.bufferInfoSize
                    val timeStamp = imageData.bufferInfoPresentationTimeUs
                    it.put(imageData.byteArray, 0, length)
                    //把inputBuffer放回队列
                    codec.queueInputBuffer(index, 0, length, timeStamp, 0)
                }
            } catch (e: IndexOutOfBoundsException) {
                Log.e(tag, "inputBuffer数据越界 IndexOutOfBoundsException");
            } catch (e: Exception) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
        codec.releaseOutputBuffer(index, false) //release the buffer back to the codec
    }

    override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
        Log.i(tag, "onError")
        codec.reset()
    }

    override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
        Log.i(tag, "onOutputFormatChanged")
    }
}