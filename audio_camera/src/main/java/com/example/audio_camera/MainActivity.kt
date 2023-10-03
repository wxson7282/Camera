package com.example.audio_camera

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio_camera.logic.Action
import com.example.audio_camera.logic.AudioCameraViewModel
import com.example.audio_camera.logic.Screen
import com.example.audio_camera.logic.takePhoto
import com.example.audio_camera.ui.CameraBody
import com.example.audio_camera.ui.CameraBodyPreview
import com.example.audio_camera.ui.combinedClickable
import com.example.audio_camera.ui.theme.AudioCameraTheme

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

                    CameraBody(combinedClickable(
                        onExchange = { viewModel.dispatch(Action.Exchange) }
                    )
                    ) {
                        Screen(
                            Modifier.fillMaxSize()
                        )
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