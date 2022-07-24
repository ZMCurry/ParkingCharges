package com.top.parkingcharges.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.king.zxing.util.CodeUtils
import com.top.parkingcharges.R
import com.top.parkingcharges.databinding.FragmentIdleStateBinding
import com.top.parkingcharges.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IdleStateFragment : Fragment() {
    private val viewModel by activityViewModels<MainViewModel>()

    private lateinit var binding: FragmentIdleStateBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIdleStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            viewModel.viewState.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collectLatest {
                    val bitmap = withContext(Dispatchers.IO) {
                        CodeUtils.createQRCode(
                            it.defaultQrCode,
                            resources.getDimensionPixelSize(R.dimen.qr_code_height)
                        )
                    }
                    binding.ivQrCode.setImageBitmap(bitmap)
                }
        }
    }
}