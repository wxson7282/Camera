package com.wxson.camera.connect

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
import java.io.IOException
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

/**
 * @author wxson
 * @date 2022/10/31
 * @apiNote
 * 这里提供server通信服务，由于Channel的收发是一对一，不是广播，因此当前连接的客户端没有退出之前，不能接收另外客户端的请求。
 * 执行顺序是 ① accept() ② 启动输出协程 ③ 启动输入协程
 * ②和③是并行
 * 由于阻塞方法只能运行在阻塞协程域中，所以父协程中用runBlocking实现
 */
class ServerRunnable(private val imageDataChannel: Channel<ImageData>) : Runnable {
    private val tag = this.javaClass.simpleName
    private val serverSocket = ServerSocket(Value.Int.serverSocketPort)
    private val serverJob by lazy { Job() }

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    override fun run() {
        Log.i(tag, "run")
        runBlocking(serverJob) {
            Log.i(tag, "serverJob start")
            while (isActive) {
                val clientSocket = getClientSocket() ?: break
                buildMsg(Msg(Value.Message.ConnectStatus, true))        //向ViewModel发出客户端已连接消息
                clientSocket.use { socket ->
                    val bufferedSource: BufferedSource = socket.source().buffer()
                    val bufferedSink: BufferedSink = socket.sink().buffer()
                    //输出协程
                    val outputJob = outputJobAsync(this, bufferedSink)
                    //输入协程
                    val inputJob = inputJobAsync(this, bufferedSource)
                    // if any job ended, other job should be cancelled.
                    if (outputJob.await() || inputJob.await()) {
                        outputJob.cancel()
                        inputJob.cancel()
                        buildMsg(Msg(Value.Message.ConnectStatus, false))        //向ViewModel发出客户端连接中断消息
                    }
                }
            }
        }
        Log.i(tag, "serverJob end")
    }

    private fun getClientSocket(): Socket? {
        return try {
            serverSocket.accept()   //这是阻塞方法，接收到客户端请求后进入后续处理
        } catch (e: IOException) {
            Log.e(tag, e.message.toString())
            null
        }
    }

    private fun outputJobAsync(coroutineScope: CoroutineScope, bufferedSink: BufferedSink) : Deferred<Boolean> {
        return coroutineScope.async(Dispatchers.IO) {
            Log.i(tag, "outputJob start")
            try {
                val objectOutputStream = ObjectOutputStream(bufferedSink.outputStream())
                while (isActive) {
                    // 接收来自MediaCodecCallback编码后的imageData数据
                    val imageData = imageDataChannel.receive()                  //这是阻塞方法
                    // 发送imageData数据
                    objectOutputStream.writeObject(imageData)
                    objectOutputStream.reset()
                    bufferedSink.flush()
                    //Log.i(tag, "imageData wrote")
                }
            } catch (socketException: SocketException) {
                Log.e(tag, "writeImageData SocketException")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "outputJob end")
            true
        }
    }

    private fun inputJobAsync(coroutineScope: CoroutineScope, bufferedSource: BufferedSource) : Deferred<Boolean> {
        return coroutineScope.async(Dispatchers.IO) {
            Log.i(tag, "inputJob start")
            try {
                while (isActive) {
                    val receivedMsg = bufferedSource.readByteArray().toString() //这是阻塞方法，接收到客户端数据后进入后续处理
                    msgHandle(receivedMsg)
                }
            }  catch (socketException: SocketException) {
                Log.e(tag, "readClientMsg SocketException")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Log.i(tag, "inputJob end")
            true
        }
    }

    private fun msgHandle(receivedMsg: String) {
        when (receivedMsg) {
            Value.Message.ClientInterruptRequest -> {      //客户端中断请求
                //clientSocket should be closed and recreated
            }
        }
    }

    fun stopService() {
        if (!serverSocket.isClosed) serverSocket.close()
        CoroutineScope(Job()).launch {
            if (serverJob.isActive) serverJob.cancel()
        }
    }
}