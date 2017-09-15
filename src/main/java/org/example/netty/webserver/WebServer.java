package org.example.netty.webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class WebServer extends HttpServer {
	private static final Logger logger = LoggerFactory.getLogger(WebServer.class);

	private String wsUri;
	
	public WebServer(String wsUri) {
		super();
		
		this.wsUri = wsUri;
	}
	
	@Override
	protected ChannelInitializer<Channel> createChannelInitializer(RouteTable r) {
		return new WebServerInitializer(r, wsUri);
	}
	
	
}
