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
	
    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
            "\u00A0-\uD7FF" +
            "\uF900-\uFDCF" +
            "\uFDF0-\uFFEF" +
            "\uD800\uDC00-\uD83F\uDFFD" +
            "\uD840\uDC00-\uD87F\uDFFD" +
            "\uD880\uDC00-\uD8BF\uDFFD" +
            "\uD8C0\uDC00-\uD8FF\uDFFD" +
            "\uD900\uDC00-\uD93F\uDFFD" +
            "\uD940\uDC00-\uD97F\uDFFD" +
            "\uD980\uDC00-\uD9BF\uDFFD" +
            "\uD9C0\uDC00-\uD9FF\uDFFD" +
            "\uDA00\uDC00-\uDA3F\uDFFD" +
            "\uDA40\uDC00-\uDA7F\uDFFD" +
            "\uDA80\uDC00-\uDABF\uDFFD" +
            "\uDAC0\uDC00-\uDAFF\uDFFD" +
            "\uDB00\uDC00-\uDB3F\uDFFD" +
            "\uDB44\uDC00-\uDB7F\uDFFD" +
            "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;
    
	public static final int PORT = 18080;
	
	@Test
	public void testStartAndDestoy() throws InterruptedException {
		try (final HttpServer endpoint = new HttpServer()) {
			ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
		
			Thread.sleep(1000); 
		}
	}
	
	@Test
	public void testGet() throws InterruptedException, ClientProtocolException, IOException, URISyntaxException {
		String expectedContent = "Hello world";
		String url = "/hello";		
		
		try (final HttpServer endpoint = new HttpServer()) {
			endpoint.get(url, (request) -> expectedContent);
		
			ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
				
			Assert.assertEquals(expectedContent, testHTTPGet(
				"http://localhost:" + PORT + url, null));
		
		}		
		
		/*Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				endpoint.close();
			}
		});
		
		future.channel().closeFuture().syncUninterruptibly();
		*/
	}

	@Test
	public void testGetWithParams() throws InterruptedException, ClientProtocolException, IOException, URISyntaxException {
		String expectedContent = "Hello world";
		String url = "/hello";
		String urlRegrex = url + "\\?(?:(?:[" + LABEL_CHAR
            + ";/\\?:@&=#~"  // plus optional query params
            + "\\-\\.\\+!\\*'\\(\\),_\\$])|(?:%[a-fA-F0-9]{2}))*";
		
		String A_1 = "用户名username";
		String V_1 = "vip";
		String A_2 = "密码项password";
		String V_2 = "密码password";		
		Map<String, String> getParams = new HashMap<String, String>();
		getParams.put(A_1, V_1);
		getParams.put(A_2, V_2);
		
		try (final HttpServer endpoint = new HttpServer()) {
			endpoint.get(urlRegrex, (request) -> {
				boolean foundA_1 = false;
				boolean foundA_2 = false;
				
				QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.getUri());
				Map<String, List<String>> params = queryStringDecoder.parameters();
				if (!params.isEmpty()) {
					for (Entry<String, List<String>> p : params.entrySet()) {
						String key = p.getKey();
						List<String> vals = p.getValue();
						for (String val : vals) {
							logger.info("{} {}", key, val);
				            if (A_1.equals(key)) {
				            	foundA_1 = true;
				            	Assert.assertEquals(V_1, val);		          
				            } else if (A_2.equals(key)) {
				            	foundA_2 = true;
				            	Assert.assertEquals(V_2, val);
				            }							
						}
					}
				}
				
				Assert.assertTrue(foundA_1 && foundA_2);
				return expectedContent;
			});
			
			ChannelFuture future = endpoint.start(new InetSocketAddress(PORT));
					
			Assert.assertEquals(expectedContent, testHTTPGet(
					"http://localhost:" + PORT + url, getParams));
		
		}
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
		
		endpoint.close();
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
		
		endpoint.close();
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
