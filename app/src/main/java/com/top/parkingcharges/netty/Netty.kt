package com.top.parkingcharges.netty

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext

interface Netty {
    /**
     * 建立连接
     *
     * @param host
     * @param port
     */
    fun connect(host: String, port: Int)

    /**
     * 重连
     */
    fun reconnect(delayMillis: Long)

    /**
     * 发送消息  消息目前全部是string发送
     *
     * @param msg
     */
    fun sendMessage(msg: String)

    /**
     * 设置连接监听
     *
     * @param listener
     */
    fun setOnConnectListener(listener: OnConnectListener)

    /**
     * 设置消息发送监听
     *
     * @param listener
     */
    fun setOnSendMessageListener(listener: OnSendMessageListener)

    /**
     * 关闭
     */
    fun close()

    /**
     * 断开链接
     */
    fun disconnect()

    /**
     * 是否连接
     *
     * @return
     */
    val isConnected: Boolean

    /**
     * 是否打开
     *
     * @return
     */
    val isOpen: Boolean

    /**
     * 获取[ChannelFuture]
     *
     * @return
     */
    val channelFuture: ChannelFuture?

    /**
     * 连接监听
     */
    interface OnConnectListener {
        fun onSuccess()
        fun onFailed()
        fun onError(e: Exception)
    }

    /**
     * 通道消息处理（接收消息）
     */
    interface OnChannelHandler {
        fun onMessageReceived(ctx: ChannelHandlerContext, msg: String?)
        fun onExceptionCaught(ctx: ChannelHandlerContext, e: Throwable)
    }

    /**
     * 发送消息监听
     */
    interface OnSendMessageListener {
        fun onSendMessage(msg: String, success: Boolean)
        fun onException(e: Throwable?)
    }
}