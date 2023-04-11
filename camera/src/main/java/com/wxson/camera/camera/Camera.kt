package com.wxson.camera.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.wxson.camera_comm.Msg
import com.wxson.camera.MyApplication
import com.wxson.camera.codec.Encoder
import com.wxson.camera.codec.MediaCodecCallback
import com.wxson.camera.util.BitmapUtils
import com.wxson.camera_comm.ImageData
import com.wxson.camera_comm.Value
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

/**
 * @author wxson
 * @date 2022/9/28
 * @apiNote
 */
class Camera(private val imageDataChannel: Channel<ImageData>) {

    companion object {
        const val PREVIEW_WIDTH = 1440                                         //预览的宽度
        const val PREVIEW_HEIGHT = 1080                                       //预览的高度
        const val SAVE_WIDTH =  4160                                         //保存图片的宽度
        const val SAVE_HEIGHT = 3120                                          //保存图片的高度
        const val CODEC_WIDTH = 640                                             //编解码器图片的宽度
        const val CODEC_HEIGHT = 480                                            //编解码器图片的高度
    }

    private val tag = this.javaClass.simpleName
    private lateinit var cameraManager: CameraManager
    private var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var cameraId = "0"
    private lateinit var cameraCharacteristics: CameraCharacteristics
    private var cameraSensorOrientation = 0                                            //摄像头方向
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK              //默认使用后置摄像头
    private var displayRotation: Int = 0                                           //手机方向
    private var canTakePic = true                                                       //是否可以拍照
    private var canExchangeCamera = false                                               //是否可以切换摄像头
    private var cameraHandler: Handler
    private val handlerThread = HandlerThread("CameraThread")
    private var previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)                      //预览大小
    private var savePicSize = Size(SAVE_WIDTH, SAVE_HEIGHT)                            //保存图片大小
    private var codecSize = Size(CODEC_WIDTH, CODEC_HEIGHT)                             //编解码器图像大小
    private var textureViewHeight: Int = 0
    private var textureViewWidth: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private val maxZoom = 200                                          // 放大的最大值，用于计算每次放大/缩小操作改变的大小
    private var zoom = 0                                               // 0~maxZoom之间变化
    private val zoomStep = 20
    private var stepWidth: Float = 0f                                  // 每次改变的宽度大小
    private var stepHeight: Float = 0f                                 // 每次改变的高度大小
    lateinit var encoder: Encoder
    var mediaCodecCallback: MediaCodecCallback? = null
    private var videoCodecSizeString: String? = null
    private var videoCodecMime: String? = null
    private var encoderImageSize: Size = Size(640, 480)

    private val _msg = MutableStateFlow(Msg("", null))
    val msgStateFlow: StateFlow<Msg> = _msg

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            Log.i(tag, "onSurfaceTextureSizeChanged")
            Log.i(tag, "width=$width height=$height")
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            //Log.d(tag, "onSurfaceTextureUpdated")
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            Log.i(tag, "onSurfaceTextureDestroyed")
            releaseCamera()
            return true
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            Log.i(tag, "onSurfaceTextureAvailable")
            Log.i(tag, "width=$width height=$height")
            surfaceTexture = surface
            textureViewWidth = width
            textureViewHeight = height
            initCameraInfo()
        }
    }

    private val onImageAvailableListener = ImageReader.OnImageAvailableListener {
        val image = it.acquireNextImage()
        val byteBuffer = image.planes[0].buffer
        val byteArray = ByteArray(byteBuffer.remaining())
        byteBuffer.get(byteArray)
        image.close()

        BitmapUtils.savePic(byteArray, "camera2", cameraSensorOrientation == 270, { savedPath,
                                                                                    time ->
            buildMsg(Msg("msgStateFlow", "图片保存成功！ 保存路径：$savedPath 耗时：$time"))
        }, { msg -> buildMsg(Msg("msgStateFlow", "图片保存失败！ $msg")) })
    }

    private val captureCallBack = object : CameraCaptureSession.CaptureCallback() {

        override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
            super.onCaptureCompleted(session, request, result)
            canExchangeCamera = true
            canTakePic = true
        }

        override fun onCaptureFailed(session: CameraCaptureSession, request: CaptureRequest, failure: CaptureFailure) {
            super.onCaptureFailed(session, request, failure)
            Log.i(tag, "onCaptureFailed")
            buildMsg(Msg("msgStateFlow","开启预览失败！"))
        }
    }

    private fun buildMsg(msg: Msg) {
        _msg.value = msg
    }

    init {
        Log.i(tag, "init")
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MyApplication.context)
        videoCodecSizeString = sharedPreferences.getString("size_list", "640*480")
        videoCodecMime = sharedPreferences.getString("format_list", MediaFormat.MIMETYPE_VIDEO_AVC)
        videoCodecSizeString?.let {
            val (width, height) = it.split("*")
            encoderImageSize = Size(width.toInt(), height.toInt())
        }
        handlerThread.start()
        cameraHandler = Handler(handlerThread.looper)
    }

    fun release() {
        encoder.release()
        releaseCamera()
        releaseThread()
    }

    fun setDisplayRotation(rotation: Int?) {
        displayRotation = rotation ?: 0
    }

    fun getSurfaceTextureListener(): TextureView.SurfaceTextureListener {
        return surfaceTextureListener
    }

    fun takePic() {
        if (cameraDevice == null || !canTakePic) return

        cameraDevice?.apply {
            val captureRequestBuilder = createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(imageReader!!.surface)

            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)     // 闪光灯
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, cameraSensorOrientation)      //根据摄像头方向对保存的照片进行旋转，使其为"自然方向"
            cameraCaptureSession?.capture(captureRequestBuilder.build(), null, cameraHandler)
                ?: buildMsg(Msg("msgStateFlow","拍照异常！"))
        }
    }

    fun exchangeCamera() {
        if (cameraDevice == null || !canExchangeCamera) return

        cameraFacing = if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT)
            CameraCharacteristics.LENS_FACING_BACK
        else
            CameraCharacteristics.LENS_FACING_FRONT

        previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT) //重置预览大小
        encoder.release()
        releaseCamera()
        initCameraInfo()
        // 把当前客户端连接状态注入mediaCodecCallback
        buildMsg(Msg(Value.Message.CurrentConnectStatus, null))
    }


    private fun releaseCamera() {
        cameraCaptureSession?.close()
        cameraCaptureSession = null

        cameraDevice?.close()
        cameraDevice = null

        imageReader?.close()
        imageReader = null

        canExchangeCamera = false
    }

    private fun releaseThread() {
        handlerThread.quitSafely()
    }

    private fun initCameraInfo() {
        Log.i(tag, "initCameraInfo")
        cameraManager = MyApplication.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIdList = cameraManager.cameraIdList
        if (cameraIdList.isEmpty()) {
            buildMsg(Msg("msgStateFlow","没有可用相机"))
            return
        }

        for (id in cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

            if (facing == cameraFacing) {
                cameraId = id
                cameraCharacteristics = characteristics
                Log.i(tag, "initCameraInfo 设备中的摄像头 $id")
                break
            }
        }
        val supportLevel = cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
            buildMsg(Msg("msgStateFlow","相机硬件不支持新特性"))
        }

        //获取摄像头方向
        cameraSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
        val configurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

        // initZoomParameter
        // 获取最大的放大倍数 maxDigitalZoom表示 active_rect 除以 crop_rect 的最大值
        val maxDigitalZoom = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1
        Log.i(tag, "maxDigitalZoom: $maxDigitalZoom")
        // 获取未缩放的正常预览画面大小
        val rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        Log.i(tag, "sensor_info_active_array_size: $rect")
        // crop_rect的最小宽高
        rect?.let {
            val minWidth: Float = it.width().toFloat().div(maxDigitalZoom.toFloat())
            val minHeight: Float = it.height().toFloat().div(maxDigitalZoom.toFloat())
            // 因为缩放时两边都要变化，所以要除以2
            stepWidth = (it.width() - minWidth) / maxZoom / 2
            stepHeight = (it.height() - minHeight) / maxZoom / 2
        }

        val savePicSizes = configurationMap?.getOutputSizes(ImageFormat.JPEG)          //保存照片尺寸
        val previewSizes = configurationMap?.getOutputSizes(SurfaceTexture::class.java) //预览尺寸
        val codecSizes = configurationMap?.getOutputSizes(MediaCodec::class.java)       //编解码尺寸

        Log.i(tag, "图片保存尺寸")
        savePicSize = getBestSize(
            savePicSize.width, savePicSize.height, savePicSize.width, savePicSize.height,
            savePicSizes?.toList() ?: emptyList()
        )

        Log.i(tag, "图片预览尺寸")
        previewSize = getBestSize(
            previewSize.width, previewSize.height, textureViewHeight, textureViewWidth,
            previewSizes?.toList() ?: emptyList()
        )

        Log.i(tag, "编解码尺寸")
        codecSize = getBestSize(
            codecSize.width, codecSize.height, codecSize.width, codecSize.height,
            codecSizes?.toList() ?: emptyList()
        )

        // 请求MainActivity重置预览尺寸
        Log.i(tag, "setPreviewSize(${previewSize.width}, ${previewSize.height})")
        buildMsg(Msg("setPreviewSize", previewSize))

        Log.i(tag, "预览最优尺寸 ：${previewSize.width} * ${previewSize.height}, 比例  ${previewSize.width.toFloat() / previewSize.height}")
        Log.i(tag, "保存图片最优尺寸 ：${savePicSize.width} * ${savePicSize.height}, 比例  ${savePicSize.width.toFloat() / savePicSize.height}")
        Log.i(tag, "图像编解码最优尺寸 ： ${codecSize.width} * ${codecSize.height}, 比例  ${codecSize.width.toFloat() / codecSize.height}")

        imageReader = ImageReader.newInstance(savePicSize.width, savePicSize.height, ImageFormat.JPEG, 1)
        imageReader?.setOnImageAvailableListener(onImageAvailableListener, cameraHandler)
        // 定义编码器回调
        mediaCodecCallback = MediaCodecCallback(
            videoCodecMime?:MediaFormat.MIMETYPE_VIDEO_AVC,
            encoderImageSize, cameraFacing,
            imageDataChannel)

        openCamera()
    }

    /**
     *
     * 根据提供的参数值返回与指定宽高相等或最接近的尺寸
     *
     * @param targetWidth   目标宽度
     * @param targetHeight  目标高度
     * @param maxWidth      最大宽度
     * @param maxHeight     最大高度
     * @param sizeList      支持的Size列表
     *
     * @return  返回与指定宽高相等或最接近的尺寸
     *
     */
    private fun getBestSize(targetWidth: Int, targetHeight: Int, maxWidth: Int, maxHeight: Int, sizeList: List<Size>): Size {
        val bigEnough = ArrayList<Size>()     //比指定宽高大的Size列表
        val notBigEnough = ArrayList<Size>()  //比指定宽高小的Size列表

        for (size in sizeList) {
            //宽<=最大宽度  &&  高<=最大高度  &&  宽高比 == 目标值宽高比
            if (size.width <= maxWidth && size.height <= maxHeight
                && (size.width.toFloat() / size.height) == (targetWidth.toFloat() / targetHeight)) {
                if (size.width >= targetWidth && size.height >= targetHeight)
                    bigEnough.add(size)
                else
                    notBigEnough.add(size)
            }
            Log.i(tag,"系统支持的尺寸: ${size.width} * ${size.height} ,  比例 ：${size.width.toFloat() / size.height}")
        }
        Log.i(tag,"最大尺寸 ：$maxWidth * $maxHeight, 比例 ：${maxWidth.toFloat() / maxHeight}")
        Log.i(tag,"目标尺寸 ：$targetWidth * $targetHeight, 比例 ：${targetWidth.toFloat() / targetHeight}")

        //选择bigEnough中最小的值  或 notBigEnough中最大的值
        return when {
            bigEnough.size > 0 -> Collections.min(bigEnough, CompareSizesByArea())
            notBigEnough.size > 0 -> Collections.max(notBigEnough, CompareSizesByArea())
            else -> sizeList[0]
        }
    }

    private class CompareSizesByArea : Comparator<Size> {
        override fun compare(size1: Size, size2: Size): Int {
            return java.lang.Long.signum(size1.width.toLong() * size1.height - size2.width.toLong() * size2.height)
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(MyApplication.context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            buildMsg(Msg("msgStateFlow", "没有相机权限！"))
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                Log.i(tag, "onOpened")
                cameraDevice = camera
                createPreviewCaptureSession(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                Log.i(tag, "onDisconnected")
            }

            override fun onError(camera: CameraDevice, error: Int) {
                Log.i(tag, "onError $error")
                buildMsg(Msg("msgStateFlow", "打开相机失败！$error"))
            }
        }, cameraHandler)
    }

    private fun createPreviewCaptureSession(cameraDevice: CameraDevice) {

        previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        // 建立视频编码器
        if (mediaCodecCallback != null)
            encoder = Encoder(encoderImageSize, mediaCodecCallback!!)
        else {
            Log.e(tag, "createPreviewCaptureSession mediaCodecCallback==null")
            return
        }


        val previewSurface = Surface(surfaceTexture)
        previewRequestBuilder?.let {
            it.addTarget(previewSurface)  // 将CaptureRequest的构建器与Surface对象绑定在一起
            it.addTarget(encoder.encoderInputSurface)
            it.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)      // 闪光灯
            it.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE) // 自动对焦
        }

        // 为相机预览，创建一个CameraCaptureSession对象
        // api28 之后，需要先创建SessionConfiguration，然后cameraDevice.createCaptureSession(sessionConfiguration)
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigureFailed(session: CameraCaptureSession) {
                buildMsg(Msg("msgStateFlow", "开启预览会话失败！"))
            }

            override fun onConfigured(session: CameraCaptureSession) {
                Log.i(tag, "createPreviewCaptureSession onConfigured")
                cameraCaptureSession = session
                previewRequestBuilder?.let {
                    session.setRepeatingRequest(it.build(), captureCallBack, cameraHandler)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val outputConfigurations = listOf(
                OutputConfiguration(previewSurface),
                OutputConfiguration(encoder.encoderInputSurface),
                OutputConfiguration(imageReader!!.surface))
            val executor = MyApplication.context.mainExecutor
            cameraDevice.createCaptureSession(SessionConfiguration(SessionConfiguration.SESSION_REGULAR,
                outputConfigurations, executor, stateCallback ))
        } else {
            @Suppress("DEPRECATION")
            cameraDevice.createCaptureSession(arrayListOf(
                previewSurface,
                encoder.encoderInputSurface,
                imageReader?.surface), stateCallback, cameraHandler)
        }
        // 启动编码器
        encoder.start()

    }

    /*
    变焦时需要重新构建cameraCaptureSession
     */
    private fun reStartPreview() {
        cameraCaptureSession?.let {
            previewRequestBuilder?.let {
                cameraCaptureSession!!.setRepeatingRequest(it.build(), captureCallBack, cameraHandler)
            }
        }
    }

    fun handleZoom(isZoomIn: Boolean) {
        if (cameraDevice == null || previewRequestBuilder == null) {
            return
        }
        if (isZoomIn) { // 放大
            //zoom++
            zoom += zoomStep
            if (zoom > maxZoom) {
                zoom = maxZoom
            }
        } else { // 缩小
            //zoom--
            zoom -= zoomStep
            if (zoom < 0) {
                zoom = 0
            }
        }
        Log.v(tag, "handleZoom: zoom: $zoom")
        val rect = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val cropW = (stepWidth * zoom).toInt()
        val cropH = (stepHeight * zoom).toInt()
        rect?.let {
            val zoomRect = Rect(it.left + cropW, it.top + cropH, it.right - cropW, it.bottom - cropH)
            Log.i(tag, "zoomRect: $zoomRect")
            previewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
            reStartPreview() // 需要重新 start preview 才能生效
        }
    }

}