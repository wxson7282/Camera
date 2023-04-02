package com.wxson.camera_comm

/**
 * @author wxson
 * @date 2022/11/3
 * @apiNote
 */
object Value {
    object Int {
        const val serverSocketPort = 30000
        const val ServerSocketTimeout = 30000
    }
    object Message {
        const val Blank = ""
        const val ClientInterruptRequest = "client interrupt request"
        const val ConnectStatus = "connect status"
        const val CurrentConnectStatus = "current connect status"
        const val ShowWifiP2pInfo = "SHOW_WIFI_P2P_INFO"
        const val ShowRemoteDeviceInfo = "SHOW_REMOTE_DEVICE_INFO"
        const val ShowSelfDeviceInfo = "SHOW_SELF_DEVICE_INFO"
        const val StartClientConnectThread = "start client connect thread"
        const val SendMsgToRemote = "SEND_MSG_TO_REMOTE"
        const val MsgClientConnected = "msg client connected"
        const val MsgShow = "msgStateFlow"
        const val ShowSnack = "show snack bar"
        const val DismissSnack = "dismiss snack bar"
        const val ConfigAndStartDecoder = "config and start decoder"
        const val ShowMainFragment = "show MainFragment"
        const val LensFacingChanged = "lens facing changed"
//        const val ImageSizeChanged = "image size changed"
    }
}