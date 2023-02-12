package com.wxson.controller.connect

import com.wxson.camera_comm.ImageData
import kotlinx.coroutines.channels.Channel

/**
 * @author wxson
 * @date 2023/2/12
 * @apiNote
 */
interface IFirstImageDataListener {
    fun onFirstImageDataArrived(imageData: ImageData, imageDataChannel: Channel<ImageData>)
}