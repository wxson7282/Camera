package com.wxson.camera

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.permissionx.guolindev.PermissionX
import com.wxson.camera.databinding.ActivityMainBinding
import com.wxson.camera_comm.Value
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag = this.javaClass.simpleName
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding
    private lateinit var menu: Menu
    private lateinit var menuItemCreateGroup: MenuItem
    private lateinit var menuItemRemoveGroup: MenuItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        @Suppress("DEPRECATION")
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        requestPermission()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)

        @Suppress("DEPRECATION")
        val mainActivityDisplayRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                this.display?.rotation
            else
                this.windowManager.defaultDisplay.rotation

        with(binding) {
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
                    Value.Message.MsgShow -> {
                        when (val msg = it.obj as String) {
                            "group is formed" -> {
                                if (this@MainActivity::menu.isInitialized) {
                                    menuItemCreateGroup.isEnabled = false
                                    menuItemRemoveGroup.isEnabled = true
                                }
                            }
                            "group is not formed" -> {
                                if (this@MainActivity::menu.isInitialized) {
                                    menuItemCreateGroup.isEnabled = true
                                    menuItemRemoveGroup.isEnabled = false
                                }
                            }
                            else -> showMsg(msg)
                        }
                    }
                    "setPreviewSize" -> setPreviewSize(it.obj as Size)
                    Value.Message.ConnectStatus -> setConnectStatus(it.obj as Boolean)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu
        menuItemCreateGroup = this.menu.findItem(R.id.createGroup)
        menuItemRemoveGroup = this.menu.findItem(R.id.removeGroup)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // 从SettingsActivity返回后，如果设定ImageSize发生变更，应该重新建立MediaCodecCallback的实例。目前尚未实现。
            R.id.settings -> {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
            R.id.createGroup -> viewModel.createGroup()
            R.id.removeGroup -> viewModel.removeGroup()
        }
        return true
    }

    private fun showMsg(msg: String){
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    private fun setPreviewSize(size: Size) {
        binding.textureView.setAspectRation(size.width, size.height)
    }

    private fun setConnectStatus(connected: Boolean) {
        binding.imageConnected.setImageDrawable(AppCompatResources.getDrawable(
            this@MainActivity, if (connected) R.drawable.ic_connected else R.drawable.ic_disconnected))
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