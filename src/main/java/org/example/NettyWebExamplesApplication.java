package org.example;

import java.net.InetSocketAddress;

import org.example.netty.webserver.HttpServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import io.netty.channel.ChannelFuture;

@SpringBootApplication
public class NettyWebExamplesApplication {
	@Autowired
	HelloHandler helloHandler;

	@Bean(destroyMethod = "close")
	HttpServer httpServer() {
		HttpServer httpd = new HttpServer();
		
		// setup http handlers
		httpd.get("/hello", helloHandler);
		
		return httpd;
	}
		
	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(NettyWebExamplesApplication.class, args);
		
		HttpServer httpd = ctx.getBean(HttpServer.class);
		//httpd.get("/hello", (request) -> "hello");
			
		ChannelFuture future = httpd.start(new InetSocketAddress(8080));

/*		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				httpd.destroy();
			}
		});*/

		future.channel().closeFuture().syncUninterruptibly();
	}
}
