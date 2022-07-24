package com.top.parkingcharges.netty

import android.util.Log
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import java.util.concurrent.TimeUnit


class NettyClientHandler(private val callback: () -> Unit) :
    ChannelInboundHandlerAdapter() {
    /**
     * @Author lsc
     *
     *  运行时断线重连
     * @Param [ctx]
     * @Return
     */
    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
//        val eventLoop = ctx.channel().eventLoop()
//        eventLoop.schedule({
//            Log.d("-----", "callback2: ")
//            callback.invoke()
//        }, 2000L, TimeUnit.MILLISECONDS)
        Log.d("-----", "callback2: ")
        callback.invoke()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        super.exceptionCaught(ctx, cause)
        Log.d("-----", "callback2: ")
        callback.invoke()
    }
}