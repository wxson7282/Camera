package com.example.audio_camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio_camera.logic.Action
import com.example.audio_camera.logic.AudioCameraViewModel
import com.example.audio_camera.ui.CameraBody
import com.example.audio_camera.ui.CameraBodyPreview
import com.example.audio_camera.ui.combinedClickable
import com.example.audio_camera.ui.theme.AudioCameraTheme
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioCameraTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val viewModel = viewModel<AudioCameraViewModel>()

                    LaunchedEffect(key1 = Unit) {
                        while (isActive) {
                            viewModel.dispatch(Action.OpenCamera)   //启动时打开相机
                        }
                    }

                    viewModel.apply {
                        CameraBody(combinedClickable(
                            onExchange = { dispatch(Action.Exchange) },
                            onTakePic = { dispatch(Action.TakePic) },
                            onZoomIn = { dispatch(Action.ZoomIn) },
                            onZoomOut = { dispatch(Action.ZoomOut) }
                        ))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AudioCameraTheme {
        CameraBodyPreview()
    }
}