package com.wxson.camera.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.wxson.camera.MyApplication
import com.wxson.camera.R
import com.wxson.camera_comm.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * @author wxson
 * @date 2022/11/1
 * @apiNote
 */
class ServerWifiDirect : WifiP2pManager.ChannelListener, IDirectActionListener {
    private val tag = this.javaClass.simpleName
    private val wifiP2pManager: WifiP2pManager
    private val channel: WifiP2pManager.Channel
    private val receiver: BroadcastReceiver
    private val clientList = ArrayList<String>()

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    init {
        Log.i(tag, "init")
        wifiP2pManager =  MyApplication.context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(MyApplication.context, Looper.getMainLooper(), this)
        createGroup()
        wifiP2pManager.setDeviceName(channel, MyApplication.context.getString(R.string.app_name))
        clientList.clear()
        receiver = DirectBroadcastReceiver(wifiP2pManager, channel, this)
        MyApplication.context.registerReceiver(receiver, DirectBroadcastReceiver.getIntentFilter())
    }

    fun release() {
        unregisterBroadcastReceiver()
        removeGroup()
    }

    fun createGroup() {
        Log.i(tag, "createGroup()")
        if (ActivityCompat.checkSelfPermission(MyApplication.context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i(tag, "需要申请ACCESS_FINE_LOCATION权限")
            // Do not have permissions, request them now
            buildMsg(Msg(Value.Message.MsgShow, "需要申请ACCESS_FINE_LOCATION权限"))
            return
        }
        wifiP2pManager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(tag, "createGroup onSuccess")
                buildMsg(Msg(Value.Message.MsgShow, "group is formed"))
            }

            override fun onFailure(reason: Int) {
                Log.i(tag, "createGroup onFailure: $reason")
                buildMsg(Msg(Value.Message.MsgShow, "createGroup onFailure"))
            }
        })
    }

    fun removeGroup() {
        Log.i(tag, "removeGroup()")
        wifiP2pManager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.i(tag, "removeGroup onSuccess")
                //channel.close()
                clearClientList()
                // notify MediaCodecCallback of connect status
                buildMsg(Msg(Value.Message.MsgShow, "group is not formed"))
            }

            override fun onFailure(reason: Int) {
                Log.i(tag, "removeGroup onFailure")
                buildMsg(Msg(Value.Message.MsgShow, "removeGroup onFailure"))
            }
        })
    }

    private fun clearClientList() {
        clientList.clear()
        buildMsg(Msg(Value.Message.MsgShow, "clientList Cleared"))
    }

    private fun unregisterBroadcastReceiver() {
        MyApplication.context.unregisterReceiver(receiver)
    }

    override fun onChannelDisconnected() {
        Log.i(tag, "onChannelDisconnected")
    }

    override fun onWifiP2pEnabled(enabled: Boolean) {
        Log.i(tag, "onWifiP2pEnabled=$enabled")
    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.i(tag, "onConnectionInfoAvailable=$wifiP2pInfo")

        if (wifiP2pInfo.groupFormed) {
            buildMsg(Msg(Value.Message.MsgShow, "group is formed"))
        } else {
            Log.i(tag, "未建组！")
            buildMsg(Msg(Value.Message.MsgShow, "group is not formed"))
        }
        if (!wifiP2pInfo.isGroupOwner) {
            Log.i(tag, "本机不是组长！")
            buildMsg(Msg(Value.Message.MsgShow, "本机不是组长！"))
        }
        // 启动通信服务进程
        //serverThread.start()

    }

    override fun onDisconnection() {
        Log.i(tag, "onDisconnection")
        clearClientList()
        buildMsg(Msg(Value.Message.ConnectStatus, false))        //向ViewModel发出客户端连接已断开消息
    }

    override fun onSelfDeviceAvailable(selfDevice: WifiP2pDevice) {
        Log.i(tag, "onSelfDeviceAvailable=$selfDevice")
    }

    override fun onPeersAvailable(deviceList: Collection<WifiP2pDevice>) {
        Log.i(tag, "onPeersAvailable=$deviceList")
    }

    override fun onP2pDiscoveryStopped() {
        Log.i(tag, "onP2pDiscoveryStopped")
    }
}