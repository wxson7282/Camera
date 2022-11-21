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
    object Msg {
        const val ClientInterruptRequest = "client interrupt request"
        const val ClientConnectStatus = "is client connected"
    }
}