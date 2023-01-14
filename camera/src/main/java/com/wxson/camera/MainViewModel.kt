package com.wxson.camera

import android.util.Log
import android.view.TextureView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wxson.camera.camera.Camera
import com.wxson.camera.connect.ServerRunnable
import com.wxson.camera.wifi.ServerWifiDirect
import com.wxson.camera_comm.ImageData
import com.wxson.camera_comm.Msg
import com.wxson.camera_comm.Value
import kotlinx.coroutines.channels.Channel
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
    private val camera: Camera
    private val serverRunnable: ServerRunnable
    private val serverThread: Thread
    private val serverWifiDirect: ServerWifiDirect
    // imageDataChannel 的buffer内只保留最新数据
    // Drop the oldest value in the buffer on overflow, add the new value to the buffer, do not suspend.
    private val imageDataChannel = Channel<ImageData>(Channel.CONFLATED)

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    init {
        Log.i(tag, "init")

        camera = Camera(imageDataChannel)
        viewModelScope.launch {
            camera.msgStateFlow.collect {
                buildMsg(it)        // 转发来自camera的Msg
            }
        }
        serverRunnable = ServerRunnable(imageDataChannel)
        serverThread = Thread(serverRunnable)
        serverWifiDirect = ServerWifiDirect()
        viewModelScope.launch {
            serverRunnable.msgStateFlow.collect{
                when (it.type) {
                    Value.Message.ConnectStatus -> {  //把客户端连接状态注入mediaCodecCallback
                        camera.mediaCodecCallback.isClientConnected = it.obj as Boolean
                        buildMsg(it)        //转发到MainActivity
                    }
                }
            }
        }
        viewModelScope.launch {
            serverWifiDirect.msgStateFlow.collect {
                buildMsg(it)        // 转发来自wifiDirect的Msg
            }
        }
        // 启动通信服务进程
        serverThread.start()
    }

    override fun onCleared() {
        super.onCleared()
        // stop serverThread
        serverRunnable.stopService()
        camera.release()
        serverWifiDirect.release()
        imageDataChannel.close()
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

    fun createGroup() {
        serverWifiDirect.createGroup()
    }

    fun removeGroup() {
        serverWifiDirect.removeGroup()
    }

}
