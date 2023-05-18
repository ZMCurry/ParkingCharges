package com.top.parkingcharges.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.blankj.utilcode.util.AdaptScreenUtils
import com.google.android.material.textfield.TextInputLayout
import com.top.parkingcharges.databinding.DialogFragmentHostBinding
import com.top.parkingcharges.viewmodel.HostPort
import com.top.parkingcharges.viewmodel.MainViewModel


class HostDialogFragment : DialogFragment() {
    private lateinit var binding: DialogFragmentHostBinding
    private val viewModel by activityViewModels<MainViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogFragmentHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(AdaptScreenUtils.pt2Px(400f), AdaptScreenUtils.pt2Px(370f))
        }
        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btOk.setOnClickListener {
            if (validateHost() && validatePort()) {
                viewModel.updateHost(
                    HostPort(
                        binding.etHost.text!!.trim().toString(),
                        binding.etPort.text!!.trim().toString()
                    )
                )
                dismissAllowingStateLoss()
            }
        }

        binding.btCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
    }

    /**
     * 显示错误提示，并获取焦点
     * @param textInputLayout
     * @param error
     */
    private fun showError(textInputLayout: TextInputLayout, error: String) {
        textInputLayout.error = error
        textInputLayout.editText?.isFocusable = true
        textInputLayout.editText?.isFocusableInTouchMode = true
        textInputLayout.editText?.requestFocus()
    }

    private fun validateHost(): Boolean {
        if (binding.etHost.text?.trim().isNullOrBlank()) {
            showError(binding.tilHost, "串口设备不能为空")
            return false
        }
        return true
    }

    private fun validatePort(): Boolean {
        if (binding.etPort.text?.trim().isNullOrBlank()) {
            showError(binding.tilPort, "波特率不能为空")
            return false
        }
        return true
    }
}