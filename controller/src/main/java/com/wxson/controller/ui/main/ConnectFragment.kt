package com.wxson.controller.ui.main

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.wxson.camera_comm.Value
import com.wxson.camera_comm.WifiP2pUtil
import com.wxson.controller.R
import com.wxson.controller.databinding.FragmentConnectBinding
import kotlinx.coroutines.launch

class ConnectFragment : Fragment() {
    private var binding: FragmentConnectBinding? = null
    private lateinit var viewModel: SharedViewModel
    private lateinit var snackbar: Snackbar
    private lateinit var spaces: String

    companion object {
        fun newInstance() = ConnectFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(tag, "onCreateView")
        binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(tag, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        binding!!.rvDeviceList.adapter = viewModel.getDeviceAdapter()

        spaces = resources.getString(R.string.Spaces)

        lifecycleScope.launch {
            viewModel.msgStateFlow.collect {
                Value.Message.apply {
                    when (it.type) {
                        MsgShow -> showMsg(it.obj as String)
                        ShowSnack -> showSnack(binding!!.root, it.obj as String)
                        DismissSnack -> dismissSnack()
                        ShowSelfDeviceInfo -> showSelfDeviceInfo(it.obj as WifiP2pDevice?)
                        ShowRemoteDeviceInfo -> showRemoteDeviceInfo(it.obj as WifiP2pDevice?)
                        ShowWifiP2pInfo -> showWifiP2pInfo(it.obj as WifiP2pInfo?)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        Log.i(tag, "onDestroyView")
        super.onDestroyView()
        binding = null
    }

    private fun showMsg(msg: String){
        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }

    private fun showSnack(view: View, msg: String) {
        snackbar = Snackbar.make(view, msg, Snackbar.LENGTH_INDEFINITE)
        snackbar.show()
    }

    private fun dismissSnack() {
        snackbar.dismiss()
    }

    private fun showSelfDeviceInfo(device: WifiP2pDevice?) {
        binding?.let {
            if (device == null) {
                it.tvMyDeviceName.text = spaces
                it.tvMyDeviceMacAddress.text = spaces
                it.tvMyDeviceStatus.text = spaces

            } else {
                it.tvMyDeviceName.text = device.deviceName
                it.tvMyDeviceMacAddress.text = device.deviceAddress
                it.tvMyDeviceStatus.text = WifiP2pUtil.getDeviceStatus(device.status)
            }
        }
    }

    private fun showRemoteDeviceInfo(device: WifiP2pDevice?) {
        binding?.let {
            if (device == null) {
                it.tvRemoteDeviceName.text = spaces
                it.tvRemoteDeviceAddress.text = spaces
            } else {
                it.tvRemoteDeviceName.text = device.deviceName
                it.tvRemoteDeviceAddress.text = device.deviceAddress
            }
        }
    }

    private fun showWifiP2pInfo(wifiP2pInfo: WifiP2pInfo?) {
        binding?.let {
            if (wifiP2pInfo == null) {
                it.tvIsGroupOwner.text = spaces
                it.tvGroupOwnerAddress.text = spaces
                it.tvGroupFormed.text = spaces
            } else {
                it.tvIsGroupOwner.text = if (wifiP2pInfo.isGroupOwner) "是" else "否"
                it.tvGroupOwnerAddress.text = wifiP2pInfo.groupOwnerAddress.hostAddress
                it.tvGroupFormed.text = if (wifiP2pInfo.groupFormed) "是" else "否"
            }
        }
    }
}

