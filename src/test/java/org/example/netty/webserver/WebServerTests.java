package org.example.netty.webserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebServerTests {

	private final Logger logger = LoggerFactory.getLogger(WebServerTests.class);
	
	public static final int PORT = 18080;
	
	static final String WSURI = "/ws";
	
	@Test
	public void testStartAndDestoy() throws InterruptedException {
		final WebServer endpoint = new WebServer(WSURI);
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
		
		Thread.sleep(1000); 
		
		endpoint.destroy();
	}
	
	@Test
	public void testGet() throws InterruptedException, ClientProtocolException, IOException, URISyntaxException {
		String expectedContent = "Hello world";
		String url = "/hello";		
		
		final WebServer endpoint = new WebServer(WSURI);
		endpoint.get(url, (request) -> expectedContent);
		
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
				
		Assert.assertEquals(expectedContent, HttpServerTests.testHTTPGet(
				"http://localhost:" + PORT + url, null));
		
		endpoint.destroy();
		
		/*
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				endpoint.destroy();
			}
		});
		
		future.channel().closeFuture().syncUninterruptibly();
		*/
	}
	
	@Test
	public void testPost() throws InterruptedException, ClientProtocolException, IOException {
		String url = "/login";		
		String expectedContent = "OK";
		String A_1 = "用户名username";
		String V_1 = "vip";
		String A_2 = "密码项password";
		String V_2 = "密码password";
		
		List <NameValuePair> nvps = new ArrayList <NameValuePair>();
		nvps.add(new BasicNameValuePair(A_1, V_1));
		nvps.add(new BasicNameValuePair(A_2, V_2)); //URLEncoder.encode("密码password", "UTF-8")));
		
		final WebServer endpoint = new WebServer(WSURI);
		endpoint.post(url, (request) -> {
			boolean foundA_1 = false;
			boolean foundA_2 = false;

			HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
					new DefaultHttpDataFactory(false), request);
			try {
		        List<InterfaceHttpData> interfaceHttpDatas = decoder.getBodyHttpDatas();
		        for (InterfaceHttpData data : interfaceHttpDatas) {
		        	if (data.getHttpDataType() != HttpDataType.Attribute)
		        		continue;
		            Attribute attribute = (Attribute) data;
		            logger.info("{}:{}", attribute.getName(), attribute.getValue()); //URLDecoder.decode(attribute.getValue(), "UTF-8"));
		            if (A_1.equals(attribute.getName())) {
		            	foundA_1 = true;
		            	Assert.assertEquals(V_1, attribute.getValue());		          
		            } else if (A_2.equals(attribute.getName())) {
		            	foundA_2 = true;
		            	Assert.assertEquals(V_2, attribute.getValue());
		            }
		        }
			}
			finally {
				decoder.destroy();
			}
			
			Assert.assertTrue(foundA_1 && foundA_2);
			
			return expectedContent;
		});
		
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
				
		Assert.assertEquals(expectedContent, HttpServerTests.testHTTPPost(
				"http://localhost:" + PORT + url, nvps));
		
		endpoint.destroy();
	}
	
	@Test
	//@Ignore
	public void testWebSocketHandshake() throws InterruptedException {
		final WebServer endpoint = new WebServer(WSURI);
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
		
		SocketListener l = new SocketListener();
		WebSocket ws = testWebSocket(l);

	    Thread.sleep(1000);
	    
	    Assert.assertTrue(l.isOpened());		
				
	    ws.close(1000, null);
	    
	    Thread.sleep(1000);
	    Assert.assertTrue(!l.isOpened());
	    
	    endpoint.destroy();
	}
	
	public static WebSocket testWebSocket(SocketListener l) throws InterruptedException {
		OkHttpClient client = new OkHttpClient.Builder()
				.readTimeout(3,  TimeUnit.SECONDS)
				.retryOnConnectionFailure(true)
				.build();
		
		Request request = new Request.Builder()
	                .url("http://localhost:" + PORT + WSURI)
	                .build();
			
	    return client.newWebSocket(request, l);	    
	}
	
    private static class SocketListener extends WebSocketListener {
    	private static final Logger logger = LoggerFactory.getLogger(SocketListener.class);
    	
    	private volatile boolean opened = false;
    	
        @Override
        public void onOpen(WebSocket webSocket, Response response) {
        	this.opened = true;
        	logger.debug("onOpen");
        	//webSocket.send("hello");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
        	logger.debug(text);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
        	this.opened = false;
        	logger.debug("onClosed: {} {}", code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            //disconnect();
        }

		public boolean isOpened() {
			return opened;
		}

		public void setOpened(boolean opened) {
			this.opened = opened;
		}
                
    }	
}
