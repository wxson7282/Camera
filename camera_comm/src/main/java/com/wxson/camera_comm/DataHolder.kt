package com.wxson.camera_comm

import android.graphics.SurfaceTexture

/**
 * @author wxson
 * @date 2022/12/14
 * @apiNote
 */
class DataHolder private constructor() {

    companion object {
        @Volatile private var instance: DataHolder? = null
        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: DataHolder().also {
                    instance = it
                }
            }
    }

    var surfaceTexture: SurfaceTexture? = null
    var isConnected = false
}