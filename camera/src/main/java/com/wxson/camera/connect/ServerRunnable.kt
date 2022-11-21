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
import java.net.ServerSocket
import kotlin.concurrent.thread

/**
 * @author wxson
 * @date 2022/10/31
 * @apiNote
 * 这里提供server通信服务，由于Channel的收发是一对一，不是广播，因此当前连接的客户端没有退出之前，不能接收另外客户端的请求。
 * 执行顺序是 ① accept() ② 启动输出线程 ③ 读取客户端信息
 * ②和③是并行
 * 当inputJob被取消后，释放资源，重新准备接收下一次客户端请求。
 * 由于阻塞方法只能运行在阻塞协程域中，所以父子协程中分别用runBlocking实现
 */
class ServerRunnable(private val coroutineChannel: Channel<ImageData>) : Runnable {
    private val tag = this.javaClass.simpleName
    private val serverSocket = ServerSocket(Value.Int.serverSocketPort)
    private val inputJob by lazy { Job() }
    private val acceptJob by lazy { Job() }
    private val outputJob  by lazy { Job() }

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg
    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    override fun run() {
        Log.i(tag, "run")
        runBlocking(acceptJob) {
            Log.i(tag, "acceptJob start")
            while (isActive) {
                try {
                    val clientSocket = serverSocket.accept()   //这是阻塞方法，接收到客户端请求后进入后续处理
                    buildMsg(Msg(Value.Msg.ClientConnectStatus, true))        //向ViewModel发出客户端已连接消息
                    val bufferedSource: BufferedSource = clientSocket.source().buffer()
                    val bufferedSink: BufferedSink = clientSocket.sink().buffer()
                    //启动输出线程
                    thread {
                        runBlocking(outputJob) {
                            Log.i(tag, "outputJob start")
                            while (isActive) {
                                // 接收来自MediaCodecCallback编码后的imageData数据
                                val imageData = coroutineChannel.receive()                  //这是阻塞方法
                                // 发送编码后的imageData数据
                                writeImageData(bufferedSink, imageData)
                            }
                        }
                        Log.i(tag, "outputJob end")
                    }
                    //输入协程，读取客户端信息，与serverSocket.accept()在同一线程，
                    // 只有当inputJob中止后才能接收下一次客户端连接请求
                    withContext(CoroutineScope(inputJob).coroutineContext) {
                        Log.i(tag, "inputJob start")
                        while (isActive) {
                            val receivedMsg = readClientMsg(bufferedSource)
                            msgHandle(receivedMsg)
                        }
                    }
                    Log.i(tag, "inputJob end")
                    // inputJob取消后，执行以下代码
                    if (bufferedSink.isOpen) bufferedSink.close()
                    if (bufferedSource.isOpen) bufferedSource.close()
                    if (!clientSocket.isClosed) clientSocket.close()
                } catch (e: IOException) {  // 因Socket is closed退出循环
                    break
                }
                catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
        Log.i(tag, "acceptJob end")
    }

    private fun writeImageData(bufferedSink: BufferedSink, imageData: ImageData) {
        try {
            // 发送序列化后的imageData数据
            CommonTools.objectToByteString(imageData)?.let {
                bufferedSink.write(it)      //这是阻塞方法
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun readClientMsg(bufferedSource: BufferedSource) : String {
        try {
            return bufferedSource.readByteString().toString()           //这是阻塞方法，接收到客户端数据后进入后续处理
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun msgHandle(receivedMsg: String) {
        when (receivedMsg) {
            Value.Msg.ClientInterruptRequest -> {      //客户端中断请求
                outputJob.cancel()
                inputJob.cancel()
            }
        }
    }

    fun stopService() {
        if (!serverSocket.isClosed) serverSocket.close()
        CoroutineScope(Job()).launch {
            if (outputJob.isActive) outputJob.cancel()
            if (inputJob.isActive) inputJob.cancel()
            if (acceptJob.isActive) acceptJob.cancel()
        }
    }
}