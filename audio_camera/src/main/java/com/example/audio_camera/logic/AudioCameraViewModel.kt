package com.example.audio_camera.logic

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudioCameraViewModel : ViewModel() {

    private val _viewState: MutableState<ViewState> = mutableStateOf(ViewState())
    val viewState : State<ViewState> = _viewState
    val zoomRatioMax = 200

    fun dispatch(action: Action) =
        reduce(viewState.value, action)

    private fun reduce(state: ViewState, action: Action) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                emit(when (action) {
                    Action.OpenCamera -> run { state}
                    Action.Exchange -> run { state }
                    Action.TakePic -> run { state }
                    Action.ZoomIn -> run { state }
                    Action.ZoomOut -> run { state }
                })
            }
        }
    }

    data class ViewState(
        val btnZoomInEnabled: Boolean = false,
        val btnZoomOutEnabled: Boolean = false,
        val btnTakePicEnabled: Boolean = false,
        val btnExchangeEnabled: Boolean = false
    ) {
        val isCameraReady: Boolean = false
        val zoomRatio: Int = 0  // 放大倍率，0-zoomMax之间
    }

    private fun emit(state: ViewState) {
        _viewState.value = state
    }
}

sealed interface Action {
    object OpenCamera : Action
    object ZoomIn : Action
    object ZoomOut : Action
    object TakePic : Action
    object Exchange : Action
}


