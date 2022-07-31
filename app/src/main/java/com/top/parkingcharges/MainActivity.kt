package com.top.parkingcharges

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.res.Resources
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.azhon.appupdate.listener.OnDownloadListener
import com.azhon.appupdate.manager.DownloadManager
import com.blankj.utilcode.util.AdaptScreenUtils
import com.blankj.utilcode.util.AppUtils
import com.blankj.utilcode.util.GsonUtils
import com.top.parkingcharges.databinding.ActivityMainBinding
import com.top.parkingcharges.entity.NettyResult
import com.top.parkingcharges.fragment.HostDialogFragment
import com.top.parkingcharges.netty.Netty
import com.top.parkingcharges.netty.NettyUtil
import com.top.parkingcharges.viewmodel.Event
import com.top.parkingcharges.viewmodel.MainViewModel
import com.top.parkingcharges.viewmodel.Page
import com.top.parkingcharges.viewmodel.dataStore
import es.dmoral.toasty.Toasty
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.util.*


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var mNetty: NettyUtil

    private lateinit var binding: ActivityMainBinding

    private val viewModel by viewModels<MainViewModel>()

    private lateinit var navController: NavController

    private lateinit var mSpeech: TextToSpeech

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

        mSpeech = TextToSpeech(baseContext, this)
        navController =
            (supportFragmentManager.findFragmentById(R.id.fcv) as NavHostFragment).navController

        initNetty()

        lifecycleScope.launchWhenStarted {
            viewModel.event.collectLatest {
                when (it) {
                    is Event.SendMsg -> {
                        sendNettyMsg(it.msg)
                    }
                    is Event.LoginEvent -> {
                        if (it.succeed) {
                            Toasty.success(baseContext, getString(R.string.login_success)).show()
                            startHeartBeatJob()
                            //测试代码
//                            lifecycleScope.launch {
//                                delay(5000)
//                                if (mNetty.isConnected) {
//                                    sendNettyMsg("test")
//                                }
//                            }
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.newestHost.collectLatest {
                mNetty.disconnect()
                delay(300)
                mNetty.connect(it.host, it.port)
            }
        }

        lifecycleScope.launchWhenCreated {
            val firstOrNull = baseContext.dataStore.data.map { preferences ->
                val host = preferences[stringPreferencesKey("host")]
                val port = preferences[intPreferencesKey("port")]
                if (!host.isNullOrEmpty() && port != null) {
                    Pair(host, port)
                } else {
                    null
                }
            }.firstOrNull()
            if (firstOrNull != null) {
                mNetty.connect(firstOrNull.first, firstOrNull.second)
            }
        }
        val navOptions: NavOptions = NavOptions.Builder()
            .setEnterAnim(R.anim.slide_in_right) //进入动画
            .setExitAnim(R.anim.slide_out_left) //退出动画
            .setPopEnterAnim(R.anim.slide_in_left) //弹出进入动画
            .setPopExitAnim(R.anim.slide_out_right) //弹出退出动画
            .build()
        lifecycleScope.launch {
            viewModel.viewState.distinctUntilChangedBy {
                it.page
            }.collectLatest {
                when (it.page) {
                    Page.IDLE -> {
                        navController.navigate(R.id.idleStateFragment, args = null, navOptions)
                    }
                    Page.PAYMENT -> {
                        navController.navigate(R.id.paymentStateFragment, args = null, navOptions)
                    }
                    Page.RELEASE -> {
                        navController.navigate(R.id.releaseStateFragment, args = null, navOptions)
                    }
                }
            }
        }

        lifecycleScope.launchWhenCreated {
            var index = 0
            if (viewModel.backgroundColors.size == 0) {
                return@launchWhenCreated
            }
            while (true) {
                if (viewModel.backgroundColors.size > 1) {
                    val current = viewModel.backgroundColors[index]
                    val next = if (index == viewModel.backgroundColors.size - 1) {
                        viewModel.backgroundColors[0]
                    } else viewModel.backgroundColors[index + 1]
                    applyColorFade(
                        binding.rlRoot,
                        current,
                        next
                    )
                    if (index == viewModel.backgroundColors.size - 1) {
                        index = 0
                    } else {
                        index++
                    }
                }
                delay(DURATION_SLIDE)
            }
        }

        lifecycleScope.launchWhenCreated {
            val firstOrNull = viewModel.getUUID()
            binding.tvUuid.text = firstOrNull
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

        lifecycleScope.launch {
            delay(3000)
            speak()
        }
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

    private fun initNetty() {
        mNetty = NettyUtil(object : Netty.OnChannelHandler {
            override fun onMessageReceived(ctx: ChannelHandlerContext, msg: String?) {
                Log.d("-----", "onMessageReceived: $msg")
                viewModel.handleMsg(msg)
            }

            override fun onExceptionCaught(ctx: ChannelHandlerContext, e: Throwable) {

            }

        }, true)
        mNetty.setOnConnectListener(object : Netty.OnConnectListener {
            override fun onSuccess() {
                Log.d("-----", "onSuccess: ")
                sendLoginMsg()
            }

            override fun onFailed() {
                Log.d("-----", "onFailed: ")
            }

            override fun onError(e: Exception) {
                e.message?.apply {
                    Toasty.error(baseContext, this).show()
                }
                Log.d("-----", "onError:${e.message} ")
            }

        })
        mNetty.setOnSendMessageListener(object : Netty.OnSendMessageListener {
            override fun onSendMessage(msg: String, success: Boolean) {
                Log.d("-----", "onSendMessage:  我发送的消息 $msg")
            }

            override fun onException(e: Throwable?) {

            }

        })

    }

    private fun sendLoginMsg() {
        lifecycleScope.launch {
            sendNettyMsg(
                GsonUtils.toJson(
                    NettyResult(
                        msgType = MsgType.LoginSend.msgType,
                        data = viewModel.getUUID() ?: ""
                    )
                )
            )
        }
    }

    private fun sendHeartBeat() {
        sendNettyMsg(
            GsonUtils.toJson(
                NettyResult(
                    msgType = MsgType.HeartBeatSend.msgType,
                    data = "ping"
                )
            )
        )
    }

    private var heartBeatJob: Job? = null

    /**
     * 每15s 发送一次心跳
     */
    private fun startHeartBeatJob() {
        heartBeatJob?.cancel()
        heartBeatJob = lifecycleScope.launchWhenResumed {
            while (true) {
                if (mNetty.isConnected) {
                    sendHeartBeat()
                }
                delay(150000)
            }
        }
    }

    private fun sendNettyMsg(msg: String) {
        if (mNetty.isConnected) {
            mNetty.sendMessage(msg)
        } else {
            Log.d("-----", "sendNettyMsg: Netty未连接到服务器 ")
//            Toasty.error(baseContext, "Netty未连接到服务器").show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mNetty.disconnect()
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

    }

    private fun speak() {
        val utteranceId = System.currentTimeMillis()
        val ttsOptions = HashMap<String, String>()
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] =
            utteranceId.toString() //utterance，这个参数随便写，用于监听播报完成的回调中
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_VOLUME] = 1.toString() //音量
        ttsOptions[TextToSpeech.Engine.KEY_PARAM_STREAM] =
            AudioManager.STREAM_NOTIFICATION.toString() //播放类型
        val ret = mSpeech.speak("停车时间5小时3分钟，请交费25元", TextToSpeech.QUEUE_FLUSH, ttsOptions)
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