package com.fta.testengine;

import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;

public class TcpClient {
    private static final String TAG = "test_engine";
    //Disconnect code
    public static final int ERR_CODE_NETWORK_INVALID = 0;
    public static final int ERR_CODE_TIMEOUT = 1;
    public static final int ERR_CODE_UNKNOWN = 2;
    public static final int ERR_CODE_DISCONNECTED = 3;

    private EventLoopGroup mGroup;
    private Bootstrap mBootstrap;
    private TcpClientListener mListener;
    private ChannelFuture mChannelFuture;
    private boolean isConnected = false;

    public TcpClient() {
        mGroup = new NioEventLoopGroup();
        mBootstrap = new Bootstrap();
        mBootstrap.group(mGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        Log.i(TAG, "TcpClient-initChannel: ");
                        ch.pipeline().addLast("decoder", new FrameDecoder());
                        ch.pipeline().addLast("handler", new MsgHandler());
                    }
                });
    }

    public boolean isConnected() {
        return isConnected;
    }


    /**
     * 建立连接
     *
     * @param host Server host
     * @param port Server port
     * @return
     * @author swallow
     * @createTime 2016/2/16
     * @lastModify 2016/2/16
     */
    public void connect(final String host, final int port) {
        try {
            if (isConnected) {
                return;
            }
            mChannelFuture = mBootstrap.connect(host, port);
            Log.i(TAG, "TcpClient-connect: " + (mChannelFuture == null));
//             mChannelFuture.sync();
//             mChannelFuture.channel().closeFuture();
        } catch (Exception e) {
            Log.i(TAG, "TcpClient-connect: " + e.getMessage());
            mGroup.shutdownGracefully();
            if (mListener != null)
                mListener.disConnected(ERR_CODE_TIMEOUT, e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * 断开连接
     *
     * @param
     * @return
     * @author swallow
     * @createTime 2016/1/29
     * @lastModify 2016/1/29
     */
    public void disConnect() {
        if (mChannelFuture == null)
            return;
        if (!mChannelFuture.channel().isActive())
            return;
        mChannelFuture.channel().close();
        mChannelFuture.channel().closeFuture();
        mGroup.shutdownGracefully();
    }


    /**
     * 消息发送
     *
     * @param
     * @return
     * @author swallow
     * @createTime 2016/1/29
     * @lastModify 2016/1/29
     */
    public void send(final byte[] data) {
        final ByteBuf byteBuf = Unpooled.copiedBuffer(data);
        mChannelFuture.channel()
                .writeAndFlush(byteBuf)
                .addListener(
                        new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                if (mListener != null)
                                    mListener.sent(data);
                            }
                        }
                );
    }


    /**
     * 设置TcpClient监听
     *
     * @param
     * @return
     * @author swallow
     * @createTime 2016/1/29
     * @lastModify 2016/1/29
     */
    public void setTcpClentListener(TcpClientListener listener) {
        this.mListener = listener;
    }


    /**
     * @className: FrameDecoder
     * @classDescription: Decode to frames
     * @author: swallow
     * @createTime: 2015/11/23
     */
    private class FrameDecoder extends ByteToMessageDecoder {
        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            out.add(data);
// // 不够4个字节则不予解析
// if (in.readableBytes() < 4) {
// return;
// }
// in.markReaderIndex(); // mina in.mark();
//
// // 获取帧长度
// byte[] headers = new byte[4];
// in.readBytes(headers);
// int frameLen = Utils.bytesToInt(headers);
// if (frameLen > in.readableBytes()) {
// in.resetReaderIndex();
// return;
// }
//
// //获取一个帧
// byte[] data = new byte[frameLen];
// in.readBytes(data);
// out.add(data);
        }
    }


    /**
     * @className: MsgHandler
     * @classDescription: 经过FrameDecoder的消息处理器
     * @author: swallow
     * @createTime: 2015/11/23
     */
    private class MsgHandler extends SimpleChannelInboundHandler<byte[]> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, byte[] msg) throws Exception {
            Log.i(TAG, "MsgHandler-channelRead0: ");
            if (mListener != null)
                mListener.received(msg);
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            Log.i(TAG, "MsgHandler-channelActive: ");
            if (mListener != null) {
                isConnected = true;
                mListener.connected();
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Log.i(TAG, "MsgHandler-channelInactive: ");
            if (mListener != null) {
                isConnected = false;
                mListener.disConnected(ERR_CODE_DISCONNECTED, "The connection is disconnected!");
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            Log.i(TAG, "MsgHandler-exceptionCaught: ");
            super.exceptionCaught(ctx, cause);
        }
    }


}
