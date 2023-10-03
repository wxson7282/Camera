package com.example.audio_camera.ui

import android.util.Log
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio_camera.logic.AudioCameraViewModel
import com.example.audio_camera.logic.takePhoto
import com.example.audio_camera.ui.theme.BodyColor

@Composable
fun CameraBody(
    clickable: Clickable = combinedClickable(),
    screen: @Composable () -> Unit
) {
    Log.i("CameraBody", "start")
    val viewModel = viewModel<AudioCameraViewModel>()
    val viewState = viewModel.viewState.value

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .background(Color.Black)    // 最下层底色
            .background(BodyColor, RoundedCornerShape(10.dp))   // 上层底色
    ) {
        val (imageRef, btnAreaRef) = remember { createRefs() }
        // preview area
        Box(
            modifier = Modifier
                .constrainAs(imageRef) {
                    bottom.linkTo(btnAreaRef.top, 2.dp)
                    height = Dimension.fillToConstraints
                    width = Dimension.matchParent
                }
        ) {
            screen()
        }

        // 按钮区
        Row(
            modifier = Modifier
                .constrainAs(btnAreaRef) {
                    bottom.linkTo(parent.bottom)
                }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { takePhoto() },  //该函数必须运行在主线程，不能使用Clickable
                enabled = viewState.btnTakePicEnabled
            ) {
                Text("拍照")
            }
            Button(
                onClick = { clickable.onExchange() },
                enabled = viewState.btnExchangeEnabled
            ) {
                Text("切换镜头")
            }
        }
    }
}


data class Clickable constructor(
    val onExchange: () -> Unit
)

fun combinedClickable(
    onExchange: () -> Unit = {}
) = Clickable(onExchange)

@Preview(widthDp = 400, heightDp = 700)
@Composable
fun CameraBodyPreview() {
    CameraBody {}
}