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
import com.blankj.utilcode.util.ImageUtils
import com.top.parkingcharges.R
import com.top.parkingcharges.databinding.FragmentPaymentStateBinding
import com.top.parkingcharges.viewmodel.Event
import com.top.parkingcharges.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger

class PaymentStateFragment : Fragment() {
    private lateinit var binding: FragmentPaymentStateBinding
    private val viewModel by activityViewModels<MainViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPaymentStateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.viewState.flowWithLifecycle(lifecycle).collectLatest {
                it.paymentInfo?.also { paymentInfo ->
                    val bitmap = withContext(Dispatchers.IO) {
                        ImageUtils.getBitmap(BigInteger(paymentInfo.qrCode, 16).toByteArray(), 0)
                    }
                    binding.ivQrCode.setImageBitmap(bitmap)
                    val stringList = paymentInfo.payContentEntity.text.split(",")
                    binding.tvPlateType.text = buildString {
                        stringList.forEach {
                            append(it)
                            append("\n").append("\n")
                        }
                    }
                    val st = it.paymentInfo.payContentEntity.st
                    if (st != 0) {
                        countdownJob(st)
                    }
//                    binding.tvPlateType.text = paymentInfo.plateType
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