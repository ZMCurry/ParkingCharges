package com.top.parkingcharges.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.top.parkingcharges.R
import com.top.parkingcharges.databinding.FragmentReleaseStateBinding
import com.top.parkingcharges.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ReleaseStateFragment : Fragment() {
    private val viewModel by activityViewModels<MainViewModel>()
    private lateinit var binding: FragmentReleaseStateBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReleaseStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.flowWithLifecycle(lifecycle).collectLatest {
                it.releaseInfo?.also { releaseInfo ->
                    binding.tvMsg1.text = releaseInfo.msg1
                    binding.tvMsg2.text = releaseInfo.msg2
                    binding.tvPlateNum.text = releaseInfo.plateNo
                    binding.tvPlateType.text = releaseInfo.plateType
                }
            }
        }

        //放行信息显示5s后，回到空闲页
        viewLifecycleOwner.lifecycleScope.launch {
            delay(5000)
            findNavController().navigate(R.id.idleStateFragment)
        }
    }
}