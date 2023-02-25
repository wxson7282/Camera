package com.wxson.controller.codec

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import com.wxson.camera_comm.ImageData
import com.wxson.controller.connect.IFirstImageDataListener
import com.wxson.controller.ui.main.SharedViewModel
import kotlinx.coroutines.channels.Channel

/**
 * @author wxson
 * @date 2023/1/31
 * @apiNote
 */
class ControllerTextureView(mContext: Context, attrs: AttributeSet) : TextureView(mContext, attrs),
    TextureView.SurfaceTextureListener {

    private val tag = this.javaClass.simpleName
    private var mediaCodec: MediaCodec? = null

    init {
        this.surfaceTextureListener = this
        this.rotation = 270f
        this.rotationY = 180f
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        Log.i(tag, "onSurfaceTextureAvailable")
        registerFirstImageDataListener()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.i(tag, "onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.i(tag, "onSurfaceTextureDestroyed")
        //stop and release mediaCodec
        mediaCodec?.let {
            it.stop()
            it.release()
        }
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        //Log.i(tag, "onSurfaceTextureUpdated")
    }

    private fun registerFirstImageDataListener() {
        val firstImageDataListener = object : IFirstImageDataListener {
            override fun onFirstImageDataArrived(imageData: ImageData, imageDataChannel: Channel<ImageData>) {
                ////prepare decoder
                //val surface = Surface(super@ControllerTextureView.getSurfaceTexture())
                //mediaCodec = Decoder.prepareDecoder(surface, imageData, imageDataChannel)
                ////start decoder
                //mediaCodec?.start()
            }
        }
        //listener injected into SharedViewModel
        //SharedViewModel.firstImageDataListener = firstImageDataListener
    }
}