package com.top.parkingcharges

sealed class MsgType(val msgType: Int) {
    //登录发送本机信息
    object LoginSend : MsgType(1001)
    //确认收到登录信息
    object LoginReceive : MsgType(1002)
    //心跳发送
    object HeartBeatSend : MsgType(1003)
    //心跳接受
    object HeartBeatReceive : MsgType(1004)
    //确认收到收费信息
    object PaymentConfirm : MsgType(1005)
    //收到收费信息
    object PaymentReceive : MsgType(1006)
    //放行信息确认
    object ReleaseConfirm : MsgType(1007)
    //显示放行内容
    object ReleaseReceive : MsgType(1008)
}