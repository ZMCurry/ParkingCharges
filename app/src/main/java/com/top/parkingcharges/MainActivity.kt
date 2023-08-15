package com.top.parkingcharges

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azhon.appupdate.listener.OnDownloadListener
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.*
import com.cl.log.XLog
import com.kongqw.serialportlibrary.Driver
import com.kongqw.serialportlibrary.SerialUtils
import com.kongqw.serialportlibrary.enumerate.SerialPortEnum
import com.kongqw.serialportlibrary.enumerate.SerialStatus
import com.kongqw.serialportlibrary.listener.SerialPortDirectorListens
import com.top.parkingcharges.databinding.ActivityMainBinding
import com.top.parkingcharges.entity.ParkingInfoEntity
import com.top.parkingcharges.entity.PayContentEntity
import com.top.parkingcharges.entity.PayInfoEntity
import com.top.parkingcharges.entity.TextContentEntity
import com.top.parkingcharges.fragment.HostDialogFragment
import com.top.parkingcharges.fragment.IdleStateFragment
import com.top.parkingcharges.fragment.PaymentStateFragment
import com.top.parkingcharges.fragment.ReleaseStateFragment
import com.top.parkingcharges.viewmodel.*
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var mSpeech: TextToSpeech

    private var partialData: Triple<ParkingMsgType, Int, LinkedList<String>>? = null

    private val msgAdapter by lazy {
        MsgAdapter()
    }


    private var count = 0

    @SuppressLint("SetTextI18n")
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction()
            .replace(R.id.fcv, IdleStateFragment())
            .commitNowAllowingStateLoss()

        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(baseContext)
            adapter = msgAdapter
        }

        mSpeech = TextToSpeech(baseContext, this)

        lifecycleScope.launch {
            viewModel.newestHost.collectLatest {
                SerialUtils.getInstance().serialPortClose()
                delay(300)
                SerialUtils.getInstance()
                    .manyOpenSerialPort(listOf(Driver(it.serialPort, it.baudRate)))
            }
        }

        lifecycleScope.launchWhenCreated {
            val firstOrNull = baseContext.dataStore.data.map { preferences ->
                val serialPort = preferences[stringPreferencesKey(KEY_SERIAL_PORT)]
                val baudRate = preferences[stringPreferencesKey(KEY_BAUD_RATE)]
                if (!serialPort.isNullOrEmpty() && !baudRate.isNullOrEmpty()) {
                    Pair(serialPort, baudRate)
                } else {
                    Pair("/dev/ttyS2", "9600")
                }
            }.firstOrNull()
            if (firstOrNull != null) {
                SerialUtils.getInstance().serialPortClose()
                delay(300)
                SerialUtils.getInstance()
                    .manyOpenSerialPort(listOf(Driver(firstOrNull.first, firstOrNull.second)))
            }
        }

//        val navOptions: NavOptions = NavOptions.Builder()
//            .setEnterAnim(R.anim.slide_in_right) //进入动画
//            .setExitAnim(R.anim.slide_out_left) //退出动画
//            .setPopEnterAnim(R.anim.slide_in_left) //弹出进入动画
//            .setPopExitAnim(R.anim.slide_out_right) //弹出退出动画
//            .build()
        lifecycleScope.launch {
            viewModel.event.collectLatest {
                when (it) {
                    Event.Idle -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fcv, IdleStateFragment())
                            .commitNowAllowingStateLoss()
                    }

                    Event.Payment -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fcv, PaymentStateFragment())
                            .commitNowAllowingStateLoss()
                    }

                    Event.Release -> {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.fcv, ReleaseStateFragment())
                            .commitNowAllowingStateLoss()
                    }
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            val firstOrNull = viewModel.getUUID()
            binding.tvUuid.text = firstOrNull
        }

        lifecycleScope.launch {
            baseContext.dataStore.data.collectLatest {
                val s = it[booleanPreferencesKey(KEY_LOG_SWITCH)]
                binding.rvMsg.isVisible = s == true
            }
        }

        //连续点击事件
        binding.tvUuid.setOnClickListener {
            //每次点击时，数组向前移动一位
            System.arraycopy(mHits, 1, mHits, 0, mHits.size - 1)
            //为数组最后一位赋值
            mHits[mHits.size - 1] = SystemClock.uptimeMillis()
            if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
                mHits = LongArray(COUNT) //重新初始化数组
                val hostDialogFragment = HostDialogFragment()
                hostDialogFragment.show(
                    supportFragmentManager,
                    HostDialogFragment::class.java.simpleName
                )
            }
        }


