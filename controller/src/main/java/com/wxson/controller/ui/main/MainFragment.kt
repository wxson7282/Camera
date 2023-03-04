package com.wxson.controller.ui.main

import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.wxson.camera_comm.Value
import com.wxson.controller.databinding.FragmentMainBinding
import kotlinx.coroutines.launch

class MainFragment : Fragment() {

    private lateinit var viewModel: SharedViewModel
    private var binding: FragmentMainBinding? = null

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(tag, "onCreateView")
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(tag, "onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SharedViewModel::class.java]
        binding?.textureView?.surfaceTextureListener = viewModel.getSurfaceTextureListener()

        lifecycleScope.launch {
            viewModel.msgStateFlow.collect {
                when (it.type) {
                    Value.Message.LensFacingChanged -> {
                        when (it.obj as Int) {      // it.obj as lens facing
                            CameraCharacteristics.LENS_FACING_BACK -> {
                                binding?.textureView?.rotation = 90f
                            }
                            CameraCharacteristics.LENS_FACING_FRONT -> {
                                binding?.textureView?.rotation = 270f
                                binding?.textureView?.rotationY = 180f
                            }
                        }
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
}