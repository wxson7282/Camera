package com.wxson.controller.ui.main

import android.net.wifi.p2p.WifiP2pDevice
import android.util.Log
import android.util.Size
import android.view.TextureView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wxson.camera_comm.ImageData
import com.wxson.camera_comm.Msg
import com.wxson.camera_comm.Value
import com.wxson.controller.codec.Decoder
import com.wxson.controller.codec.MediaCodecCallback
import com.wxson.controller.connect.ClientRunnable
import com.wxson.controller.wifi.ClientWifiDirect
import com.wxson.controller.wifi.DeviceAdapter
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SharedViewModel : ViewModel() {
    private val tag = this.javaClass.simpleName
    private lateinit var clientRunnable: ClientRunnable
    private lateinit var clientThread: Thread
    private val wifiP2pDeviceList = ArrayList<WifiP2pDevice>()
    private val clientWifiDirect: ClientWifiDirect
    private val decoder: Decoder
    private val imageDataChannel = Channel<ImageData>(Channel.CONFLATED)
    private val msgChannel = Channel<String>(Channel.BUFFERED)

    //region attributes
    private val deviceAdapter: DeviceAdapter
    fun getDeviceAdapter(): DeviceAdapter {
        return this.deviceAdapter
    }

    fun getSurfaceTextureListener() : TextureView.SurfaceTextureListener {
        return decoder.getSurfaceTextureListener()
    }

    //endregion

    //region communication
    val isConnectedLiveData: LiveData<Boolean>      //for connectedStatus on activity toolbar
        get() = _isConnectedLiveData
    private val _isConnectedLiveData = MutableLiveData<Boolean>()

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }
    //endregion

    init {
        Log.i(tag, "init")
        wifiP2pDeviceList.clear()
        deviceAdapter = DeviceAdapter(wifiP2pDeviceList)
        clientWifiDirect = ClientWifiDirect(deviceAdapter, wifiP2pDeviceList)
        deviceAdapter.setClickListener(clientWifiDirect.deviceAdapterItemClickListener)
        decoder = Decoder.getInstance(MediaCodecCallback(imageDataChannel))
        clientWifiDirect.startDiscoverPeers()

        viewModelScope.launch {
            clientWifiDirect.msgStateFlow.collect {
                Value.Message.apply {
                    when (it.type) {
                        ConnectStatus -> _isConnectedLiveData.value = it.obj as Boolean
                        StartClientConnectThread -> startClientConnectThread(it.obj as String)   //需要使用服务器host ip， 启动客户端通信线程
                        SendMsgToRemote -> msgChannel.send(it.obj as String)       //向服务器端发出消息
                        else -> buildMsg(it)    //向上转发消息
                    }
                }
            }
        }
    }

    private fun startClientConnectThread(hostIp: String) {
        Log.i(tag, "startClientConnectThread")
        clientRunnable = ClientRunnable(hostIp, imageDataChannel, msgChannel)
        viewModelScope.launch {
            clientRunnable.msgStateFlow.collect {
                when (it.type) {
                    Value.Message.ConfigAndStartDecoder -> {        //收到第一帧图像时，先要配置解码器
                        // request to show MainFragment
                        buildMsg(Msg(Value.Message.ShowMainFragment, null))
                        // config decoder
                        val imageData = it.obj as ImageData
                        // 通知textureView根据camera镜头方向改变rotation
                        buildMsg(Msg(Value.Message.LensFacingChanged, imageData.lensFacing))
                        decoder.configure(                                       //配置解码器
                            String(imageData.mime),
                            Size(imageData.width, imageData.height),
                            imageData.csd
                        )
                        decoder.start()                                         //启动解码器
                        Log.i(tag, "decoder start")
                    }
                    Value.Message.Blank -> Log.i(tag, "blank msg from ClientRunnable")
                    else -> buildMsg(it)
                }
            }
        }
        clientThread = Thread(clientRunnable)
        clientThread.start()
    }
}