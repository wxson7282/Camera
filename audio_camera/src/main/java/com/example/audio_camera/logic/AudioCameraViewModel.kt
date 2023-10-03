package com.example.audio_camera.logic

import androidx.camera.core.CameraSelector
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

    fun dispatch(action: Action) =
        reduce(viewState.value, action)

    private fun reduce(state: ViewState, action: Action) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                emit(when (action) {
                    Action.Exchange -> run {
                        if (state.lensFacing == CameraSelector.LENS_FACING_BACK)
                            state.copy(lensFacing = CameraSelector.LENS_FACING_FRONT)
                        else
                            state.copy(lensFacing = CameraSelector.LENS_FACING_BACK)
                    }
                })
            }
        }
    }

    data class ViewState(
        val btnTakePicEnabled: Boolean = true,
        val btnExchangeEnabled: Boolean = true,
        val lensFacing: Int = CameraSelector.LENS_FACING_BACK
    )

    private fun emit(state: ViewState) {
        _viewState.value = state
    }

}

sealed interface Action {
    object Exchange : Action
}





