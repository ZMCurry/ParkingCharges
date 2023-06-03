package com.top.parkingcharges.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ToastUtils
import com.cl.log.XLog
import com.google.android.material.textfield.TextInputLayout
import com.kongqw.serialportlibrary.Device
import com.kongqw.serialportlibrary.Driver
import com.kongqw.serialportlibrary.SerialPortFinder
import com.kongqw.serialportlibrary.SerialUtils
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum
import com.kongqw.serialportlibrary.enumerate.SerialStatus
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens
import com.top.parkingcharges.databinding.DialogFragmentHostBinding
import com.top.parkingcharges.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File


class HostDialogFragment : DialogFragment() {
    private val serialHostAdapter by lazy {
        SerialHostAdapter()
    }
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
            setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        }
        dialog?.apply {
            setCancelable(true)
            setCanceledOnTouchOutside(false)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSerialHost.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = serialHostAdapter
        }


        val serialPortFinder = SerialPortFinder()
        val devices = serialPortFinder.allDevicesPath.toList()

//        val devices = listOf(
//            Device("1", "2", File("")),
//            Device("2", "2", File("")),
//            Device("3", "2", File(""))
//        )
        serialHostAdapter.submitList(devices)

        binding.btOk.setOnClickListener {
            lifecycleScope.launch {
                if (validateHost() && validatePort()) {
                    viewModel.updateHost(
                        HostPort(
                            devices[serialHostAdapter.selectPosition!!],
                            binding.etPort.text!!.trim().toString()
                        )
                    )
                    SerialUtils.getInstance().serialPortClose()
                    delay(300)
                    SerialUtils.getInstance().manyOpenSerialPort(
                        listOf(
                            Driver(
                                devices[serialHostAdapter.selectPosition!!],
                                binding.etPort.text!!.trim().toString()
                            )
                        )
                    )
                }
            }
        }

        binding.btCancel.setOnClickListener {
            dismissAllowingStateLoss()
        }


        lifecycleScope.launch {
            requireContext().dataStore.data.collectLatest {
                val s = it[booleanPreferencesKey(KEY_LOG_SWITCH)]
                binding.tvSwitch.isChecked = s == true
            }
        }

        binding.tvSwitch.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                requireContext().dataStore.edit {
                    it[booleanPreferencesKey(KEY_LOG_SWITCH)] = isChecked
                }
            }
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
        if (serialHostAdapter.selectPosition == null) {
            ToastUtils.showLong("串口设备不能为空")
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

class SerialHostAdapter : ListAdapter<String, SerialHostAdapter.SerialHostViewHolder>(object :
    DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}) {
    var selectPosition: Int? = null

    class SerialHostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SerialHostViewHolder {
        val textView = TextView(parent.context).apply {
            val layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            setLayoutParams(layoutParams)
            setTextColor(Color.BLACK)
            textSize = 50f
        }
        return SerialHostViewHolder(textView)
    }

    override fun onBindViewHolder(holder: SerialHostViewHolder, position: Int) {
        (holder.itemView as TextView).text = getItem(position)
        if (selectPosition == holder.adapterPosition) {
            holder.itemView.setBackgroundColor(Color.YELLOW)
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener {
            selectPosition = holder.adapterPosition
            notifyDataSetChanged()
        }
    }
}