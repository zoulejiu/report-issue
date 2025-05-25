package com.github;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class HttpClient {
    private CloseableHttpClient httpClient;
    private final Gson gson;
    private final Map<String, String> heads;

    public HttpClient() throws Exception {
        this(null);
    }

    public HttpClient(Map<String, String> heads) throws Exception {
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();
        this.heads = heads;
        getClient();
    }


    private void getClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
                .build();
        CookieStore cookieStore = new BasicCookieStore();
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if (heads != null) {
            List<Header> headerList = new ArrayList<>();
            for (Map.Entry<String, String> entry : heads.entrySet()) {
                headerList.add(new BasicHeader(entry.getKey(), entry.getValue()));
            }
            if (!headerList.isEmpty()) {
                httpClientBuilder.setDefaultHeaders(headerList);
            }
        }
        SSLContextBuilder sslContext = SSLContextBuilder.create().loadTrustMaterial(null, (chain, authType) -> true);
        DefaultClientTlsStrategy sslConnectionSocketFactory =
                new DefaultClientTlsStrategy(sslContext.build(),
                        NoopHostnameVerifier.INSTANCE);
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setSocketTimeout(Timeout.ofMilliseconds(3000))  // Socket Timeout
                .setConnectTimeout(Timeout.ofMilliseconds(3000)) // Connect Timeout
                .build();
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setTlsSocketStrategy(sslConnectionSocketFactory)
                .build();
        this.httpClient = httpClientBuilder
                .setConnectionManager(connectionManager).setDefaultCookieStore(cookieStore).setDefaultRequestConfig(defaultRequestConfig).build();
    }

    public <T> HttpClientResponse<T> post(String url, Object obj, Class<T> responseType) throws Exception {
        return post(url, new Gson().toJson(obj), responseType);
    }

    public <T> HttpClientResponse<T> post(String url, Object obj, Type type) throws Exception {
        return post(url, new Gson().toJson(obj), type);
    }

    public <T> HttpClientResponse<T> post(String url, String json, Type type) throws Exception {
        checkParams(url);
        HttpPost post;
        CloseableHttpResponse response = null;
        HttpClientResponse<T> httpMessage = new HttpClientResponse<>();
        httpMessage.setStatus(false);
        post = new HttpPost(url);
        if (StringUtils.isNotBlank(json)) {
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);//解決中文亂碼問題
            post.setEntity(entity);
        }
        try {
            response = httpClient.execute(post);
            String html = EntityUtils.toString(response.getEntity());
            error(response);
            httpMessage.setStatus(true);
            httpMessage.setContent(html);
            httpMessage.setCode(response.getCode());
            httpMessage.setBody(gson.fromJson(httpMessage.getContent(), type));
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return httpMessage;
    }


    public <T> HttpClientResponse<T> post(String url, String json, Class<T> responseType) throws Exception {
        checkParams(url);
        HttpPost post;
        CloseableHttpResponse response = null;
        HttpClientResponse<T> httpMessage = new HttpClientResponse<>();
        httpMessage.setStatus(false);
        post = new HttpPost(url);
        if (StringUtils.isNotBlank(json)) {
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);//解決中文亂碼問題
            post.setEntity(entity);
        }
        try {
            response = httpClient.execute(post);
            String html = EntityUtils.toString(response.getEntity());
            error(response);
            httpMessage.setStatus(true);
            httpMessage.setContent(html);
            httpMessage.setCode(response.getCode());
            httpMessage.setBody(gson.fromJson(httpMessage.getContent(), responseType));
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return httpMessage;
    }

    public <T> HttpClientResponse<T> patch(String url, Object obj, Class<T> responseType) throws Exception {
        return patch(url, gson.toJson(obj), responseType);
    }

    public <T> HttpClientResponse<T> patch(String url, String json, Class<T> responseType) throws Exception {
        checkParams(url);
        HttpPatch httpPatch;
        CloseableHttpResponse response = null;
        HttpClientResponse<T> httpMessage = new HttpClientResponse<>();
        httpMessage.setStatus(false);
        httpPatch = new HttpPatch(url);
        if (StringUtils.isNotBlank(json)) {
            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);//解決中文亂碼問題
            httpPatch.setEntity(entity);
        }
        try {
            response = httpClient.execute(httpPatch);
            String html = EntityUtils.toString(response.getEntity());
            error(response);
            httpMessage.setStatus(true);
            httpMessage.setContent(html);
            httpMessage.setCode(response.getCode());
            httpMessage.setBody(gson.fromJson(httpMessage.getContent(), responseType));
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return httpMessage;
    }

//    public <T> HttpClientResponse<T> get(String url, Object obj, Class<T> responseType) throws Exception {
//        return get(url, BeanUtils.describe(obj), responseType);
//    }


    public <T> HttpClientResponse<T> get(String url, Map<String, String> params, Type type) throws Exception {
        checkParams(url);
        HttpGet get;
        CloseableHttpResponse response = null;
        HttpClientResponse<T> httpMessage = new HttpClientResponse<>();
        httpMessage.setStatus(false);
        StringBuffer stringBuffer = coverMapToGetParams(params);
        if (stringBuffer.length() > 0) {
            url = url + "?" + stringBuffer;
        }
        get = new HttpGet(url);
        try {
            response = httpClient.execute(get);
            String html = EntityUtils.toString(response.getEntity());
            error(response);
            httpMessage.setStatus(true);
            httpMessage.setContent(html);
            httpMessage.setCode(response.getCode());
            httpMessage.setBody(gson.fromJson(httpMessage.getContent(), type));
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return httpMessage;
    }

    public <T> HttpClientResponse<T> get(String url, Map<String, String> params, Class<T> responseType) throws Exception {
        checkParams(url);
        HttpGet get;
        CloseableHttpResponse response = null;
        HttpClientResponse<T> httpMessage = new HttpClientResponse<>();
        httpMessage.setStatus(false);
        StringBuffer stringBuffer = coverMapToGetParams(params);
        if (stringBuffer.length() > 0) {
            url = url + "?" + stringBuffer;
        }
        get = new HttpGet(url);
        try {
            response = httpClient.execute(get);
            String html = EntityUtils.toString(response.getEntity());
            error(response);
            httpMessage.setStatus(true);
            httpMessage.setContent(html);
            httpMessage.setCode(response.getCode());
            httpMessage.setBody(gson.fromJson(httpMessage.getContent(), responseType));
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return httpMessage;
    }

    private StringBuffer coverMapToGetParams(Map<String, String> params) throws UnsupportedEncodingException {
        StringBuffer stringBuffer = new StringBuffer();
        // 編碼請求參數
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                stringBuffer.append(entry.getKey()).append("=").append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).append("&");
            }
            if (stringBuffer.length() > 0) {
                stringBuffer.delete(stringBuffer.length() - 1, stringBuffer.length());
            }
        }
        return stringBuffer;
    }

    private void checkParams(String url) throws Exception {
        if (StringUtils.isBlank(url)) {
            throw new Exception("請求URL 不能為空");
        }
    }

    private void error(HttpResponse response) throws Exception {
        if (response == null) {
            throw new Exception("服務返 回失敗");
        }
        if (response.getCode() == HttpStatus.SC_BAD_GATEWAY) {
            throw new Exception("服務502,無法連接.");
        }
    }
}
