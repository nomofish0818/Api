package org.Zcz.gateway.core;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;

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
        System.out.println("收到请求，正通过原生 HttpClient 转发至: " + TARGET_URL);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(TARGET_URL))
                .GET();

        httpClient.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        System.err.println("转发失败: " + throwable.getMessage());
                        FullHttpResponse errorResponse = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);
                        ctx.writeAndFlush(errorResponse).addListener(ChannelFutureListener.CLOSE);
                    } else {
                        FullHttpResponse nettyResponse = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                HttpResponseStatus.valueOf(response.statusCode()),
                                Unpooled.wrappedBuffer(response.body())
                        );

                        nettyResponse.headers().setInt(
                                HttpHeaderNames.CONTENT_LENGTH,
                                nettyResponse.content().readableBytes()
                        );

                        ctx.writeAndFlush(nettyResponse).addListener(ChannelFutureListener.CLOSE);
                    }
                });
    }
}