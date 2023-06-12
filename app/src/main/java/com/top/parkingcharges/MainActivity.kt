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
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.azhon.appupdate.listener.OnDownloadListener
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.constant.PermissionConstants
import com.blankj.utilcode.util.AdaptScreenUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.FileIOUtils
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.PathUtils
import com.blankj.utilcode.util.PermissionUtils
import com.blankj.utilcode.util.ToastUtils
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

//    private lateinit var mNetty: NettyUtil

    private val hexArray =
        LinkedList(
            "00 64 FF FF 6E 6D 00 04 00 15 01 19 00 FF 00 00 00 08 D2 BB C2 B7 CB B3 B7 E7 0D 01 15 01 19 00 00 FF 00 00 09 BB A6 41 41 50 36 33 37 35 0D 02 15 01 19 00 FF 00 00 00 06 C1 D9 CA B1 B3 B5 0D 03 15 01 19 00 FF 00 00 00 08 D0 BB D0 BB BB DD B9 CB 00 0A 1D BB A6 41 41 50 36 33 37 35 2C C1 D9 CA B1 B3 B5 2C D7 A3 C4 FA D2 BB C2 B7 CB B3 B7 E7 00 5C F3".split(
                " "
            )
        )

    private val payArray = LinkedList(
        "00 C8 FF FF E5 2F 01 01 00 01 78 00 81 76 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 20 BB A6 42 39 39 31 48 35 2C CD A3 B3 B5 30 D0 A1 CA B1 34 35 B7 D6 D6 D3 31 38 C3 EB 2C C7 EB BD C9 B7 D1 35 D4 AA 42 4D B2 00 00 00 00 00 00 00 3E 00 00 00 28 00 00 00 1D 00 00 00 E3 FF FF FF 01 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 FF FF FF 00 00 00 00 00 FE C7 7B F8 82 9D 42 08 BA BA 8A E8 BA AF C2 E8 BA 94 32 E8 82 54 92 08 FE AA AB F8 00 B3 08 00 56 E2 CE F8 F0 FB 5D 18 2E 95 FD 38 70 38 38 38 4B A5 A5 A8 10 88 AA 80 8E CB E4 50 45 AA 3B A0 6B 7B 35 70 50 EA E8 A8 0E 54 C2 60 B9 5D CB 48 A7 57 4F D0 00 D7 68 A0 FE 77 6A A0 82 A4 E8 A0 BA 67 7F B8 BA 9A 86 78 BA 0A EE E8 82 91 1C F0 FE 44 4D C0 AE 9C".split(
            " "
        )
    )

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var navController: NavController

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

        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(baseContext)
            adapter = msgAdapter
        }

        mSpeech = TextToSpeech(baseContext, this)
        navController =
            (supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment).navController

//        initNetty()

//        lifecycleScope.launchWhenStarted {
//            viewModel.event.collectLatest {
//                when (it) {
//                    is Event.SendMsg -> {
//                        sendNettyMsg(it.msg)
//                    }
//
//                    is Event.LoginEvent -> {
//                        if (it.succeed) {
//                            Toasty.success(baseContext, getString(R.string.login_success)).show()
//                            startHeartBeatJob()
//                        }
//                    }
//                }
//            }
//        }

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

        val navOptions: NavOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right) //进入动画
            .setExitAnim(R.anim.slide_out_left) //退出动画
            .setPopEnterAnim(R.anim.slide_in_left) //弹出进入动画
            .setPopExitAnim(R.anim.slide_out_right) //弹出退出动画
            .build()
        lifecycleScope.launch {
            viewModel.event.collectLatest {
                when (it) {
                    Event.Idle -> {
                        navController.navigate(R.id.idleStateFragment, args = null, navOptions)
                    }

                    Event.Payment -> {
                        navController.navigate(R.id.paymentStateFragment, args = null, navOptions)
                    }

                    Event.Release -> {
                        navController.navigate(R.id.releaseStateFragment, args = null, navOptions)
                    }
                }
            }
        }

//        lifecycleScope.launchWhenCreated {
//            var index = 0
//            if (viewModel.backgroundColors.size == 0) {
//                return@launchWhenCreated
//            }
//            while (true) {
//                if (viewModel.backgroundColors.size > 1) {
//                    val current = viewModel.backgroundColors[index]
//                    val next = if (index == viewModel.backgroundColors.size - 1) {
//                        viewModel.backgroundColors[0]
//                    } else viewModel.backgroundColors[index + 1]
//                    applyColorFade(
//                        binding.rlRoot,
//                        current,
//                        next
//                    )
//                    if (index == viewModel.backgroundColors.size - 1) {
//                        index = 0
//                    } else {
//                        index++
//                    }
//                }
//                delay(DURATION_SLIDE)
//            }
//        }

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
//        val dex =
//            "1064FFFF6E5400020015011900FF00000008BBB6D3ADB9E2C1D90D011501190000FF000019CBD5424D38303753C1D9CAB1B3B5CAA3D3E0B3B5CEBB3A3738000A18CBD5424D383037532CC1D9CAB1B3B52CBBB6D3ADB9E2C1D9002870"

