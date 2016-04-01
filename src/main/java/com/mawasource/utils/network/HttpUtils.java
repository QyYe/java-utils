package com.mawasource.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpUtils {

	private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);
	
	/**
	 * send a default http-post request
	 * 
	 * @param url
	 * @param body
	 * @param username
	 * @param password
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String get(final String url, final String username, final String password)
			throws ClientProtocolException, IOException {
		return get(buildClient(url, username, password), url);
	}
	
	/**
	 * send a default http-post request
	 * 
	 * @param url
	 * @param body
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String get(final String url)
			throws ClientProtocolException, IOException {
		return get(buildClient(url, null, null), url);
	}
	
	/**
	 * send a default http-post request
	 * 
	 * @param url
	 * @param body
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String post(final String url, final String body)
			throws ClientProtocolException, IOException {
		return post(buildClient(url, null, null), body, null, null);
	}
	
	/**
	 * send a default http-post request
	 * 
	 * @param url
	 * @param parameters
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String post(final String url, final List<NameValuePair> parameters)
			throws ClientProtocolException, IOException {
		return post(buildClient(url, null, null), url, new UrlEncodedFormEntity(parameters), null);
	}

	/**
	 * send a default http-post request
	 * 
	 * @param url
	 * @param body
	 * @param username
	 * @param password
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String post(final String url, final String body, final String username, final String password)
			throws ClientProtocolException, IOException {
		return post(url, body, null, username, password);
	}

	/**
	 * send a https-post request with ignoring an insecure or invalid
	 * certificate
	 * 
	 * @param url
	 * @param body
	 * @param contentType
	 * @param username
	 * @param password
	 * @return response as string
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	public static String post(final String url, final String body, final String contentType, final String username,
			final String password) throws ClientProtocolException, IOException {
		return post(buildClient(url, username, password), url, new ByteArrayEntity(body.getBytes("UTF-8")), contentType);
	}

	/**
	 * sends a post request with the given http client
	 * 
	 * @param client
	 *            apache http client
	 * @param url
	 *            where the post request is sent to
	 * @param entity
	 *            http entity
	 * @param contentType
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static String post(HttpClient client, final String url, final HttpEntity entity, final String contentType)
			throws ClientProtocolException, IOException {

		final HttpPost post = new HttpPost(url);
		post.setEntity(entity);
		if (contentType != null) {
			post.setHeader("Content-Type", contentType);
		}

		return getResponse(client.execute(post));
		
	}
	
	/**
	 * sends a get request with the given http client
	 * 
	 * @param client
	 *            apache http client
	 * @param url
	 *            where the post request is sent to
	 * @param body
	 *            post body
	 * @param contentType
	 * @return
	 * @throws UnsupportedOperationException 
	 * @throws ClientProtocolException
	 * @throws IOException
	 */
	private static String get(HttpClient client, final String url) throws UnsupportedOperationException, ClientProtocolException, IOException {
		return getResponse(client.execute(new HttpGet(url)));
	}
	
	private static HttpClient buildClient(String url, String username, String password) {
		HttpClientBuilder builder = createHttpClientBuilder(username, password);
		builder.disableContentCompression();
		HttpClient client = url.startsWith("https") ? createSSLHttpClient(builder) : builder.build();
		return client;
	}
	
	
	private static String getResponse(HttpResponse response) throws UnsupportedOperationException, IOException {
		logger.debug("HttpPost-Response Code : " + response.getStatusLine().getStatusCode());

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		return result.toString();
	}

	/**
	 * creates an apache http client which ignores ssl certificate errors
	 * 
	 * @return
	 */
	private static HttpClient createSSLHttpClient(HttpClientBuilder builder) {
		SSLContext sslContext;
		try {
			sslContext = SSLContexts.custom().useProtocol("TLSv1").loadTrustMaterial(new TrustSelfSignedStrategy()).build();
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
			Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory> create().register("https", sslsf).build();
			HttpClientConnectionManager ccm = new BasicHttpClientConnectionManager(registry);
			builder.setConnectionManager(ccm);

		} catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
			logger.error("error on creating custom http client", e);
		}

		return builder.build();
	}

	private static CredentialsProvider buildBasicCredentialProvider(String username, String password) {
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
		provider.setCredentials(AuthScope.ANY, credentials);
		return provider;
	}

	private static HttpClientBuilder createHttpClientBuilder(String username, String password) {
		HttpClientBuilder builder = HttpClientBuilder.create();
		if (username != null && password != null) {
			builder.setDefaultCredentialsProvider(buildBasicCredentialProvider(username, password));
		}
		return builder;
	}
}
