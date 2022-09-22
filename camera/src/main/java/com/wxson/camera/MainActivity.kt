package com.wxson.camera

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.permissionx.guolindev.PermissionX
import com.wxson.camera.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var activityMainBinding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        requestPermission()

        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val mainActivityDisplayRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                this.display?.rotation
            else
                this.windowManager.defaultDisplay.rotation

        with(activityMainBinding) {
            textureView.surfaceTextureListener = viewModel.getSurfaceTextureListener()
            viewModel.setDisplayRotation(mainActivityDisplayRotation)
            viewModel.setTextureViewHeight(textureView.height)
            viewModel.setTextureViewWidth(textureView.width)
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
            viewModel.msg.collect {
                when (it.type) {
                    "msg" -> showMsg(it.obj as String)
                    "surfaceTextureDefaultBufferSize" -> setSurfaceTextureDefaultBufferSize(it.obj as Size)
                }
            }
        }
    }

    private fun setSurfaceTextureDefaultBufferSize(size: Size) {
        activityMainBinding.textureView.surfaceTexture?.setDefaultBufferSize(size.width, size.height)
    }

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    // 申请权限
    private fun requestPermission() {
        val requestList = ArrayList<String>()
        requestList.apply {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
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