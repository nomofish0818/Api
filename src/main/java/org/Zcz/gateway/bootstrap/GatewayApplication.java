package org.Zcz.gateway.bootstrap; // 替换成你实际的包名

import org.Zcz.gateway.core.GatewayServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GatewayApplication implements CommandLineRunner {

    public static void main(String[] args) {
        // 1. 先启动 Spring 容器
        SpringApplication.run(GatewayApplication.class, args);
    }

    /**
     * Spring 容器启动完成后，会自动回调这个 run 方法
     */
    @Override
    public void run(String... args) throws Exception {
        // 2. 在这里启动你的 Netty 网关服务
        // 这里的 GatewayServer 就是你之前写的那段 Netty 启动代码所在类的名字
        GatewayServer server = new GatewayServer(8080);
        server.start();
    }
}