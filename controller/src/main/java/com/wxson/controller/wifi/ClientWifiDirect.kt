package com.wxson.controller.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Looper
import android.util.Log
import com.wxson.camera_comm.*
import com.wxson.controller.MyApplication
import com.wxson.controller.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.ArrayList

/**
 * @author wxson
 * @date 2022/12/2
 * @apiNote
 */
class ClientWifiDirect(private val deviceAdapter: DeviceAdapter, private val wifiP2pDeviceList: ArrayList<WifiP2pDevice>) :
    WifiP2pManager.ChannelListener, IDirectActionListener {
    private val tag = this.javaClass.simpleName
    private val wifiP2pManager: WifiP2pManager
    private val channel: WifiP2pManager.Channel
    private val receiver: BroadcastReceiver
    private var wifiP2pEnabled = false
    private var remoteDevice: WifiP2pDevice? = null

    ////region attributes
    //private val wifiP2pDeviceList = ArrayList<WifiP2pDevice>()
    //fun getWifiP2pDeviceList(): ArrayList<WifiP2pDevice> {
    //    return this.wifiP2pDeviceList
    //}
    ////endregion

    //region communication
    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }
    //endregion

    init {
        Log.i(tag, "init")
        wifiP2pManager = MyApplication.context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = wifiP2pManager.initialize(MyApplication.context, Looper.getMainLooper(), this)
        wifiP2pManager.setDeviceName(channel, MyApplication.context.getString(R.string.app_name))
        receiver = DirectBroadcastReceiver(wifiP2pManager, channel, this)
        MyApplication.context.registerReceiver(receiver, DirectBroadcastReceiver.getIntentFilter())
        wifiP2pDeviceList.clear()

    }

    val deviceAdapterItemClickListener = object : DeviceAdapter.OnClickListener {
        override fun onItemClick(position: Int) {
            remoteDevice = wifiP2pDeviceList[position]
            buildMsg(Msg(Value.Message.MsgShow, remoteDevice!!.deviceName + "将要连接"))
            connect()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscoverPeers() {
        if (!wifiP2pEnabled) {
            // 通知调用者打开wifi
            buildMsg(Msg(Value.Message.WLANIsOff, null))
            return
        }
        // 通知调用者在ui上显示 正在搜索附近设备
        buildMsg(Msg(Value.Message.ShowSnack, "正在搜索附近设备"))
        clearWifiP2pDeviceList()
        //搜寻附近带有 Wi-Fi P2P 的设备
        wifiP2pManager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                buildMsg(Msg(Value.Message.MsgShow, "discoverPeers success"))
            }

            override fun onFailure(reasonCode: Int) {
                buildMsg(Msg(Value.Message.DismissSnack, null))
                buildMsg(Msg(Value.Message.MsgShow, "discoverPeers failure. reasonCode=$reasonCode"))
            }
        })
    }

    fun release() {
        unregisterBroadcastReceiver()
    }

    override fun onChannelDisconnected() {
        Log.i(tag, "onChannelDisconnected")
    }

    override fun onWifiP2pEnabled(enabled: Boolean) {
        Log.i(tag, "onWifiP2pEnabled")
        wifiP2pEnabled = enabled
    }

    override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
        Log.i(tag, "onConnectionInfoAvailable")
        //关闭“连接中”信息显示
        buildMsg(Msg(Value.Message.DismissSnack, null))
        //通知调用者显示WifiP2pInfo
        buildMsg(Msg(Value.Message.ShowWifiP2pInfo, wifiP2pInfo))
        //通知调用者显示选中的wifiP2pDevice
        if (remoteDevice != null) {
            buildMsg(Msg(Value.Message.ShowRemoteDeviceInfo, remoteDevice))
        }
        //判断本机为非群主，且群已经建立
        if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
            //通知调用者启动连接线程
            buildMsg(Msg(Value.Message.StartClientConnectThread, wifiP2pInfo.groupOwnerAddress.hostAddress))
            //启动连接线程需要一点时间
            Thread.sleep(100)
            //通知调用者向服务器发出连接成功消息
            buildMsg(Msg(Value.Message.SendMsgToRemote, Value.Message.MsgClientConnected))
        }
    }

    override fun onDisconnection() {
        Log.i(tag, "onDisconnection")
        buildMsg(Msg(Value.Message.MsgShow, "已断开连接"))
        clearWifiP2pDeviceList()
        //通知调用者清除各种wifi连接信息
        buildMsg(Msg(Value.Message.ShowWifiP2pInfo, null))
        buildMsg(Msg(Value.Message.ConnectStatus, false))
        buildMsg(Msg(Value.Message.ShowRemoteDeviceInfo, null))
    }

    override fun onSelfDeviceAvailable(selfDevice: WifiP2pDevice) {
        Log.i(tag, "onSelfDeviceAvailable")
        buildMsg(Msg(Value.Message.ShowSelfDeviceInfo, selfDevice))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onPeersAvailable(deviceList: Collection<WifiP2pDevice>) {
        Log.i(tag, "onPeersAvailable :" + deviceList.size)
        wifiP2pDeviceList.clear()
        wifiP2pDeviceList.addAll(deviceList)
        deviceAdapter.notifyDataSetChanged()
        buildMsg(Msg(Value.Message.DismissSnack, null))
        if (wifiP2pDeviceList.size == 0) {
            Log.e(tag, "No devices found")
        }
    }

    override fun onP2pDiscoveryStopped() {
        Log.i(tag, "onP2pDiscoveryStopped")
        if (wifiP2pDeviceList.size == 0) {
            //再度搜寻附近带有 Wi-Fi P2P 的设备
            startDiscoverPeers()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connect() {
        val config = WifiP2pConfig()
        config.deviceAddress = remoteDevice!!.deviceAddress
        config.wps.setup = WpsInfo.PBC      // Push ButtonConfiguration
        buildMsg(Msg(Value.Message.MsgShow, "正在连接"))

        wifiP2pManager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                //sendMsgLiveData(ViewModelMsg(MsgType.SHOW_CONNECT_STATUS.ordinal, true))
                buildMsg(Msg(Value.Message.ConnectStatus, true))
                Log.i(tag, "connect onSuccess")
            }

            override fun onFailure(reason: Int) {
                buildMsg(Msg(Value.Message.ConnectStatus, false))
                Log.e(tag, "连接失败 ${getGetConnectFailureReason(reason)}")
                buildMsg(Msg(Value.Message.DismissSnack, null))
            }
        })
    }


    private fun unregisterBroadcastReceiver() {
        MyApplication.context.unregisterReceiver(receiver)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun clearWifiP2pDeviceList() {
        wifiP2pDeviceList.clear()
        deviceAdapter.notifyDataSetChanged()

    }

    private fun getGetConnectFailureReason(reasonCode: Int): String {
        return when (reasonCode) {
            WifiP2pManager.ERROR -> "ERROR"
            WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
            WifiP2pManager.BUSY -> "BUSY"
            WifiP2pManager.NO_SERVICE_REQUESTS -> "NO_SERVICE_REQUESTS"
            else -> "UNKNOWN ERROR"
        }
    }

}