//        val dex1 =
//            "00,C8,FF,FF,E5,2F,01,01,00,01,78,00,81,76,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,CB,D5,55,56,31,31,37,4D,2C,CD,A3,B3,B5,30,D0,A1,CA,B1,34,37,B7,D6,D6,D3,35,32,C3,EB,2C,C7,EB,BD,C9,B7,D1,32,D4,AA,42,4D,B2,00,00,00,00,00,00,00,3E,00,00,00,28,00,00,00,1D,00,00,00,E3,FF,FF,FF,01,00,01,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,FF,FF,FF,00,00,00,00,00,FE,9D,4B,F8,82,D7,CA,08,BA,27,8A,E8,BA,6E,22,E8,BA,BF,92,E8,82,E2,32,08,FE,AA,AB,F8,00,6B,58,00,F6,72,ED,98,D1,46,8B,68,E2,59,E3,68,34,B0,B0,B0,87,69,69,68,14,66"
//        val dex2 =
//            "BC,B8,FF,18,E5,A0,B8,67,7F,68,CB,B8,45,80,ED,B9,DB,B8,8B,15,76,78,65,18,9F,E0,07,C2,AF,B8,00,1E,A8,D0,FE,45,4A,88,82,97,D8,90,BA,54,4F,88,BA,D6,4A,B0,BA,C6,22,20,82,DD,D1,38,FE,F7,7F,F0,AE,85"
//
//        val dex = "00,64,FF,FF,6E,57,00,02,00,15,01,19,00,FF,00,00,00,08,D2,BB,C2,B7,CB,B3,B7,E7,0D,01,15,01,19,00,00,FF,00,00,17,CB,D5,45,41,44,33,33,30,35,C1,D9,CA,B1,B3,B5,D0,BB,D0,BB,BB,DD,B9,CB,00,0A,1D,CB,D5,45,41,44,33,33,30,35,2C,C1,D9,CA,B1,B3,B5,2C,D7,A3,C4,FA,D2,BB,C2,B7,CB,B3,B7,E7,00,5A,81"
//
//        lifecycleScope.launch {
//            repeat(2) {
//                val list = when (it) {
//                    0 -> {
//                        LinkedList(
//                            dex1.split(",").map { map ->
//                                map.toInt(16)
//                            }.joinToString(separator = ",") { eachByte ->
//                                "%02x".format(eachByte)
//                            }.split(",")
//                        )
//                    }
//
//                    1 -> {
//                        LinkedList(
//                            dex2.split(",").map { map ->
//                                map.toInt(16)
//                            }.joinToString(separator = ",") { eachByte ->
//                                "%02x".format(eachByte)
//                            }.split(",")
//                        )
//                    }
//
//                    else -> {
//                        LinkedList(
//                            BigInteger(dex2.replace(",", ""), 16).toByteArray()
//                                .joinToString(separator = ",") { eachByte ->
//                                    "%02x".format(eachByte)
//                                }.split(",")
//                        )
//                    }
//                }
//                if (partialData == null) {
//                    val length = getLength(list)
//                    handleList(list, length, getType(list), SerialPortEnum.SERIAL_ONE)
//                } else {
//                    partialData?.third?.addAll(list)
//                    if (partialData!!.third.size >= partialData!!.second) {
//                        val first = partialData?.first
//                        val second = partialData?.second
//                        val third = partialData?.third
//                        if (first != null && second != null && third != null) {
//                            handleList(
//                                third,
//                                getLength(third),
//                                getType(third),
//                                SerialPortEnum.SERIAL_ONE
//                            )
//                        }
//                    }
//                }
//            }
//
//        }

        SerialUtils.getInstance().setmSerialPortDirectorListens(object : SerialPortDirectorListens {

            /**
             * 接收回调
             * @param bytes 接收到的数据
             * @param serialPortEnum  串口类型
             */

            @Synchronized
            override fun onDataReceived(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                Log.i(TAG, "当前接收串口类型：" + serialPortEnum.name)
                Log.i(TAG, "onDataReceived [ byte[] ]: " + bytes.contentToString())
                Log.i(TAG, "onDataReceived [ String ]: " + String(bytes, Charset.forName("GB2312")))

                lifecycleScope.launch(Dispatchers.Main) {
                    try {
                        val uppercase = (bytes.joinToString(separator = ",") { eachByte ->
                            "%02x".format(eachByte)
                        }).uppercase()
                        withContext(Dispatchers.IO) {
                            FileIOUtils.writeFileFromString(
                                logFile,
                                "APP开启第${count + 1}次收到消息:$uppercase", true
                            )
                            FileIOUtils.writeFileFromString(
                                logFile,
                                "\n", true
                            )
                            count++
                        }
                        if (binding.rvMsg.isVisible) {
                            msgAdapter.submitList(msgAdapter.currentList + uppercase)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                val list = LinkedList(bytes.joinToString(separator = ",") { eachByte ->
                    "%02x".format(eachByte)
                }.split(","))

                if (partialData == null) {
                    val length = getLength(list)
                    handleList(list, length, getType(list), serialPortEnum)
                } else {
                    partialData?.third?.addAll(list)
                    if (partialData!!.third.size >= partialData!!.second) {
                        val first = partialData?.first
                        val second = partialData?.second
                        val third = partialData?.third
                        if (first != null && second != null && third != null) {
                            handleList(third, getLength(third), getType(third), serialPortEnum)
                        }
                    }
                }

            }

            /**
             * 发送回调
             * @param bytes 发送的数据
             * @param serialPortEnum  串口类型
             */
            override fun onDataSent(bytes: ByteArray, serialPortEnum: SerialPortEnum) {
                Log.i(TAG, "当前发送串口类型：" + serialPortEnum.name)
                Log.i(TAG, "onDataSent [ byte[] ]: " + bytes.contentToString())
                Log.i(TAG, "onDataSent [ String ]: " + String(bytes))
            }

            /**
             * 串口打开回调
             * @param serialPortEnum  串口类型
             * @param device  串口号
             * @param status 打开状态
             */
            override fun openState(
                serialPortEnum: SerialPortEnum,
                device: File,
                status: SerialStatus
            ) {
                XLog.i("串口打开状态：" + device.name + "---打开状态：" + status.name)
                when (status) {
                    SerialStatus.SUCCESS_OPENED -> ToastUtils.showShort("串口打开成功")
                    SerialStatus.NO_READ_WRITE_PERMISSION -> ToastUtils.showShort("没有读写权限")
                    SerialStatus.OPEN_FAIL -> ToastUtils.showShort("串口打开失败")
                }
                if (status == SerialStatus.SUCCESS_OPENED) {
                    val findFragmentByTag =
                        supportFragmentManager.findFragmentByTag(HostDialogFragment::class.java.simpleName)
                    if (findFragmentByTag != null && findFragmentByTag is HostDialogFragment) {
                        findFragmentByTag.dismissAllowingStateLoss()
                    }
                }
            }
        })


        PermissionUtils.permission(PermissionConstants.STORAGE)
            .callback { isAllGranted, granted, deniedForever, denied ->
                if (isAllGranted) {
                    FileUtils.delete(logFile)
                    FileUtils.createOrExistsFile(logFile)
                }
            }.request()

    }


    private val logFile =
        File(PathUtils.getExternalStoragePath() + File.separator + "parkingLog.txt")

    private fun handleList(
        list: LinkedList<String>,
        length: Int,
        parkingMsgType: ParkingMsgType,
        serialPortEnum: SerialPortEnum
    ) {
        if (length == 0 || list.isEmpty()) {
            return
        }
        when (parkingMsgType) {
            ParkingMsgType.SIX_E -> {
                if (list.size <= length) {
                    if (list.size < length) {
                        partialData =
                            Triple(ParkingMsgType.SIX_E, length, list)
                    } else {
                        try {
                            parseHexList(list, serialPortEnum)
                        } catch (e: Exception) {
                            partialData = null
                        }
                    }
                } else {
                    partialData = null
                }
            }

            ParkingMsgType.E_FIVE -> {
                if (list.size <= length) {
                    if (list.size < length) {
                        partialData =
                            Triple(ParkingMsgType.E_FIVE, length, list)
                    } else {
                        try {
                            parsePayList(list, serialPortEnum)
                        } catch (e: Exception) {
                            partialData = null
                        }
                    }
                } else {
                    partialData = null
                }

            }

            ParkingMsgType.UNKNOWN -> {
                partialData = null
                //do nothing
            }
        }

    }

    private fun getType(hexList: LinkedList<String>): ParkingMsgType {
        val orNull = hexList.getOrNull(4)
        return if ("E5".equals(orNull, true)) {
            ParkingMsgType.E_FIVE
        } else if ("6E".equals(orNull, true)) {
            ParkingMsgType.SIX_E
        } else {
            ParkingMsgType.UNKNOWN
        }
    }


    private fun getLength(hexList: LinkedList<String>): Int {
        val parkingMsgType = getType(hexList)
        return if (parkingMsgType == ParkingMsgType.UNKNOWN) {
            hexList.size
        } else {
            val vr = hexList.getOrNull(1)?.toInt(16)
            val fiveHex = hexList.getOrNull(5)
            val sixHex = hexList.getOrNull(6)
            if (fiveHex != null && sixHex != null && vr != null) {
                if (vr == 100) {
                    fiveHex.toInt(16) + 8
                } else {
                    (sixHex + fiveHex).toInt(16) + 9
                }
            } else {
                0
            }
        }
    }

    private fun parseHexList(hexList: LinkedList<String>, serialPort: SerialPortEnum) {
        val da = hexList.pop()
        val vr = hexList.pop().toInt(16)
        val pn = hexList.pop() + hexList.pop()
        val cmd = hexList.pop()
        val dl =
            if (vr == 100) hexList.pop().toInt(16) else (hexList.pop() + hexList.pop()).toInt(16)
        val saveFlag = hexList.pop().toInt(16)
        val textContentNum = hexList.pop().toInt(16)
        val textContentList = mutableListOf<TextContentEntity>()
        repeat(textContentNum) {
            val lid = hexList.pop()
            val dm = hexList.pop()
            val ds = hexList.pop()
            val dt = hexList.pop().toInt(16)
            val dr = hexList.pop().toInt(16)
            val tc = hexList.pop() + hexList.pop() + hexList.pop() + hexList.pop()
            val textLength = hexList.pop().toInt(16)
            val stringBuilder = StringBuilder()
            repeat(textLength) {
                stringBuilder.append(hexList.pop())
            }
            val textContentEntity = TextContentEntity(
                lid = lid,
                dm = dm,
                ds = ds,
                dt = dt,
                dr = dr,
                tc = tc,
                textLength = textLength,
                text = BigInteger(
                    stringBuilder.toString(), 16
                ).toByteArray().toString(charset("GB2312")),
                endFlag = hexList.pop()
            )
            textContentList.add(textContentEntity)
        }
        val vf = hexList.pop()
        val vtl = hexList.pop().toInt(16)
        val voiceStringBuilder = StringBuilder()
        repeat(vtl) {
            voiceStringBuilder.append(hexList.pop())
        }
        val voiceContent =
            BigInteger(voiceStringBuilder.toString().trim(), 16).toByteArray().toString(
                charset("GB2312")
            )
        val voiceEndFlag = hexList.pop()
        val crc = hexList.pop() + hexList.pop()
        val parkingInfoEntity = ParkingInfoEntity(
            da = da,
            vr = vr,
            pn = pn,
            cmd = cmd,
            dl = dl,
            saveFlag = saveFlag,
            textContentNumber = textContentNum,
            textContentList = textContentList,
            vf = vf,
            vtl = vtl,
            voiceContent = voiceContent,
            voiceEndFlag = voiceEndFlag,
            crc = crc
        )
        try {
            SerialUtils.getInstance().sendData(
                serialPort,
                BigInteger("00 64 FF FF 6E 01 00 57 69", 16).toByteArray()
            )
            speak(voiceContent)
        } catch (_: Exception) {
        }
        viewModel.dispatch(action = Action.Release(parkingInfoEntity))
        viewModel.onEvent(event = Event.Release)
        partialData = null
        Log.d(TAG, "parseBytes: $parkingInfoEntity")
    }


    private fun parsePayList(hexList: LinkedList<String>, serialPort: SerialPortEnum) {
        val da = hexList.pop()
        val vr = hexList.pop().toInt(16)
        val pn = hexList.pop() + hexList.pop()
        val cmd = hexList.pop()
        val dl =
            if (vr == 100) hexList.pop().toInt(16) else (hexList.pop() + hexList.pop()).toInt(16)
        val sf = hexList.pop()
        val em = hexList.pop()
        val etm = hexList.pop()
        val st = hexList.pop().toInt(16)
        val ni = hexList.pop()
        val ven = hexList.pop()
        val tl = hexList.pop().toInt(16)
        val voiceStringBuilder = StringBuilder()
        if (tl > 0) {
            repeat(tl) {
                voiceStringBuilder.append(hexList.pop())
            }
        }
        val text = if (voiceStringBuilder.isNotEmpty()) {
            BigInteger(voiceStringBuilder.toString(), 16).toByteArray().toString(
                charset("GB2312")
            ).trim()
        } else ""

        val payContentEntity = PayContentEntity(
            sf = sf,
            em = em,
            etm = etm,
            st = st,
            ni = ni,
            ven = ven,
            tl = tl,
            text = text
        )
        val qrCode = hexList.dropLast(2).joinToString(separator = "")
        val crc = hexList.drop(hexList.size - 2).joinToString(separator = "")
        val payInfoEntity = PayInfoEntity(
            da = da,
            vr = vr,
            pn = pn,
            cmd = cmd,
            dl = dl,
            payContentEntity = payContentEntity,
            qrCode = qrCode,
            crc = crc
        )
        try {
            SerialUtils.getInstance().sendData(
                serialPort,
                BigInteger("00 C8 FF FF E5 01 00 00 6F 10", 16).toByteArray()
            )
            if (payContentEntity.ven == "81") {
                speak(payContentEntity.text)
            }
        } catch (_: Exception) {
        }
        viewModel.dispatch(action = Action.Payment(payInfoEntity))
        viewModel.onEvent(Event.Payment)
        partialData = null
        Log.d(TAG, "payEntity: $payInfoEntity")
    }


    override fun getResources(): Resources {
        return AdaptScreenUtils.adaptWidth(super.getResources(), 1080)
    }

    private fun checkUpdate() {
        val needUpdate = false
        if (needUpdate) {
            val manager = DownloadManager.Builder(this).run {
                apkUrl("your apk url")
                apkName("appupdate.apk")
                smallIcon(R.mipmap.ic_launcher)
                //设置了此参数，那么内部会自动判断是否需要显示更新对话框，否则需要自己判断是否需要更新
                apkVersionCode(2)
                //同时下面三个参数也必须要设置
                apkVersionName("2.0.0")
                apkSize("20.4")
                apkDescription("更新描述信息(取服务端返回数据)")
                onDownloadListener(object : OnDownloadListener {
                    override fun cancel() {

                    }

                    override fun done(apk: File) {
                        Toasty.error(baseContext, "下载完成，准备更新").show()
                        AppUtils.installApp(apk)
                    }

                    override fun downloading(max: Int, progress: Int) {
//                        dialog.setProgress((progress.toFloat() / max * 100).toInt())
                    }

                    override fun error(e: Throwable) {
                        Toasty.error(baseContext, "下载更新失败").show()
//                        dialog.dismiss()
                    }

                    override fun start() {
//                        dialog.show()
                    }

                })
                //省略一些非必须参数...
                build()
            }
            manager.download()
        }
    }

    private fun applyColorFade(view: View, fromColor: Int, toColor: Int) {
        ObjectAnimator.ofObject(
            view, "backgroundColor", ArgbEvaluator(),
            fromColor,
            toColor
        ).apply {
            duration = 2000
            start()
        }
    }

    //用于连续点击判断
    private var mHits = LongArray(COUNT)

//    private fun initNetty() {
//        mNetty = NettyUtil(object : Netty.OnChannelHandler {
//            override fun onMessageReceived(ctx: ChannelHandlerContext, msg: String?) {
//                Log.d("-----", "onMessageReceived: $msg")
//                viewModel.handleMsg(msg)
//            }
//
//            override fun onExceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {
//
//            }
//
//        }, true)
//        mNetty.setOnConnectListener(object : Netty.OnConnectListener {
//            override fun onSuccess() {
//                Log.d("-----", "onSuccess: ")
//                sendLoginMsg()
//            }
//
//            override fun onFailed() {
//                Log.d("-----", "onFailed: ")
//            }
//
//            override fun onError(e: Exception) {
//                e.message?.apply {
//                    Toasty.error(baseContext, this).show()
//                }
//                Log.d("-----", "onError:${e.message} ")
//            }
//
//        })
//        mNetty.setOnSendMessageListener(object : Netty.OnSendMessageListener {
//            override fun onSendMessage(msg: String, success: Boolean) {
//                Log.d("-----", "onSendMessage:  我发送的消息 $msg")
//            }
//
//            override fun onException(e: Throwable?) {
//
//            }
//
//        })
//
//    }
//
//    private fun sendLoginMsg() {
//        lifecycleScope.launch {
//            sendNettyMsg(
//                GsonUtils.toJson(
//                    NettyResult(
//                        msgType = MsgType.LoginSend.msgType,
//                        data = viewModel.getUUID() ?: ""
//                    )
//                )
//            )
//        }
//    }

//    private fun sendHeartBeat() {
//        sendNettyMsg(
//            GsonUtils.toJson(
//                NettyResult(
//                    msgType = MsgType.HeartBeatSend.msgType,
//                    data = "ping"
//                )
//            )
//        )
//    }
//
//    private var heartBeatJob: Job? = null

    /**
     * 每15s 发送一次心跳
     */
//    private fun startHeartBeatJob() {
//        heartBeatJob?.cancel()
//        heartBeatJob = lifecycleScope.launchWhenResumed {
//            while (true) {
//                if (mNetty.isConnected) {
//                    sendHeartBeat()
//                }
//                delay(150000)
//            }
//        }
//    }

//    private fun sendNettyMsg(msg: String) {
//        if (mNetty.isConnected) {
//            mNetty.sendMessage(msg)
//        } else {
//            Log.d("-----", "sendNettyMsg: Netty未连接到服务器 ")
////            Toasty.error(baseContext, "Netty未连接到服务器").show()
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
//        mNetty.disconnect()
    }

    object Status {
        const val OK = "ok"
        const val ERROR = "error"
    }

    companion object {
        //规定时间内连续点击
        private const val DURATION = 3000

        //连续点击次数
        private const val COUNT = 7

        //切换背景的周期
        private const val DURATION_SLIDE = 10000L

        private const val TAG: String = "MainActivityParking"


    }

    private fun speak(text: String) {
        val utteranceId = System.currentTimeMillis()
        val ttsOptions = HashMap<String, String>()
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] =
            utteranceId.toString() //utterance，这个参数随便写，用于监听播报完成的回调中
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_VOLUME] = 1.toString() //音量
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_STREAM] =
            AudioManager.STREAM_NOTIFICATION.toString() //播放类型
        val ret =
            mSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, ttsOptions)
        if (ret == TextToSpeech.SUCCESS) {
            //播报成功
        }
    }

    override fun onInit(status: Int) {
        if (this::mSpeech.isInitialized) {
            val isSupportChinese: Int = mSpeech.isLanguageAvailable(Locale.CHINESE) //是否支持中文
            TextToSpeech.getMaxSpeechInputLength() //最大播报文本长度
            if (isSupportChinese == TextToSpeech.LANG_AVAILABLE) {
                val setLanRet: Int = mSpeech.setLanguage(Locale.CHINESE) //设置语言
                val setSpeechRateRet: Int = mSpeech.setSpeechRate(1.0f) //设置语
                val setPitchRet: Int = mSpeech.setPitch(1.0f) //设置音量
                val defaultEngine: String = mSpeech.defaultEngine //默认引擎
                if (status == TextToSpeech.SUCCESS) {
                    //初始化TextToSpeech引擎成功，初始化成功后才可以play等
                }
            }
        } else {
            //初始化TextToSpeech引擎失败
        }
    }
}

class MsgAdapter : ListAdapter<String, MsgAdapter.MsgViewHolder>(object :
    DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}) {

    class MsgViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MsgViewHolder {
        val textView = TextView(parent.context).apply {
            val layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setLayoutParams(layoutParams)
            setTextColor(Color.BLACK)
            textSize = 20f
        }
        return MsgViewHolder(textView)
    }

    override fun onBindViewHolder(holder: MsgViewHolder, position: Int) {
        (holder.itemView as TextView).text = "收到消息" + getItem(position)
    }
}

enum class ParkingMsgType {
    E_FIVE, SIX_E, UNKNOWN
}