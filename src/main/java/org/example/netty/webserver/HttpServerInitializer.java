package org.example.netty.webserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

public class HttpServerInitializer extends ChannelInitializer<Channel> {
	private static final Logger logger = LoggerFactory.getLogger(HttpServerInitializer.class);
	
	protected RouteTable routeTable;
	
	public HttpServerInitializer(RouteTable r) {
		routeTable = r;
	}
	
	@Override
	protected void initChannel(Channel ch) throws Exception {
		ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new HttpContentCompressor());
		pipeline.addLast(new ChunkedWriteHandler());
		pipeline.addLast(new HttpObjectAggregator(64 * 1024));
		
		addHttpServerHandler(pipeline);
	}

	protected void addHttpServerHandler(ChannelPipeline pipeline) {
		pipeline.addLast(new HttpRequestHandler(routeTable));
	}
}
