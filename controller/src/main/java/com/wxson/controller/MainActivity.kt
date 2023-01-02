package com.wxson.controller

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.permissionx.guolindev.PermissionX
import com.wxson.controller.databinding.ActivityMainBinding
import com.wxson.controller.ui.main.ConnectFragment
import com.wxson.controller.ui.main.MainFragment
import com.wxson.controller.ui.main.SharedViewModel

class MainActivity : AppCompatActivity() {
    private val tag = this.javaClass.simpleName
    private val fragmentManager = supportFragmentManager
    private lateinit var currentFragment: Fragment
    private val mainFragment = MainFragment.newInstance()
    private val connectFragment = ConnectFragment.newInstance()
    private lateinit var binding: ActivityMainBinding
    //private val transaction = fragmentManager.beginTransaction()
    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(tag, "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this)[SharedViewModel::class.java]
        requestPermission()
        if (savedInstanceState == null) {
            val transaction = fragmentManager.beginTransaction()
            with(transaction) {
                //replace(R.id.container, mainFragment).commitNow()
                //把全体Fragment加入到FragmentManager
                add(R.id.frameLayout, connectFragment, connectFragment.javaClass.name).show(connectFragment)
                add(R.id.frameLayout, mainFragment, mainFragment.javaClass.name).hide(mainFragment)
                commit()
                currentFragment = connectFragment
            }
        }

        viewModel.isConnectedLiveData.observe(this) {
            binding.imageConnected.setImageResource(if (it) R.drawable.ic_connected else R.drawable.ic_disconnected)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.i(tag, "onCreateOptionsMenu")
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(tag, "onOptionsItemSelected")
        when (item.itemId) {
            R.id.menuDirectDiscover -> manageFragments(connectFragment.javaClass.name)
            R.id.menuDisplay -> manageFragments(mainFragment.javaClass.name)
            R.id.menuDirectEnable -> startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            R.id.settings -> startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun manageFragments(savedFragmentName: String) {
        Log.i(tag, "manageFragments")
        val transaction = fragmentManager.beginTransaction()
        val fragments = fragmentManager.fragments
        if (savedFragmentName.isEmpty()) {
            //遍历fragments
            for (i in fragments.indices) {
                if (i == 0) {
                    transaction.show(fragments[i])
                    currentFragment = fragments[i]
                } else {
                    transaction.hide(fragments[i])
                }
            }
        } else {
            //遍历fragments
            for (i in fragments.indices) {
                if (savedFragmentName == fragments[i].javaClass.name) {
                    transaction.show(fragments[i])
                    currentFragment = fragments[i]
                } else {
                    transaction.hide(fragments[i])
                }
            }
        }
        transaction.commit()
    }

    private fun showMsg(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    // 申请权限
    private fun requestPermission() {
        Log.i(tag, "manageFragments")
        val requestList = ArrayList<String>()
        requestList.apply {
            add(Manifest.permission.INTERNET)
            add(Manifest.permission.ACCESS_NETWORK_STATE)
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        if (requestList.isNotEmpty()) {
            PermissionX.init(this)
                .permissions(requestList)
                .explainReasonBeforeRequest()
                .onExplainRequestReason { scope, deniedList ->
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