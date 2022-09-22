package com.wxson.camera

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * @author wxson
 * @date 2022/8/12
 * @apiNote
 */
class MyApplication: Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}