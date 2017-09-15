package org.example.netty.webserver;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.ReferenceCountUtil;

public class WebRequestHandler extends HttpRequestHandler {

	private String wsUri;
	
	public WebRequestHandler(RouteTable r, String wsUri) {
		super(r);
		
		this.wsUri = wsUri;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (wsUri.equalsIgnoreCase(request.getUri())) {
			ctx.fireChannelRead(ReferenceCountUtil.retain(request));
		} else
			super.channelRead0(ctx, request);
	}

}
