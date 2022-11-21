package com.wxson.camera

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.permissionx.guolindev.PermissionX
import com.wxson.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag = this.javaClass.simpleName
    private lateinit var viewModel: MainViewModel
    private lateinit var activityMainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        requestPermission()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        @Suppress("DEPRECATION")
        val mainActivityDisplayRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                this.display?.rotation
            else
                this.windowManager.defaultDisplay.rotation

        with(activityMainBinding) {
            textureView.surfaceTextureListener = viewModel.getSurfaceTextureListener()
            viewModel.setDisplayRotation(mainActivityDisplayRotation)
            btnTakePic.setOnClickListener {
                if (textureView.isAvailable)
                    viewModel.takePic()
            }
            btnExchange.setOnClickListener {
                if (textureView.isAvailable)
                    viewModel.exchangeCamera()
            }
            btnZoomIn.setOnClickListener{
                viewModel.handleZoom(true)
            }
            btnZoomOut.setOnClickListener {
                viewModel.handleZoom(false)
            }
        }

        lifecycleScope.launch {
            viewModel.msgStateFlow.collect {
                when (it.type) {
                    "msgStateFlow" -> showMsg(it.obj as String)
                    "setPreviewSize" -> setPreviewSize(it.obj as Size)
                }
            }
        }
    }

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun setPreviewSize(size: Size) {
        activityMainBinding.textureView.setAspectRation(size.width, size.height)
    }

    // 申请权限
    private fun requestPermission() {
        val requestList = ArrayList<String>()
        requestList.apply {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        if (requestList.isNotEmpty()) {
            PermissionX.init(this)
                .permissions(requestList)
                .explainReasonBeforeRequest()
                .onExplainRequestReason {scope, deniedList ->
                    val message = "PermissionX需要您同意以下权限才能正常使用"
                    scope.showRequestReasonDialog(deniedList, message, "允许", "拒绝")
                }
                .request { allGranted, _, deniedList ->
                    if (allGranted) {
                        showMsg("所有申请的权限都已通过")
                    } else {
                        showMsg("您拒绝了如下权限：$deniedList")
                        if (!this.isFinishing) {
                            this.finish()
                        }
                    }
                }
        }
    }
}