package com.wxson.controller.connect

import android.util.Log
import com.wxson.camera_comm.ImageData
import com.wxson.camera_comm.Msg
import com.wxson.camera_comm.Value
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.*
import java.io.ObjectInputStream
import java.net.Socket
import java.net.SocketException
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
    private val clientJob by lazy { Job() }
    private lateinit var clientSocket: Socket
    private var lensFacing = 99              //undefined

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    override fun run() {
        Log.i(tag, "ClientRunnable start")
        runBlocking(clientJob) {
            Log.i(tag, "clientJob start")
            try {
                clientSocket = Socket(serverIp, Value.Int.serverSocketPort)
                //buildMsg(Msg(Value.Message.ConnectStatus, true))        //向ViewModel发出客户端已连接消息
                clientSocket.use { socket ->
                    val bufferedSource: BufferedSource = socket.source().buffer()
                    val bufferedSink: BufferedSink = socket.sink().buffer()
                    //输入协程
                    val inputJob = inputJobAsync(this, bufferedSource)
                    //输出协程
                    val outputJob = outputJobAsync(this, bufferedSink)
                    // if any job ended, other job should be cancelled.
                    if (inputJob.await() || outputJob.await()) {
                        outputJob.cancel()
                        inputJob.cancel()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "clientJob end")
        }
        Log.i(tag, "ClientRunnable end")
    }

    private fun inputJobAsync(coroutineScope: CoroutineScope, bufferedSource: BufferedSource) =
        coroutineScope.async(Dispatchers.IO) {
            Log.i(tag, "inputJob start")
            try {
                val objectInputStream = ObjectInputStream(bufferedSource.inputStream())
                var isFirstImageData = true
                while (isActive) {
                    val imageData = objectInputStream.readObject()
                    if (imageData.javaClass.simpleName == "ImageData") {
                        val cameraFacing = (imageData as ImageData).lensFacing
                        if (isFirstImageData) {     //如果是第一帧图像数据，则通知调用者配置解码器，并且启动。
                            Log.i(tag, "first imageData arrived")
                            this@ClientRunnable.lensFacing = cameraFacing
                            buildMsg(Msg(Value.Message.ConfigAndStartDecoder, imageData))
                            isFirstImageData = false
                            delay(200)  //需要给解码器一点时间
                        } else if (this@ClientRunnable.lensFacing != cameraFacing) {
                            Log.i(tag, "lens facing changed")
                            buildMsg(Msg(Value.Message.LensFacingChanged, imageData))
                            this@ClientRunnable.lensFacing = cameraFacing
                            delay(300)
                        }
                        imageDataChannel.send(imageData)
                        //Log.i(tag, "imageData arrived")
                    }
                }
                objectInputStream.close()
            } catch (socketException: SocketException) {
                Log.e(tag, "inputJobAsync SocketException")
            } catch (e: IOException) {
                Log.e(tag, "inputJobAsync IOException")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "inputJob end")
            true
        }


    private fun outputJobAsync(coroutineScope: CoroutineScope, bufferedSink: BufferedSink) : Deferred<Boolean> {
        return coroutineScope.async(Dispatchers.IO) {
            Log.i(tag, "outputJob start")
            try {
                while (isActive) {
                    // 接收本机(客户端)发出的各种信息
                    val msgString = msgChannel.receive()
                    bufferedSink.writeUtf8(msgString + System.lineSeparator())
                    bufferedSink.flush()
                    Log.i(tag, "output message: $msgString")
                }
            } catch (e: SocketException) {
                Log.e(tag, "outputJobAsync SocketException")
            } catch (e: IOException) {
                Log.e(tag, "outputJobAsync IOException")
            }catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "outputJob end")
            true
        }
    }
    fun stopRunnable() {
        if (!clientSocket.isClosed) clientSocket.close()
        CoroutineScope(Job()).launch {
            if (clientJob.isActive) clientJob.cancel()
        }
    }
}