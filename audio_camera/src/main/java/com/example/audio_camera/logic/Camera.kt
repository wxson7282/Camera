package com.example.audio_camera.logic

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio_camera.MyApplication
import com.example.audio_camera.R
import com.google.accompanist.permissions.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private lateinit var cameraController: LifecycleCameraController

@Composable
private fun CameraScreen(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK) {
    Log.i("CameraScreen", "start")
    val context = MyApplication.context
    val lifecycleOwner = LocalLifecycleOwner.current
    cameraController = remember { LifecycleCameraController(context) }

    //根据lensFacing初始化相机
    cameraController.cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

    AndroidView(
        modifier = modifier,
        factory = { context1 ->
            PreviewView(context1).apply {
                setBackgroundColor(Color.White.toArgb())
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                scaleType = PreviewView.ScaleType.FIT_CENTER    // 视频帧位于中心，无拉伸变形
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE  // TextureView模式
            }.also { previewView ->
                previewView.controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
                Log.i("AndroidView.previewView", "cameraController.bindToLifecycle")
            }
        },
        onReset = {
            Log.i("AndroidView.previewView", "onReset")
        },
        onRelease = {
            cameraController.unbind()
            Log.i("AndroidView.previewView", "onRelease cameraController.unbind")
        }
    ) {
        Log.i("AndroidView.${it.javaClass.simpleName}", "update")
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Screen(modifier: Modifier = Modifier) {
    Log.i("Screen", "start")
    val multiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    )
    if (multiplePermissionsState.allPermissionsGranted) {
        Log.i("Screen", "allPermissionsGranted")
        val viewModel = viewModel<AudioCameraViewModel>()
        val viewState = viewModel.viewState.value
        CameraScreen(modifier, viewState.lensFacing)
        Log.i("Screen", "CameraScreen()")
    } else {
        MultiplePermissionsScreen(multiplePermissionsState)
        Log.i("Screen", "MultiplePermissionsScreen()")
    }

//    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
//    if (!cameraPermissionState.status.isGranted && !cameraPermissionState.status.shouldShowRationale) {
//        LaunchedEffect(key1 = Unit) {
//            cameraPermissionState.launchPermissionRequest()
//            Log.i("Screen", "cameraPermissionState.launchPermissionRequest")
//        }
//    }
//    if (cameraPermissionState.status.isGranted) {
//        val viewModel = viewModel<AudioCameraViewModel>()
//        val viewState = viewModel.viewState.value
//
//        CameraScreen(modifier, viewState.lensFacing)
//        Log.i("Screen", "CameraScreen()")
//    } else {
//        NoCameraPermissionScreen(cameraPermissionState = cameraPermissionState)
//        Log.i("Screen", "NoCameraPermissionScreen()")
//    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun NoCameraPermissionScreen(cameraPermissionState: PermissionState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val textToShow = if (cameraPermissionState.status.shouldShowRationale) {
            // 如果用户之前选择了拒绝该权限，应当向用户解释为什么应用程序需要这个权限
            "未获取相机授权将导致该功能无法正常使用。"
        } else {
            // 首次请求授权
            "该功能需要使用相机权限，请点击授权。"
        }
        Text(textToShow)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) { Text("请求权限") }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun MultiplePermissionsScreen(multiplePermissionsState: MultiplePermissionsState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = getTextToShowGivenPermissions(
                multiplePermissionsState.revokedPermissions,
                multiplePermissionsState.shouldShowRationale
            ),
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
            Text("请求权限")
        }
        multiplePermissionsState.permissions.forEach {
            Divider()
            Text(text = "权限名：${it.permission} \n " +
                    "授权状态：${it.status.isGranted} \n " +
                    "需要解释：${it.status.shouldShowRationale}", fontSize = 16.sp)
        }
        Divider()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getTextToShowGivenPermissions(
    permissions: List<PermissionState>,
    shouldShowRationale: Boolean
): String {
    val size = permissions.size
    if (size == 0) return ""
    val textToShow = StringBuilder().apply { append("以下权限：") }
    for (i in permissions.indices) {
        textToShow.append(permissions[i].permission).apply {
            if (i == size - 1) append(" ") else append(", ")
        }
    }
    textToShow.append(
        if (shouldShowRationale) {
            " 需要被授权，以保证应用功能正常使用."
        } else {
            " 被拒绝使用. 应用功能将不能正常使用."
        }
    )
    return textToShow.toString()
}

private const val FILENAME = "yyyy-MM-dd-HH-mm-ss-SSS"
private const val PHOTO_TYPE = "image/jpeg"
private const val ANIMATION_SLOW_MILLIS = 100L
private const val ANIMATION_FAST_MILLIS = 50L

fun takePhoto() {
    val context = MyApplication.context
    val mainExecutor = ContextCompat.getMainExecutor(context)
    // Create time stamped name and MediaStore entry.
    val name = SimpleDateFormat(FILENAME, Locale.CHINA).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_TYPE)
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            val appName = context.resources.getString(R.string.app_name)
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${appName}")
        }
    }
    // Create output options object which contains file + metadata
    val outputOptions = ImageCapture.OutputFileOptions
        .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues).build()
    cameraController.takePicture(outputOptions, mainExecutor, object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val savedUri = outputFileResults.savedUri
            Log.i("takePhoto", "Photo capture succeeded: $savedUri")
            context.notifySystem(savedUri)
        }
        override fun onError(exception: ImageCaptureException) {
            Log.e("takePhoto", "Photo capture failed: ${exception.message}", exception)
        }
    }
    )
    context.showFlushAnimation()
}

// flash 动画
private fun Context.showFlushAnimation() {
    // We can only change the foreground Drawable using API level 23+ API
    // Display flash animation to indicate that photo was captured
    if (this is Activity) {
        val decorView = window.decorView
        decorView.postDelayed({
            decorView.foreground = ColorDrawable(android.graphics.Color.WHITE)
            decorView.postDelayed({ decorView.foreground = null }, ANIMATION_FAST_MILLIS)
        }, ANIMATION_SLOW_MILLIS)
    }
}

// 发送系统广播
private fun Context.notifySystem(savedUri: Uri?) {
    // 对于运行API级别>=24的设备，将忽略隐式广播，因此，如果您只针对24+级API，则可以删除此语句
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
        sendBroadcast(Intent(Camera.ACTION_NEW_PICTURE, savedUri))
    }
}
private fun Context.getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }

    return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
}



