@[TOC](wifi直连遥控照相系统重构)
## 回顾
2018年刚开始接触android时，用两部手机实现了wifi直连遥控照相（请参考：[用wifi直连(p2p)实现遥控照相
](https://blog.csdn.net/wxson/article/details/83018691?spm=1001.2014.3001.5501)），当时对于android系统和java语言了解甚少，做出来的东西比较幼稚，现在回头看，有些方法比较笨，存在不少问题。例如：为了实现双工通信在系统中用了两个端口，收发各用一个。实际上一个端口也完全能实现双工通信。
近期有一些空闲时间，把老系统重构了。
## kotlin语言
在android开发中使用 kotlin语言已经是大势所趋，况且 kotlin语言的优势（便捷、抽象力、多态）也是java不能比拟的。所以此次重构用 kotlin语言重写了程序。虽然在android studio平台上可以把全篇java转换成kotlin，但是有些转换并不成功，或者转换后的代码明显不合理，需要手工重写代码。上手kotlin以后，基本上没有了再使用java来开发android应用的意愿。
## 功能改进

- 增加前置镜头，原来只能使用后置镜头。
- 在远程控制功能中增加了前后置镜头切换、放大(Zoom In)、缩小(Zoom Out)，保留了拍照功能。

## 架构
时过境迁，这次采用Android Studio中常用的MVVM架构，UI层代码和逻辑层代码可以有效分离，调试和修改比较方便。
本系统涉及WIFI连接、socket通信、照相机、视频图像编解码，每一种处理都比较复杂，为了避免ViewModel臃肿，各个业务逻辑分别放在不同的模块中，通过ViewModel调用这些模块。
![相机端模块构成图](https://img-blog.csdnimg.cn/2266d90a72d34ae9a60f4faa530166ff.jpeg#pic_center)
*相机端模块构成图*
![控制器端模块构成图](https://img-blog.csdnimg.cn/a29389223c564c548e22d4787c873648.jpeg#pic_center)
*控制器端模块构成图*
控制器端的两个UI界面ConnectFragment和MainFragment通过SharedViewModel共享数据。
## 协程应用
使用kotlin协程可以轻松应对各种异步逻辑需求，主要应用于模块之间通信和并发处理。
### 模块之间通信
以前开发过程中，相当多时间和精力消耗在模块之间通信上。模块之间通信本质上是异步处理逻辑，正是协程擅长的领域，这次用到以下两种手段，感觉很方便，代码也简洁许多。当然与协程相关的数据传递方法不止这两种。
#### StateFlow
为了传递消息，定义了一个消息类Msg。成员type表示消息的类型，obj表示消息实体（可以为null）。

```kotlin
class Msg(val type: String, val obj: Any? )
```
在需要发送消息的模块中，定义StateFlow的实例，StateFlow属于热数据流（无论是否有订阅者，都会执行发出数据流的操作），使用场景与LiveData相似。对于可变消息的发布需要使用MutableStateFlow。
下面是相机端MainViewModel中的代码。

```kotlin
private val _msg = MutableStateFlow(Msg("", null))
val msgStateFlow: StateFlow<Msg> = _msg
private fun buildMsg(msg: Msg) {
    _msg.value = msg
    }
```
需要发出消息时，调用buildMsg()即可。例如：

```kotlin
buildMsg(Msg("msgStateFlow", "图片保存成功！ 保存路径：$savedPath 耗时：$time"))
```

相机端MainActivity是消息的订阅者，接收数据的collect方法是一个挂起函数，只能在协程域中运行，因此在MainActivity的UI进程中需要启动协程，以异步执行方式接收MainViewModel发出的消息。

```kotlin
lifecycleScope.launch {
    viewModel.msgStateFlow.collect {
        when (it.type) {
             // 根据消息的类型分别处理
        }
    }
}
```
这里也可以使用通常的CoroutineScope创建协程，但是lifecycleScope是与生命周期绑定的协程域，当LifecycleOwner也就是MainActivity销毁时自动取消，无需人工干预。对于ViewModel也有与生命周期绑定的协程域，它就是viewModelScope。

#### Channel
对于视频图像数据的传递，感觉用Channel更方便一些。Channel是一种热数据通道，内部有一个并发安全的队列，可以指定缓存，用于实现不同协程间的通信。
定义Channel的实例。

```kotlin
private val imageDataChannel = Channel<ImageData>(Channel.CONFLATED)
```
imageDataChannel的缓存定义为CONFLATED，意思是如果缓存溢出，丢弃旧数据，加入新数据，不挂起。

在需要发送数据的模块的协程域中，使用send方法发出数据。

```kotlin
CoroutineScope(Job()).launch {imageDataChannel.send(imageData)}
```
在接收数据模块的协程域中，使用receive方法取得数据。

```kotlin
coroutineScope.async(Dispatchers.IO) {
	val objectOutputStream = ObjectOutputStream(bufferedSink.outputStream())
	while (isActive) {
		val imageData = imageDataChannel.receive()
		// 发送imageData数据
        objectOutputStream.writeObject(imageData)
        bufferedSink.flush()
	}
}
```
对于接收到的imageData，此处使用okio的BufferedSink转为socket数据流输出。
###  并发处理
相机端和控制器端都使用socket双向通信，以前使用不同线程实现收发功能，用kotlin协程来实现则便利很多。
以相机端的通信模块为例，代码如下

```kotlin
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
                    //输入协程
                    val inputJob = inputJobAsync(this, bufferedSource)
                    //输出协程
                    val outputJob = outputJobAsync(this, bufferedSink)
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
                    bufferedSink.flush()
//                    Log.i(tag, "imageData wrote")
                }
                objectOutputStream.close()
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
                    Log.i(tag, "inputJobAsync coroutineScope isActive")
                    val receivedMsg = bufferedSource.readUtf8Line()
                    msgHandle(receivedMsg?: Value.Message.Blank)
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
        Log.i(tag, "msgHandle client message: $receivedMsg")
        when (receivedMsg) {
            Value.Message.ClientInterruptRequest -> {      //客户端中断请求
                //clientSocket should be closed and recreated
            }
           else -> {
               buildMsg(Msg(Value.Message.ClientMessage, receivedMsg))     //forward client message up
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
```
尽管协程可以处理异步任务，但是在主线程中直接用协程处理IO也会报错的，所以必须在子线程中启动协程。
在子线程中用runBlocking启动一个阻塞当前线程的名为serverJob的协程，一旦serverJob.cancel()实施，serverJob状态不再isActive，而且阻塞方法serverSocket.accept()会发生异常，双管齐下，serverJob就被取消了。
对于输入输出IO处理，程序中使用async函数制造了两个Deferred<Boolean>的实例：inputJob和outputJob，它们本质上是具有返回值的Job，可以让调用者得到inputJob和outputJob的执行结果：成功或失败。outputJob.await()和inputJob.await()会阻塞当前协程，直至inputJob和outputJob执行完毕成功返回，或者发生异常返回。在本程序中无论是否发生异常，inputJob和outputJob均返回ture。这部分的异常处理留待以后完善。
outputJob发送的是图像数据，inputJob接收的是以换行符System.lineSeparator()结尾的文字信息。
控制器端的通信模块与相机端相似，只是接收发送的内容不同，inputJob接收的是图像数据，outputJob发送的是文字数据。
## 后记
关于wifi p2p直连、照相机的使用、视频编解码，与以前的系统并没有大的不同，因此不再赘述。如果有兴趣请参考[源代码](https://download.csdn.net/download/wxson/87858597)。
欢迎指摘和讨论。

