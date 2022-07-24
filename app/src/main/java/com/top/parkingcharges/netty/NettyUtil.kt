/*
 * Copyright 2019 Jenly Yu
 * <a href="mailto:jenly1314@gmail.com">Jenly</a>
 * <a href="https://github.com/jenly1314">jenly1314</a>
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.top.parkingcharges.netty

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Log
import com.top.parkingcharges.netty.Netty.*
import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.concurrent.Future

class NettyUtil : Netty {
    /**
     * 是否debug，true则会显示打印日志
     */
    private var isDebug: Boolean = true
    private lateinit var mHandlerThread: HandlerThread
    private lateinit var mHandler: Handler
    private lateinit var mMainHandler: Handler
    private var mHost: String = ""
    private var mPort = 0
    override var channelFuture: ChannelFuture? = null
        private set
    private lateinit var mBootstrap: Bootstrap
    private var mGroup: EventLoopGroup? = null
    private var mChannelInitializer: ChannelInitializer<SocketChannel>? = null
    private var mOnConnectListener: OnConnectListener? = null
    private var mOnSendMessageListener: OnSendMessageListener? = null

    /**
     * 构造
     * @param onChannelHandler [OnChannelHandler]
     */
    constructor(onChannelHandler: OnChannelHandler?) : this(onChannelHandler, false) {}

    /**
     * 构造
     * @param onChannelHandler [OnChannelHandler]
     * @param isDebug
     */
    constructor(onChannelHandler: OnChannelHandler?, isDebug: Boolean) {
        this.isDebug = isDebug
        mChannelInitializer = object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                //建立管道
                val channelPipeline = ch.pipeline()
                //添加相关编码器，解码器，处理器等
                channelPipeline
                    .addLast(StringEncoder())
                    .addLast(StringDecoder())
                    .addLast(NettyClientHandler {
                        reconnect(0)
                    })
                    .addLast(object : StringChannelHandler() {
                        @Throws(Exception::class)
                        override fun channelRead0(ctx: ChannelHandlerContext, msg: String?) {
                            super.channelRead0(ctx, msg)
                            if (isDebug) {
                                Log.d(TAG, "Received message:$msg")
                            }

                            if (onChannelHandler != null) {
                                mMainHandler.post { onChannelHandler.onMessageReceived(ctx, msg) }
                            }
                        }

                        @Throws(Exception::class)
                        override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                            super.exceptionCaught(ctx, cause)
                            if (isDebug) {
                                Log.w(TAG, cause.message!!)
                            }
                            if (onChannelHandler != null) {
                                mMainHandler.post {
                                    onChannelHandler.onExceptionCaught(
                                        ctx,
                                        cause
                                    )
                                }
                            }
                        }
                    })
            }
        }
        initHandlerThread()
        mHandler.sendEmptyMessage(NETTY_INIT)
    }

    /**
     * 构造
     * @param channelInitializer [ChannelInitializer]
     * @param isDebug
     */
    constructor(channelInitializer: ChannelInitializer<SocketChannel>?, isDebug: Boolean) {
        this.isDebug = isDebug
        mChannelInitializer = channelInitializer
        initHandlerThread()
        mHandler.sendEmptyMessage(NETTY_INIT)
    }
    /**
     * 构造
     * @param bootstrap [Bootstrap]
     * @param isDebug
     */
    /**
     * 构造
     * @param bootstrap [Bootstrap]
     */
    @JvmOverloads
    constructor(bootstrap: Bootstrap, isDebug: Boolean = false) {
        mBootstrap = bootstrap
        this.isDebug = isDebug
        mGroup = bootstrap.config().group()
        initHandlerThread()
    }

    private fun initHandlerThread() {
        mMainHandler = Handler(Looper.getMainLooper())
        mHandlerThread = HandlerThread(NettyUtil::class.java.simpleName)
        mHandlerThread.start()
        mHandler = object : Handler(mHandlerThread.looper) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when (msg.what) {
                    NETTY_INIT -> handleNettyInit()
                    NETTY_CONNECT -> handleConnect()
                    NETTY_SEND_MESSAGE -> handleSendMessage(msg.obj as String)
                }
            }
        }
    }

    fun getHost(): String {
        return mHost
    }

    fun getPort(): Int {
        return mPort
    }

    private fun handleNettyInit() {
        mBootstrap = Bootstrap()
        mBootstrap.channel(NioSocketChannel::class.java)
        mGroup = NioEventLoopGroup()
        mBootstrap.group(mGroup)
            .option(ChannelOption.TCP_NODELAY, true) // 消息立即发出去
            .option(ChannelOption.SO_REUSEADDR, true)
            .handler(LoggingHandler(if (isDebug) LogLevel.DEBUG else LogLevel.INFO))
        mBootstrap.handler(mChannelInitializer)
    }

    private fun handleConnect() {
        try {
            channelFuture = mBootstrap.connect(mHost, mPort)
                .addListener(ConnectionListener {
                    reconnect(0)
                })
                .addListener { future: Future<in Void?> ->
                    val isSuccess = future.isSuccess
                    if (isDebug) {
                        if (isSuccess) {
                            Log.d(TAG, "Netty connect success.")
                        } else {
                            Log.d(TAG, "Netty connect failed.")
                        }
                    }
                    if (mOnConnectListener != null) {
                        Log.d(TAG, "handleConnecteeeor: ")
                        mMainHandler.post {
                            if (isSuccess) {
                                mOnConnectListener?.onSuccess()
                            } else {
                                Log.d(TAG, "handleConnecteeeor: ")
                                mOnConnectListener?.onFailed()
                            }
                        }
                    }
                }
                .sync()
        } catch (e: Exception) {
            e.printStackTrace()
            if (mOnConnectListener != null) {
                Log.d(TAG, "handleConnecteeeor:onError ")
                mMainHandler.post { mOnConnectListener?.onError(e) }
            }
        }
    }

    private fun handleSendMessage(msg: String) {
        try {
            if (isOpen) {
                channelFuture?.channel()?.writeAndFlush(msg)
                    ?.addListener { future: Future<in Void?> ->
                        val isSuccess = future.isSuccess
                        if (isDebug) {
                            if (isSuccess) {
                                Log.d(TAG, "Send message:$msg")
                            } else {
                                Log.d(TAG, "Send failed.")
                            }
                        }
                        mOnSendMessageListener?.apply {
                            mMainHandler.post {
                                onSendMessage(
                                    msg,
                                    isSuccess
                                )
                            }
                        }
                    }?.sync()
            } else {
                if (mOnSendMessageListener != null) {
                    mMainHandler.post { mOnSendMessageListener?.onSendMessage(msg, false) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (mOnSendMessageListener != null) {
                mMainHandler.post { mOnSendMessageListener?.onException(e) }
            }
        }
    }

    override fun connect(host: String, port: Int) {
        if (isConnected) {
            return
        }
        mHost = host
        mPort = port
        mHandler.sendEmptyMessage(NETTY_CONNECT)
    }

    override fun reconnect(delayMillis: Long) {
        if (isConnected) {
            return
        }
        close()
        mHandler.sendEmptyMessageDelayed(NETTY_CONNECT, delayMillis)
    }

    override fun sendMessage(msg: String) {
        mHandler.obtainMessage(NETTY_SEND_MESSAGE, msg).sendToTarget()
    }

    override fun setOnConnectListener(listener: OnConnectListener) {
        mOnConnectListener = listener
    }

    override fun setOnSendMessageListener(listener: OnSendMessageListener) {
        mOnSendMessageListener = listener
    }

    override fun close() {
        if (isOpen) {
            channelFuture?.channel()?.close()
            if (isDebug) {
                Log.d(TAG, "Netty channel connect closed.")
            }
        }
    }

    override fun disconnect() {
        if (isConnected) {
            channelFuture?.channel()?.disconnect()
            if (isDebug) {
                Log.d(TAG, "Netty channel disconnected.")
            }
        }
    }

    override val isConnected: Boolean
        get() {
            val isConnected = channelFuture != null && channelFuture?.channel()?.isActive ?: false
            if (isDebug && !isConnected) {
                Log.w(TAG, "Netty channel is not connected.")
            }
            return isConnected
        }
    override val isOpen: Boolean
        get() {
            val isOpen = channelFuture != null && channelFuture?.channel()?.isOpen ?: false
            if (isDebug && !isOpen) {
                Log.w(TAG, "Netty channel is not opened.")
            }
            return isOpen
        }

    companion object {
        const val TAG = "ANetty"
        private const val NETTY_INIT = 0x01
        private const val NETTY_CONNECT = 0x02
        private const val NETTY_SEND_MESSAGE = 0x03
        const val DEFAULT_CHARSET = "UTF-8"

        /**
         * 默认消息结束分隔符
         */
        val DEFAULT_DELIMITER = byteArrayOf(0x04)

        /**
         * 默认传输内容最大长度 128K
         */
        const val DEFAULT_MAX_FRAME_LENGTH = 131072
    }
}