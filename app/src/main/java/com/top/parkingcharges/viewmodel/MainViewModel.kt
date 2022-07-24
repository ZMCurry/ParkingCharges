package com.top.parkingcharges.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.JsonSyntaxException
import com.top.parkingcharges.App
import com.top.parkingcharges.MainActivity
import com.top.parkingcharges.MsgType
import com.top.parkingcharges.entity.NettyResult
import com.top.parkingcharges.entity.PaymentInfo
import com.top.parkingcharges.entity.ReleaseInfo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*


val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val mApplication = application
    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private val _viewState = MutableStateFlow(ViewState())
    val viewState = _viewState.asStateFlow()

    private val _newestHost = MutableSharedFlow<HostPort>()
    val newestHost = _newestHost.asSharedFlow()

    fun updateHost(hostPort: HostPort) {
        viewModelScope.launch {
            mApplication.dataStore.edit { preferences ->
                preferences[stringPreferencesKey("host")] = hostPort.host
                preferences[intPreferencesKey("port")] = hostPort.port
            }
            _newestHost.emit(hostPort)
        }
    }

    suspend fun getUUID(): String? {
        return getApplication<App>().dataStore.data.map {
            val uuid = UUID.randomUUID().toString()
            val value = it[stringPreferencesKey("uuid")]
                ?: kotlin.run {
                    getApplication<App>().dataStore.edit { mutablePreferences ->
                        mutablePreferences[stringPreferencesKey("uuid")] = uuid
                    }
                    uuid
                }
            value
        }.firstOrNull()
    }

    fun sendMessage(msg: String) {
        viewModelScope.launch {
            _event.emit(Event.SendMsg(msg))
        }
    }

    fun dispatch(action: Action) {
        viewModelScope.launch {
            val state = _viewState.value
            when (action) {
                is Action.Payment -> {
                    _event.emit(
                        Event.SendMsg(
                            GsonUtils.toJson(
                                NettyResult(
                                    msgType = MsgType.PaymentConfirm.msgType,
                                    data = if (action.paymentInfo == null) MainActivity.Status.ERROR
                                    else MainActivity.Status.OK
                                )
                            )
                        )
                    )
                    emit(state.copy(page = Page.PAYMENT, paymentInfo = action.paymentInfo))
                }
                is Action.Release -> {
                    _event.emit(
                        Event.SendMsg(
                            GsonUtils.toJson(
                                NettyResult(
                                    msgType = MsgType.ReleaseConfirm.msgType,
                                    data = if (action.releaseInfo == null) MainActivity.Status.ERROR
                                    else MainActivity.Status.OK
                                )
                            )
                        )
                    )
                    emit(state.copy(page = Page.RELEASE, releaseInfo = action.releaseInfo))
                }
            }
        }
    }

    private fun emit(viewState: ViewState) {
        _viewState.value = viewState
    }

    fun handleMsg(msg: String?) {
        msg?.let { json ->
            viewModelScope.launch {
                try {
                    val nettyResult = GsonUtils.fromJson(json, NettyResult::class.java)
                    val data = nettyResult.data
                    when (nettyResult.msgType) {
                        MsgType.LoginReceive.msgType -> {
                            _event.emit(
                                Event.LoginEvent(
                                    MainActivity.Status.OK.equals(
                                        data,
                                        true
                                    )
                                )
                            )
                        }
                        MsgType.PaymentReceive.msgType -> {
                            try {
                                val paymentInfo = GsonUtils.fromJson(data, PaymentInfo::class.java)
                                dispatch(Action.Payment(paymentInfo))
                            } catch (e: JsonSyntaxException) {
                                dispatch(Action.Payment(null))
                            }
                        }
                        MsgType.ReleaseReceive.msgType -> {
                            try {
                                val releaseInfo =
                                    GsonUtils.fromJson(data, ReleaseInfo::class.java)
                                dispatch(Action.Release(releaseInfo))
                            } catch (e: JsonSyntaxException) {
                                dispatch(Action.Release(null))
                            }
                        }
                    }
                } catch (e: JsonSyntaxException) {

                }
            }
        }
    }

    //这里的列表至少是两个元素，否则无法切换
    val backgroundColors = arrayListOf(
        Color.parseColor("#939391"), Color.parseColor("#7B8B6F"),
        Color.parseColor("#656565"), Color.parseColor("#6B5152"),
        Color.parseColor("#8696A7"), Color.parseColor("#965454"),
        Color.BLACK
    )
}

data class ViewState(
    val page: Page = Page.IDLE,
    val paymentInfo: PaymentInfo? = null,
    val releaseInfo: ReleaseInfo? = null,
    val defaultQrCode: String = "haha"
)

data class HostPort(val host: String, val port: Int)

enum class Page {
    IDLE, PAYMENT, RELEASE
}

//事件，只触发一次，不随着生命周期重复触发
sealed class Event {
    data class SendMsg(val msg: String) : Event()
    data class LoginEvent(val succeed: Boolean) : Event()
}

sealed class Action {
    data class Payment(val paymentInfo: PaymentInfo?) : Action()
    data class Release(val releaseInfo: ReleaseInfo?) : Action()
}