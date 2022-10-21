package com.wxson.camera

import android.util.Log
import android.view.TextureView
import androidx.lifecycle.ViewModel
import com.wxson.camera.module_camera.Camera
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * @author wxson
 * @date 2022/8/12
 * @apiNote
 */
class MainViewModel: ViewModel() {

    private val tag = this.javaClass.simpleName
    private var camera: Camera
    private val job = Job()
    private val _msg = MutableStateFlow(Msg("", null))
    val msg: StateFlow<Msg> = _msg

    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    init {
        Log.i(tag, "init")
        camera = Camera()
        CoroutineScope(job).launch {
            camera.msg.collect {
                buildMsg(it)        // 转发来自camera的Msg
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        camera.release()
        job.cancel()
    }

    fun setDisplayRotation(rotation: Int?) {
        camera.setDisplayRotation(rotation)
    }

    fun getSurfaceTextureListener(): TextureView.SurfaceTextureListener {
        return camera.getSurfaceTextureListener()
    }

    fun takePic() {
        camera.takePic()
    }

    fun exchangeCamera() {
        camera.exchangeCamera()
    }

    fun handleZoom(isZoomIn: Boolean) {
        camera.handleZoom(isZoomIn)
    }
}
