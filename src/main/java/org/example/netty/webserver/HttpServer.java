package org.example.netty.webserver;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.Future;

public class HttpServer {
	private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);

	private Channel channel;
	
	private final EventLoopGroup group;
    
	private final RouteTable routeTable;
	
	public HttpServer() {
		if (Epoll.isAvailable()) {
			group = new EpollEventLoopGroup();
		} else {
			group = new NioEventLoopGroup();					
		}
		
		channel = null;
		routeTable = new RouteTable();				
	}
		
	public ChannelFuture start(InetSocketAddress address) {
		ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		
		bootstrap.group(group)
			.channel(Epoll.isAvailable()?EpollServerSocketChannel.class:NioServerSocketChannel.class)
			//.handler(new LoggingHandler(LogLevel.INFO))
			.childHandler(createChannelInitializer(routeTable));
		
		ChannelFuture future = bootstrap.bind(address);
		future.syncUninterruptibly();
		channel = future.channel();
		return future;
	}
	
	protected ChannelInitializer<Channel> createChannelInitializer(RouteTable r) {
		return new HttpServerInitializer(r);
	}
	
	public void destroy() {
		if (channel != null)
			channel.close();
		
		Future<?> future = group.shutdownGracefully();
		future.syncUninterruptibly();
	}
	
    /**
     * Adds a GET route.
     *
     * @param path The URL path.
     * @param handler The request handler.
     * @return This WebServer.
     */
    public HttpServer get(final String path, final Handler handler) {
        this.routeTable.addRoute(new Route(HttpMethod.GET, Pattern.compile(path), handler));
        return this;
    }


    /**
     * Adds a POST route.
     *
     * @param path The URL path.
     * @param handler The request handler.
     * @return This WebServer.
     */
    public HttpServer post(final String path, final Handler handler) {
        this.routeTable.addRoute(new Route(HttpMethod.POST, Pattern.compile(path), handler));
        return this;
    }	
}
