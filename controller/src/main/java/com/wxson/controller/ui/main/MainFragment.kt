package com.wxson.controller.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.wxson.controller.databinding.FragmentMainBinding

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
        binding?.let {
            it.textureView.rotation = 270f
            it.textureView.rotationY = 180f
            it.textureView.surfaceTextureListener = viewModel.getSurfaceTextureListener()
        }
    }

    override fun onDestroyView() {
        Log.i(tag, "onDestroyView")
        super.onDestroyView()
        binding = null
    }
}