//        val dex1 =
//            "00,C8,FF,FF,E5,2F,01,01,00,01,78,00,81,76,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,D5,E3,4A,38,32,4D,37,32,2C,CD,A3,B3,B5,30,D0,A1,CA,B1,35,30,B7,D6,D6,D3,31,39,C3,EB,2C,C7,EB,BD,C9,B7,D1,32,D4,AA,42,4D,B2,00,00,00,00,00,00,00,3E,00,00,00,28,00,00,00,1D,00,00,00,E3,FF,FF,FF,01,00,01,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00"
//        val dex2 =
//            "FF,FF,FF,00,00,00,00,00,FE,9D,4B,F8,82,D7,CA,08,BA,27,8A,E8,BA,6E,22,E8,BA,BF,92,E8,82,E2,32,08,FE,AA,AB,F8,00,6B,58,00,F6,72,ED,98,D1,46,8B,68,E2,59,E3,68,34,B0,B0,B0,87,69,69,68,14,66,BC,B8,FF,18,E5,A0,B8,67,7F,68,CB,B8,45,80,ED,B9,DB,B8,8B,15,76,78,65,18,9F,E0,07,C2,AF,B8,00,1E,A8,D0,FE,45,4A,88,82,97,D8,90,BA,54,4F,88,BA,D6,4A,B0,BA,C6,22,20,82,DD,D1,38,FE,F7,7F,F0,A3,78"
//        val dex3 =
//            "00,64,FF,FF,6E,4D,00,02,00,15,01,19,00,FF,00,00,00,08,D2,BB,C2,B7,CB,B3,B7,E7,0D,01,15,01,19,00,00,FF,00,00,0E,D5,E3,4A,38,32,4D,37,32,C1,D9,CA,B1,B3,B5,00,0A,1C,D5,E3,4A,38,32,4D,37,32,2C,C1,D9,CA,B1,B3,B5,2C,D7,A3,C4,FA,D2,BB,C2,B7,CB,B3,B7,E7,00,27,62"

//        val dex1 = "00,C8,FF,FF,E5,2E,01,01,00,01,78,00,81,75,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20"
//        val dex2 = "20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,CB,D5,45,39,38,43,50,37,2C,CD,A3,B3,B5,30,D0,A1,CA,B1,34,38,B7,D6,D6,D3,32,C3,EB,2C,C7,EB,BD,C9,B7,D1,32,D4,AA,42,4D,B2,00,00,00,00,00,00,00,3E,00,00,00,28,00,00,00,1D,00,00,00,E3,FF,FF,FF,01,00,01,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,FF,FF,FF,00,00,00,00,00,FE,9D,4B,F8,82,D7,CA,08,BA,27,8A,E8,BA,6E,22,E8,BA,BF,92,E8,82,E2,32,08,FE,AA,AB,F8,00,6B,58,00,F6,72,ED,98,D1,46,8B,68,E2,59,E3,68,34,B0,B0,B0,87,69,69,68,14,66,BC,B8,FF,18,E5,A0,B8,67,7F,68,CB,B8,45,80,ED,B9,DB,B8,8B,15,76,78,65,18,9F,E0,07,C2,AF,B8,00,1E,A8,D0,FE,45,4A,88,82,97,D8,90,BA,54,4F,88,BA,D6,4A,B0,BA,C6,22,20,82,DD,D1,38,FE,F7,7F,F0,F9,D2"
//        val dex3 = "00,64,FF,FF,6E,4D,00,02,00,15,01,19,00,FF,00,00,00,08,D2,BB,C2,B7,CB,B3,B7,E7,0D,01,15,01,19,00,00,FF,00,00,0E,CB,D5,45,39,38,43,50,37,C1,D9,CA,B1,B3,B5,00,0A,1C,CB,D5,45,39,38,43,50,37,2C,C1,D9,CA,B1,B3,B5,2C,D7,A3,C4,FA,D2,BB,C2,B7,CB,B3,B7,E7,00,AE,7D"

