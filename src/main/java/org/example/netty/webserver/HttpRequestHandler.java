package org.example.netty.webserver;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.example.netty.webserver.util.HttpStaticFileHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;

public class HttpRequestHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
	private static final Logger logger = LoggerFactory.getLogger(HttpRequestHandler.class);
	
	public static final String TYPE_PLAIN = "text/plain; charset=UTF-8";
	public static final String TYPE_JSON = "application/json; charset=UTF-8";
	public static final String SERVER_NAME = "Netty";

	private RouteTable routeTable;

	public HttpRequestHandler(RouteTable r) {
		this.routeTable = r;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("Received connection from {}", ctx.channel().remoteAddress());
		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.debug("Connection to {} closed", ctx.channel().remoteAddress());
		super.channelInactive(ctx);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
		if (HttpUtil.is100ContinueExpected(request)) {
			send100Continue(ctx);
		}

		final HttpMethod method = request.getMethod();
		final String uri = request.getUri();

		final Route route = this.routeTable.findRoute(method, uri);
		if (route == null) {
			sendNotFound(ctx, request);
			return;
		}

		try {
			final Object obj = route.getHandler().handle(request);
			if (obj instanceof File) {
				File file = (File) obj;
				writeFile(ctx, request, file);
			} else {
				final String content = obj == null ? "" : obj.toString();
				writeResponse(ctx, request, HttpResponseStatus.OK, TYPE_PLAIN, content);
			}
		} catch (final Exception ex) {
			logger.warn("", ex);
			sendInternalServerError(ctx, request);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("exceptionCaught", cause);
		ctx.close();
	}

	/**
	 * Writes a 404 Not Found response.
	 *
	 * @param ctx
	 *            The channel context.
	 * @param request
	 *            The HTTP request.
	 */
	public static void sendNotFound(final ChannelHandlerContext ctx, final FullHttpRequest request) {
		writeErrorResponse(ctx, request, HttpResponseStatus.NOT_FOUND);
	}

	/**
	 * Writes a 500 Internal Server Error response.
	 *
	 * @param ctx
	 *            The channel context.
	 * @param request
	 *            The HTTP request.
	 */
	public static void sendInternalServerError(final ChannelHandlerContext ctx, final FullHttpRequest request) {
		writeErrorResponse(ctx, request, HttpResponseStatus.INTERNAL_SERVER_ERROR);
	}

	/**
	 * Writes a HTTP error response.
	 *
	 * @param ctx
	 *            The channel context.
	 * @param request
	 *            The HTTP request.
	 * @param status
	 *            The error status.
	 */
	private static void writeErrorResponse(final ChannelHandlerContext ctx, final FullHttpRequest request,
			final HttpResponseStatus status) {

		writeResponse(ctx, request, status, TYPE_PLAIN, status.reasonPhrase().toString());
	}

	/**
	 * Writes a HTTP response.
	 *
	 * @param ctx
	 *            The channel context.
	 * @param request
	 *            The HTTP request.
	 * @param status
	 *            The HTTP status code.
	 * @param contentType
	 *            The response content type.
	 * @param content
	 *            The response content.
	 */
	private static void writeResponse(final ChannelHandlerContext ctx, final FullHttpRequest request,
			final HttpResponseStatus status, final CharSequence contentType, final String content) {

		final byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		final ByteBuf entity = Unpooled.wrappedBuffer(bytes);
		writeResponse(ctx, request, status, entity, contentType, bytes.length);
	}

	/**
	 * Writes a HTTP response.
	 *
	 * @param ctx
	 *            The channel context.
	 * @param request
	 *            The HTTP request.
	 * @param status
	 *            The HTTP status code.
	 * @param buf
	 *            The response content buffer.
	 * @param contentType
	 *            The response content type.
	 * @param contentLength
	 *            The response content length;
	 */
	private static void writeResponse(final ChannelHandlerContext ctx, final FullHttpRequest request,
			final HttpResponseStatus status, final ByteBuf buf, final CharSequence contentType,
			final int contentLength) {
		// Build the response object.
		final FullHttpResponse response = new DefaultFullHttpResponse(
				request.protocolVersion(), status, buf, false);
		
		// Decide whether to close the connection or not.
		final boolean keepAlive = HttpUtil.isKeepAlive(request);
		if (keepAlive) {
			response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}
		
		final ZonedDateTime dateTime = ZonedDateTime.now();
		final DateTimeFormatter formatter = DateTimeFormatter.RFC_1123_DATE_TIME;

		final DefaultHttpHeaders headers = (DefaultHttpHeaders) response.headers();
		headers.set(HttpHeaderNames.SERVER, SERVER_NAME);
		headers.set(HttpHeaderNames.DATE, dateTime.format(formatter));
		headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
		headers.set(HttpHeaderNames.CONTENT_LENGTH, Integer.toString(contentLength));

		ChannelFuture future = ctx.writeAndFlush(response);
		
		// Close the non-keep-alive connection after the write operation is
		// done.
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static void writeFile(final ChannelHandlerContext ctx, final FullHttpRequest request,
			final File file) throws Exception {
		HttpStaticFileHelper.servFile(ctx, request, file);
	}
	
	/**
	 * Writes a 100 Continue response.
	 *
	 * @param ctx
	 *            The HTTP handler context.
	 */
	public static void send100Continue(final ChannelHandlerContext ctx) {
		FullHttpResponse response = new DefaultFullHttpResponse(
				HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
		ctx.writeAndFlush(response);
	}
}
