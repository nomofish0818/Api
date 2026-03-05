package org.Zcz.gateway.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.ReferenceCountUtil;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GatewayInboundHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 这里的 HttpClient 实例，未来随着项目复杂度的提升，可以抽象提取到 org.Zcz.gateway.client 包下
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final String TARGET_URL = "http://httpbin.org/get";

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest nettyRequest) {
        // 1. 增加引用计数，防止 SimpleChannelInboundHandler 在方法结束时自动释放它
        nettyRequest.retain();

        // 2. 判断客户端是否要求 Keep-Alive
        boolean keepAlive = HttpUtil.isKeepAlive(nettyRequest);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(TARGET_URL))
                .GET();

        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((response, throwable) -> {
                    try {
                        FullHttpResponse nettyResponse;
                        if (throwable != null) {
                            System.err.println("转发失败: " + throwable.getMessage());
                            nettyResponse = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
                        } else {
                            nettyResponse = new DefaultFullHttpResponse(
                                    HttpVersion.HTTP_1_1,
                                    HttpResponseStatus.valueOf(response.statusCode()),
                                    Unpooled.wrappedBuffer(response.body())
                            );
                        }

                        // 3. 处理 Keep-Alive 和 Content-Length 头部
                        nettyResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, nettyResponse.content().readableBytes());
                        if (keepAlive) {
                            nettyResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                            ctx.writeAndFlush(nettyResponse); // 不加 CLOSE listener
                        } else {
                            ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
                        }
                    } finally {
                        // 4. 异步处理彻底结束，手动释放最初始的请求内存
                        ReferenceCountUtil.release(nettyRequest);
                    }
                });
    }
}