//        val dex1 = "00,C8,FF,FF,E5,2F,01,01,00,01,78,00,81,76,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,CD,EE,42,39,43,38,39,31,2C,CD,A3,B3,B5,31,D0,A1,CA,B1,35,38,B7,D6,D6,D3,33,39,C3,EB,2C,C7,EB,BD,C9,B7,D1,35,D4,AA,42,4D,B2,00,00,00,00,00,00,00,3E,00,00,00,28,00,00,00,1D,00,00,00,E3,FF,FF,FF,01,00,01,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,FF,FF,FF,00,00,00,00,00,FE,9D,4B,F8,82,D7,CA,08,BA,27,8A,E8,BA,6E,22,E8,BA,BF,92,E8,82,E2,32,08,FE,AA,AB,F8,00,6B,58,00,F6,72,ED,98,D1,46,8B,68,E2,59,E3,68,34,B0,B0,B0,87,69,69,68,14,66,BC,B8,FF,18,E5,A0,B8,67,7F,68,CB,B8,45,80,ED,B9,DB,B8,8B,15,76,78,65,18,9F,E0,07,C2,AF,B8,00,1E,A8,D0,FE,45,4A,88,82,97,D8,90,BA,54,4F,88,BA,D6,4A,B0,BA,C6,22,20,82,DD,D1,38,FE,F7,7F,F0,C3,6D"
//        val dex2 = "00,64,FF,FF,6E,4D,00,02,00,15,01,19,00,FF,00,00,00,08,D2,BB,C2,B7,CB,B3,B7,E7,0D,01,15,01,19,00,00,FF,00,00,0E,CD,EE,42,39,43,38,39,31,C1,D9,CA,B1,B3,B5,00,0A,1C,CD,EE,42,39,43,38,39,31"
//        val dex3 = "2C,C1,D9,CA,B1,B3,B5,2C,D7,A3,C4,FA,D2,BB,C2,B7,CB,B3,B7,E7,00,7A,0C"
//

//        val dex1 = "00,64,FF,FF,6E,4D,00,02,00,15,01,19,00,FF,00,00,00,08,D2,BB,C2,B7,CB,B3,B7,E7,0D,01,15,01,19,00,00,FF,00,00,0E,CB,D5,45,4D,4A,32,36,31,C1,D9,CA,B1,B3,B5,00,0A,1C,CB,D5,45,4D,4A,32,36,31,2C,C1,D9,CA,B1,B3,B5,2C,D7,A3,C4,FA,D2,BB,C2,B7,CB,B3,B7,E7,00,39,46"
////        val dex2 = "00,C8,FF,FF,E5,2F,01,01,00,01,78,00,81,76,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,20,CF,E6,46,51,45,33,37,33,2C,CD,A3,B3,B5,38,D0,A1,CA,B1,32,38,B7,D6,D6,D3,31,34,C3,EB,2C,C7,EB,BD,C9,B7,D1,35,D4,AA,42,4D,B2,00,00,00,00,00,00,00,3E,00,00,00,28,00,00,00,1D,00,00,00,E3,FF,FF,FF,01,00,01,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,00,FF,FF,FF,00,00,00,00,00,FE,9D,4B,F8,82,D7,CA,08,BA,27,8A,E8,BA,6E,22,E8,BA,BF,92,E8,82,E2,32,08,FE,AA,AB,F8,00,6B,58,00,F6,72,ED,98,D1,46,8B,68,E2,59,E3,68,34,B0,B0,B0,87,69,69,68,14,66,BC,B8,FF,18,E5,A0,B8,67,7F,68,CB,B8,45,80,ED,B9,DB,B8,8B,15,76,78,65,18,9F,E0,07,C2,AF,B8,00,1E,A8,D0,FE,45,4A,88,82,97,D8,90,BA,54,4F,88,BA,D6,4A,B0,BA,C6,22,20,82,DD,D1,38,FE,F7,7F,F0,67,EE"
//        lifecycleScope.launch {
//            repeat(1) {
//              val list =   LinkedList(
//                    BigInteger(dex1.replace(",", ""), 16).toByteArray()
//                        .joinToString(separator = ",") { eachByte ->
//                            "%02x".format(eachByte)
//                        }.split(",")
//                ).apply {
//                    add(0,"00")
//                }
////                val list = when (it) {
////                    0 -> {
////                        LinkedList(
////                            BigInteger(dex1.replace(",", ""), 16).toByteArray()
////                                .joinToString(separator = ",") { eachByte ->
////                                    "%02x".format(eachByte)
////                                }.split(",")
////                        ).apply {
////                            add(0,"00")
////                        }
////                    }
////
////                    else -> {
////                        LinkedList(
////                            BigInteger(dex2.replace(",", ""), 16).toByteArray()
////                                .joinToString(separator = ",") { eachByte ->
////                                    "%02x".format(eachByte)
////                                }.split(",")
////                        )
////                    }
////                }
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
                    try {
                        parseHexList(
                            LinkedList(list.subList(0, length)),
                            serialPortEnum
                        )
                        val linkedList = LinkedList(list.subList(length - 1, list.size))
                        handleList(
                            linkedList,
                            getLength(linkedList),
                            parkingMsgType,
                            serialPortEnum
                        )
                    } catch (_: Exception) {
                    }
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
                    try {
                        parsePayList(
                            LinkedList(list.subList(0, length)),
                            serialPortEnum
                        )
                        val linkedList = LinkedList(list.subList(length - 1, list.size))
                        handleList(
                            linkedList,
                            getLength(linkedList),
                            parkingMsgType,
                            serialPortEnum
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
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