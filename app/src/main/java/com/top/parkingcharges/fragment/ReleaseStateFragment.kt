package com.top.parkingcharges.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.top.parkingcharges.databinding.FragmentReleaseStateBinding
import com.top.parkingcharges.viewmodel.Event
import com.top.parkingcharges.viewmodel.MainViewModel
import kotlinx.coroutines.Job
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
                    val textContentList = releaseInfo.textContentList
                    binding.tvPlateType.text = buildString {
                        textContentList.forEach {
                            append(it.text)
                            append("\n").append("\n")
                        }
                    }
                    releaseInfo.textContentList.firstOrNull()?.apply {
                        countdownJob(this.dt)
                    } ?: countdownJob(5)
                }
            }
        }
    }

    var job: Job? = null
    private fun countdownJob(st: Int) {
        job?.cancel()
        job = viewLifecycleOwner.lifecycleScope.launch {
            delay(st * 1000L)
            viewModel.onEvent(Event.Idle)
        }
    }
}