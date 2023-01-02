package com.wxson.controller.connect

import android.util.Log
import com.wxson.camera_comm.CommonTools
import com.wxson.camera_comm.ImageData
import com.wxson.camera_comm.Msg
import com.wxson.camera_comm.Value
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.*
import java.lang.Runnable
import java.net.Socket
import kotlin.io.use

/**
 * @author wxson
 * @date 2022/12/3
 * @apiNote
 * serverIp:
 * imageDataChannel:
 * msgChannel:
 */
class ClientRunnable(
    private val serverIp: String,
    private val imageDataChannel: Channel<ImageData>,
    private val msgChannel: Channel<String>
) : Runnable {
    private val tag = this.javaClass.simpleName
    private val mainJob by lazy { Job() }
    private val inputJob by lazy { Job() }
    private val outputJob by lazy { Job() }
    private lateinit var socket: Socket

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    inner class OutputThread(private val bufferedSink: BufferedSink) : Thread() {
        override fun run() {
            runBlocking(outputJob) {
                Log.i(tag, "outputJob start")
                while (isActive) {
                    // 接收本机(客户端)发出的各种信息
                    val msgString = msgChannel.receive()
                    // 输出到服务器
                    writeMessage(bufferedSink, msgString)
                }
            }
            Log.i(tag, "outputJob end")
        }
    }

    override fun run() {
        Log.i(tag, "ClientRunnable start")
        runBlocking(mainJob) {
            Log.i(tag, "mainJob start")
            try {
                socket = Socket(serverIp, Value.Int.serverSocketPort)
                buildMsg(Msg(Value.Message.ConnectStatus, true))        //向ViewModel发出客户端已连接消息
                socket.use { it ->
                    val bufferedSource: BufferedSource = it.source().buffer()
                    val bufferedSink: BufferedSink = it.sink().buffer()
                    //启动输出线程
                    OutputThread(bufferedSink).start()
                    //输入处理，读取server发出的imageData
                    Log.i(tag, "inputJob start")
                    var isFirstImageData = true
                    while (isActive) {
                        val imageData = readImageData(bufferedSource)
                        // 如果读取失败则丢弃
                        imageData?.let {
                            if (isFirstImageData) {     //如果是第一帧图像数据，则通知调用者配置解码器，并且启动。
                                Log.i(tag, "First imageData arrived")
                                buildMsg(Msg(Value.Message.ConfigAndStartDecoder, imageData))
                                isFirstImageData = false
                                delay(100)  //需要给解码器一点时间
                            }
                            imageDataChannel.send(imageData)
                        }
                    }
                    Log.i(tag, "inputJob end")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "mainJob end")
        }
        Log.i(tag, "ClientRunnable end")
    }

    private fun writeMessage(bufferedSink: BufferedSink, msg: String) {
        try {
            bufferedSink.write(msg.toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readImageData(bufferedSource: BufferedSource): ImageData? {
        try {
            val byteString = bufferedSource.readByteString()
            return CommonTools.byteStringToObject(byteString) as ImageData
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun stopRunnable() {
        if (!socket.isClosed) socket.close()
        CoroutineScope(Job()).launch {
            if (outputJob.isActive) outputJob.cancel()
            if (inputJob.isActive) inputJob.cancel()
            if (mainJob.isActive) mainJob.cancel()
        }
    }
}