package org.example.apache.hc;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 每次获取都是得到一个新的实例，因此使用完需要用 finally { close } 或者利用 JDK 7 try-with-resources statement
 * @author gang
 *
 */
public class HttpClientUtil {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
	
	public static final int CONN_MAX_TOTAL = 256;
	public static final int CONN_MAX_PER_ROUTE = 200;
	
	private static SSLConnectionSocketFactory createCustomSSLCSFactory(
			String customeKeyStoreResource, String password, String[] supportedProtocols)
			throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException, KeyManagementException {
		InputStream inputStream = null;
		SSLConnectionSocketFactory sslsf = null;
		try {
			SSLContext sslcontext = null;
			if (customeKeyStoreResource != null && password != null) {
				inputStream = HttpClientUtil.class.getClassLoader()
					.getResourceAsStream(customeKeyStoreResource);

				KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(inputStream, password.toCharArray());

				sslcontext = SSLContexts.custom()
					.loadTrustMaterial(keyStore, new TrustSelfSignedStrategy())
					.build();
			}
			else
				sslcontext = SSLContexts.custom().build();
			
			if (supportedProtocols != null && supportedProtocols.length > 0) {
				sslsf = new SSLConnectionSocketFactory(sslcontext,
						supportedProtocols, null,
						SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);								
			}
			else
				sslsf = new SSLConnectionSocketFactory(sslcontext,
						new String[] {"TLSv1.2", "TLSv1.1"}, null,
						SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);								
				
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {}
		}
		return sslsf;
	}
	
	/**
	 * 获得自定义 KeyStore 的 CloseableHttpClient。
	 * 
	 * @return 当 Custom Key Store 错误的时候，会返回 null。
	 */
	private static CloseableHttpClient getHttpClientCustomKeyStore(String keyStoreResource, String password, String[] supportedProtocols) {
		logger.info("Execute getHttpClientCustomKeyStore ");

		// 设置超时
		RequestConfig config = RequestConfig.custom().setConnectTimeout(3000).setSocketTimeout(30000).build();

		SSLConnectionSocketFactory sslsf = null;

		try {
			sslsf = createCustomSSLCSFactory(keyStoreResource, password, supportedProtocols);
		} catch (KeyManagementException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			// 通常情况下不会出现这个错误
			logger.warn("createCustomSSLCSFactory", e);
			return null;
		}

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

		// Configure total max or per route limits for persistent connections
		// that can be kept in the pool or leased by the connection manager.
		cm.setMaxTotal(CONN_MAX_TOTAL);
		cm.setDefaultMaxPerRoute(CONN_MAX_PER_ROUTE);

		return HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionManager(cm).setDefaultRequestConfig(config)
				.build();
	}

	/**
	 * 用完需要 close
	 * @param keyStoreResource
	 * @param password
	 * @return
	 */
	public static CloseableHttpClient getHttpClientCustomKeyStore(String keyStoreResource, String password) {
		return getHttpClientCustomKeyStore(keyStoreResource, password, TLS_PROTO);
	}
	
	/**
	 * 标准的 CloseableHttpClient，用完需要 close()
	 * 
	 * @return
	 */
	public static CloseableHttpClient getHttpClient() {
		logger.info("Execute getHttpClient ");
		
		return getHttpClientCustomKeyStore(null, null, TLS_PROTO);
	}

	public static final String[] TLS_PROTO = {"TLSv1.2", "TLSv1.1"};
}
