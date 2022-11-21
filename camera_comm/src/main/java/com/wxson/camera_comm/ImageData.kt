package com.wxson.camera_comm

import java.io.Serializable

/**
 * Created by wxson on 2022/10/23.
 * Package com.wxson.camera_comm.
 */
class ImageData(var mime: ByteArray = byteArrayOf(0x00),
                var csd: ByteArray = byteArrayOf(0x00),
                var bufferInfoFlags: Int = 0,
                var bufferInfoOffset: Int = 0,
                var bufferInfoPresentationTimeUs: Long = 0L,
                var bufferInfoSize: Int = 0,
                var previewWidth: Int = 0,
                var previewHeight: Int = 0,
                var byteArray: ByteArray = byteArrayOf(0x00)) : Serializable {

    override fun toString(): String {
        return "ImageData{ ByteBufferSize=" + byteArray.size + " }"
    }
}