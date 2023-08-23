package com.example.audio_camera.ui

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio_camera.logic.AudioCameraViewModel
import com.example.audio_camera.ui.theme.BodyColor

@Composable
fun CameraBody(
    clickable: Clickable = combinedClickable()
) {
    val viewModel = viewModel<AudioCameraViewModel>()
    val viewState = viewModel.viewState.value

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .background(Color.Black)    // 最下层底色
            .background(BodyColor, RoundedCornerShape(10.dp))   // 上层底色
    ) {
        val (imageRef, btnAreaRef) = remember { createRefs() }
        // 图像区
        AndroidView(
            modifier = Modifier
                .constrainAs(imageRef) {
                    top.linkTo(parent.top)
                    bottom.linkTo(btnAreaRef.top, 50.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            factory = { context ->
                PreviewView(context).apply {
                    setBackgroundColor(Color.White.toArgb())
                    layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FIT_CENTER    // 视频帧位于中心，无拉伸变形
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE  // TextureView模式
                }
            }
        ) {}

        // 按钮区
        Row(
            modifier = Modifier.constrainAs(btnAreaRef) {
                bottom.linkTo(parent.bottom)
            }
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { clickable.onZoomIn },
                enabled = viewState.btnZoomInEnabled
            ) {
                Text("➕")
            }
            Button(
                onClick = { clickable.onZoomOut },
                enabled = viewState.btnZoomOutEnabled
            ) {
                Text("➖")
            }
            Button(
                onClick = { clickable.onTakePic },
                enabled = viewState.btnTakePicEnabled
            ) {
                Text("拍照")
            }
            Button(
                onClick = { clickable.onExchange },
                enabled = viewState.btnExchangeEnabled
            ) {
                Text("切换镜头")
            }
        }
    }
}


data class Clickable constructor(
    val onExchange: () -> Unit,
    val onTakePic: () -> Unit,
    val onZoomIn: () -> Unit,
    val onZoomOut: () -> Unit
)

fun combinedClickable(
    onExchange: () -> Unit = {},
    onTakePic: () -> Unit = {},
    onZoomIn: () -> Unit = {},
    onZoomOut: () -> Unit = {}
) = Clickable(onExchange, onTakePic, onZoomIn, onZoomOut)

@Preview(widthDp = 400, heightDp = 700)
@Composable
fun CameraBodyPreview() {
    CameraBody()
}