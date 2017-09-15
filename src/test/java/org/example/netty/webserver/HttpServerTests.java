package org.example.netty.webserver;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.example.apache.hc.HttpClientUtil;
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

public class HttpServerTests {
	private static final Logger logger = LoggerFactory.getLogger(HttpServerTests.class);
	
	public static final int PORT = 18080;
	
	@Test
	public void testStartAndDestoy() throws InterruptedException {
		final HttpServer endpoint = new HttpServer();
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
		
		Thread.sleep(1000); 
		
		endpoint.destroy();
	}
	
	@Test
	public void testGet() throws InterruptedException, ClientProtocolException, IOException, URISyntaxException {
		String expectedContent = "Hello world";
		String url = "/hello";		
		
		final HttpServer endpoint = new HttpServer();
		endpoint.get(url, (request) -> expectedContent);
		
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
				
		Assert.assertEquals(expectedContent, testHTTPGet(
				"http://localhost:" + PORT + url, null));
		
		endpoint.destroy();
		
		
		/*Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				endpoint.destroy();
			}
		});
		
		future.channel().closeFuture().syncUninterruptibly();
		*/
	}

	@Test
	@Ignore
	public void testGetWithParams() throws InterruptedException, ClientProtocolException, IOException, URISyntaxException {
		String expectedContent = "Hello world";
		String url = "/hello";		
		String A_1 = "用户名username";
		String V_1 = "vip";
		String A_2 = "密码项password";
		String V_2 = "密码password";		
		Map<String, String> getParams = new HashMap<String, String>();
		getParams.put(A_1, V_1);
		getParams.put(A_2, V_2);
		
		final HttpServer endpoint = new HttpServer();
		endpoint.get(url, (request) -> {
			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
			Map<String, List<String>> params = queryStringDecoder.parameters();
			if (!params.isEmpty()) {
				for (Entry<String, List<String>> p : params.entrySet()) {
					String key = p.getKey();
					List<String> vals = p.getValue();
					for (String val : vals) {
						logger.info(val);
					}
				}
			}
			return expectedContent;
		});
		
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
				
		Assert.assertEquals(expectedContent, testHTTPGet(
				"http://localhost:" + PORT + url, getParams));
		
		endpoint.destroy();
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
		
		final HttpServer endpoint = new HttpServer();
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
				
		Assert.assertEquals(expectedContent, testHTTPPost(
				"http://localhost:" + PORT + url, nvps));
		
		endpoint.destroy();
	}
	
	@Test
	public void testStaticFile() throws ClientProtocolException, IOException, URISyntaxException {
		String url = "/static/components.png";
		final HttpServer endpoint = new HttpServer();
		endpoint.get(url, request -> {
			URL location = HttpRequestHandler.class.getProtectionDomain().getCodeSource().getLocation();
			String path = location.toURI() + url;
			path = !path.contains("file:") ? path:path.substring(5);
			logger.debug(path);
			return new File(path);
		});
		
		ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
		
		testHTTPGet("http://localhost:" + PORT + url, null);		
		
		endpoint.destroy();
	}
	
	public static  String testHTTPGet(String url, Map<String, String> params) throws ClientProtocolException, IOException, URISyntaxException {
		try (CloseableHttpClient httpclient = HttpClientUtil.getHttpClient()) {
			URI baseuri = new URI(url);
			URIBuilder builder = new URIBuilder()
					.setScheme(baseuri.getScheme())
					.setHost(baseuri.getHost())
					.setPort(baseuri.getPort())
					.setPath(baseuri.getPath());
			if (params != null && !params.isEmpty()) {
				for (Entry<String, String> entry: params.entrySet()) {
					builder.setParameter(entry.getKey(), entry.getValue());
				}
			}
			
			HttpGet httpget = new HttpGet(builder.build());
			
			logger.info("Executing request {}", httpget.getRequestLine());
			
			try (CloseableHttpResponse response = httpclient.execute(httpget)) {
				System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				
				if (entity != null) {
					//EntityUtils.consume(entity);
					return EntityUtils.toString(entity);
				}
				return null;
			}
		}
	}
	
	public static String testHTTPPost(String url, List <NameValuePair> nvps) throws ClientProtocolException, IOException {
		try (CloseableHttpClient httpclient = HttpClientUtil.getHttpClient()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));			
			try (CloseableHttpResponse response = httpclient.execute(httpPost)) {
				System.out.println("----------------------------------------");
				System.out.println(response.getStatusLine());

				// Get hold of the response entity
				HttpEntity entity = response.getEntity();
				
				if (entity != null) {
					//EntityUtils.consume(entity);
					return EntityUtils.toString(entity);
				}
				return null;
			}
		}
	}	
}
