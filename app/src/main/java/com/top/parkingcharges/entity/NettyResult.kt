package com.top.parkingcharges.entity

import androidx.annotation.Keep

import com.google.gson.annotations.SerializedName


data class NettyResult(val msgType: Int, val data: String)

@Keep
data class PaymentInfo(
    @SerializedName("fee")
    val fee: Int,
    @SerializedName("plateNo")
    val plateNo: String?,
    @SerializedName("plateType")
    val plateType: String,
    @SerializedName("qrCode")
    val qrCode: String,
    @SerializedName("time")
    val time: Int
)


//放行信息
@Keep
data class ReleaseInfo(
    @SerializedName("plateNo")
    val plateNo: String?,
    @SerializedName("plateType")
    val plateType: String,
    @SerializedName("msg1")
    val msg1: String,
    @SerializedName("msg2")
    val msg2: String
)