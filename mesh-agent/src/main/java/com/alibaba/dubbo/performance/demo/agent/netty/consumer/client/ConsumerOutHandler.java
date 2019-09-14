/*
 * ConsumerOutHandler.java
 * Copyright 2019 Qunhe Tech, all rights reserved.
 * Qunhe PROPRIETARY/CONFIDENTIAL, any form of usage is subject to approval.
 */

package com.alibaba.dubbo.performance.demo.agent.netty.consumer.client;

import com.alibaba.dubbo.performance.demo.agent.dubbo.model.RpcResponse;
import com.alibaba.dubbo.performance.demo.agent.netty.consumer.server.ConsumerHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.AsciiString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author xinba
 */
public class ConsumerOutHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static final AsciiString CONTENT_TYPE = AsciiString.cached("Content-Type");
    private static final AsciiString CONTENT_LENGTH = AsciiString.cached("Content-Length");
    private static final AsciiString CONNECTION = AsciiString.cached("Connection");
    private static final AsciiString KEEP_ALIVE = AsciiString.cached("keep-alive");

    private final Logger logger = LoggerFactory.getLogger(ConsumerOutHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final RpcResponse msg) throws Exception {

        //todo 这里要对数据进行拆分
        final ByteBuf byteBuf = Unpooled.copiedBuffer((msg.getBytes()));
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, byteBuf);
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, KEEP_ALIVE);
        final Channel serverChannel = ConsumerHandler.channels.get().remove(msg.getRequestId());
        if (serverChannel == null) {
            return;
        }
        serverChannel.writeAndFlush(response).addListener(future -> {
            if (future.isSuccess()) {
                logger.info("ConsumerServer---consumer data send successfully");
            } else {
                future.cause().printStackTrace();
            }
        });
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
