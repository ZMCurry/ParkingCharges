package com.top.parkingcharges.netty

import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

@Sharable
open class StringChannelHandler : SimpleChannelInboundHandler<String?>() {
    /**
     * 接收消息
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun channelRead0(ctx: ChannelHandlerContext, msg: String?) {
    }

    @Throws(Exception::class)
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        super.exceptionCaught(ctx, cause)
        cause.printStackTrace()
        ctx.close()
    }
}