package com.top.parkingcharges.netty

import android.util.Log
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import java.util.concurrent.TimeUnit


class ConnectionListener(private val callback: () -> Unit) : ChannelFutureListener {

    @Throws(Exception::class)
    override fun operationComplete(channelFuture: ChannelFuture) {
        if (!channelFuture.isSuccess) {
            val loop = channelFuture.channel().eventLoop()
            loop.schedule({
                Log.d("-----", "callback1: ")
                callback()
            }, 2000L, TimeUnit.MILLISECONDS)
        }
    }
}