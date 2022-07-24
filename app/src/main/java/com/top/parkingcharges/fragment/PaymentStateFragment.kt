package com.top.parkingcharges.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.king.zxing.util.CodeUtils
import com.top.parkingcharges.R
import com.top.parkingcharges.databinding.FragmentPaymentStateBinding
import com.top.parkingcharges.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    //plateNo为空时，使用默认二维码
                    val bitmap = withContext(Dispatchers.IO) {
                        CodeUtils.createQRCode(
                            if (paymentInfo.plateNo == null) it.defaultQrCode else paymentInfo.qrCode,
                            resources.getDimensionPixelSize(R.dimen.qr_code_height)
                        )
                    }
                    binding.ivQrCode.setImageBitmap(bitmap)
                    binding.tvPlateNum.text = paymentInfo.plateNo
                    binding.tvPlateType.text = paymentInfo.plateType
                }
            }
        }

        //测试代码
//        viewLifecycleOwner.lifecycleScope.launch {
//            delay(3000)
//            viewModel.sendMessage("payOver")
//        }
    }
}