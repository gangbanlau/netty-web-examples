package org.example.netty.webserver;

import java.util.regex.Pattern;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;

public class WebServerInitializer extends HttpServerInitializer {

	private String wsUri;
	
	public WebServerInitializer(RouteTable r, String wsUri) {
		super(r);
		this.wsUri = wsUri;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		super.initChannel(ch);
		
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new WebSocketServerCompressionHandler());
		pipeline.addLast(new WebSocketServerProtocolHandler(this.wsUri, null, true));
		pipeline.addLast(new TextWebSocketFrameHandler());
	}

	@Override
	protected void addHttpServerHandler(ChannelPipeline pipeline) {
		pipeline.addLast(new WebRequestHandler(routeTable, wsUri));
	}

}
