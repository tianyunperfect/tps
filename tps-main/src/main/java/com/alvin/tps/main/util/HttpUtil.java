package com.alvin.tps.main.util;

import com.google.gson.Gson;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 我的utils封装
 * 读取设置：url、cookies、params
 * 读取：response
 * 设置 proxy、header、超时时间
 *
 * @author Mark
 */

@Data
@Accessors(chain = true)
public class HttpUtil {


    /** 代理  */
    private HttpHost proxy;

    /** 超时时间 5秒  */
    private Integer connectTimeout;

    /** header  */
    private HashMap<String, String> headerMap;

    /** urlStr  */
    private String urlStr;

    private BasicCookieStore cookieStore;

    private RequestConfig config;

    private HttpResponse httpResponse;
    /**
     * params参数
     */
    private List<NameValuePair> nameValuePairs;

    /** 使用链接池初始化链接  */
    //private PoolingHttpClientConnectionManager connectionManager;

    public HttpUtil() {
        // 5秒
        connectTimeout = 5000;
        cookieStore = new BasicCookieStore();
        nameValuePairs = new ArrayList<>();

        //connectionManager = new PoolingHttpClientConnectionManager();
    }

    /**
     * 给url设置参数
     *
     * @param paramsMap
     * @throws URISyntaxException
     */
    public HttpUtil setParams(HashMap<String, String> paramsMap){
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    /**
     * 获取get请求结果
     *
     * @return
     * @throws IOException
     */
    public String get() throws Exception {
        return getExecuteResult(getHttpGet());
    }

    /**
     * 获取post请求结果
     *
     * @param dataMap
     * @return
     * @throws IOException
     */
    public String post(HashMap<String, String> dataMap) throws Exception {
        HttpPost httpPost = getHttpPost();

        //解决中文乱码
        httpPost.setHeader("Content-Type", "text/html; charset=UTF-8");
        // 添加data
        if (dataMap != null) {
            List<NameValuePair> nameValuePairs = new ArrayList<>();
            for (Map.Entry<String, String> entry : dataMap.entrySet()) {
                nameValuePairs.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairs, "utf-8");
            httpPost.setEntity(urlEncodedFormEntity);
        }
        return getExecuteResult(httpPost);
    }

    /**
     * 发送json请求
     * @param object
     * @return
     * @throws Exception
     */
    public String jsonPost(Object object) throws Exception {
        HttpPost httpPost = getHttpPost();
        //解决中文乱码
        httpPost.setHeader("Content-Type", "application/json;charset=UTF-8");
        if (object != null) {
            StringEntity entity = new StringEntity(new Gson().toJson(object), "utf-8");
            httpPost.setEntity(entity);
        }
        return getExecuteResult(httpPost);
    }

    /**
     * 处理get和post请求，返回html
     *
     * @param httpUriRequest
     * @return
     * @throws IOException
     */
    private String getExecuteResult(HttpUriRequest httpUriRequest) throws IOException {
        String returnStr;
        //获取一个链接
        CloseableHttpClient httpClient = HttpClients.custom()
                //.setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore).build();

        //设置header
        httpUriRequest.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36");
        if (headerMap != null) {
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
                httpUriRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }

        httpResponse = httpClient.execute(httpUriRequest);
        //获取返回结果中的实体
        HttpEntity entity = httpResponse.getEntity();
        //判断是否失败
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            returnStr = null;
        } else {
            //查看页面内容结果
            String rawHTMLContent = EntityUtils.toString(entity, "utf-8");
            returnStr = decodeUnicode(rawHTMLContent);
        }
        //关闭HttpEntity流
        EntityUtils.consume(entity);
        return returnStr;
    }

    /**
     * 设置cookies
     *
     * @param cookieMap
     */
    public void setCookies(HashMap<String, String> cookieMap) {
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            BasicClientCookie clientCookie = new BasicClientCookie(entry.getKey(), entry.getValue());
            cookieStore.addCookie(clientCookie);
        }
    }

    /**
     * 获取cookies
     *
     * @return
     */
    public List<Cookie> getCookies() {
        return cookieStore.getCookies();
    }

    private HttpGet getHttpGet() throws Exception {
        URIBuilder uriBuilder = new URIBuilder(urlStr);
        if (nameValuePairs.size() > 0) {
            uriBuilder.addParameters(nameValuePairs);
        }
        URI uri = uriBuilder.build();
        HttpGet httpGet = new HttpGet(uri);

        httpGet.setConfig(getConfig());
        return httpGet;
    }

    private HttpPost getHttpPost() throws Exception {
        URIBuilder uriBuilder = new URIBuilder(urlStr);
        if (nameValuePairs.size() > 0) {
            uriBuilder.addParameters(nameValuePairs);
        }
        URI uri = uriBuilder.build();
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setConfig(getConfig());
        return httpPost;
    }

    public boolean getDownLoad(String filePath) throws Exception {
        return downLoadFile(getHttpGet(), filePath);
    }
    public boolean PostDownLoad(String filePath) throws Exception {
        return downLoadFile(getHttpPost(), filePath);
    }
    /**
     * 下载文件到指定路径
     *
     * @param httpUriRequest
     * @param filePath
     * @return
     */
    private boolean downLoadFile(HttpUriRequest httpUriRequest, String filePath) throws IOException {
        CloseableHttpClient httpClient = HttpClients.custom()
                //.setConnectionManager(connectionManager)
                .setDefaultCookieStore(cookieStore).build();

        HttpResponse httpResponse = httpClient.execute(httpUriRequest);
        //判断是否失败
        if (httpResponse.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return false;
        }
        //获取返回结果中的实体
        HttpEntity entity = httpResponse.getEntity();

        //存储
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        entity.writeTo(bos);

        bos.close();
        //关闭HttpEntity流
        EntityUtils.consume(entity);
        return true;
    }



    /**
     * 设置代理服务器
     *
     * @param ip
     * @param port
     * @param httpType
     */
    public HttpUtil setProxy(String ip, int port, String httpType) {
        this.proxy = new HttpHost(ip, port, httpType);
        return this;
    }

    private RequestConfig getConfig() {
        RequestConfig.Builder custom = RequestConfig.custom();
        if (proxy != null) {
            custom.setProxy(proxy);
        }
        /**
         * 设置超时时间
         */
        custom.setConnectTimeout(connectTimeout)
                .setSocketTimeout(connectTimeout)
                .setConnectionRequestTimeout(connectTimeout);

        return custom.build();
    }

    /**
     * 获取符合正则的第一个字符串，需要的字符串用（）括起来
     *
     * @param content
     * @return
     */
    public String getRegex(String content,String regex) {
        //String regex = ".*charset=([^;]*).*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    /**
     * 解析string里面的字符串，因为只有部分需要解析，所有需要判断
     *
     * @param theString
     * @return
     */
    public String decodeUnicode(String theString) {
        char aChar;
        int len = theString.length();
        StringBuffer outBuffer = new StringBuffer(len);
        for (int x = 0; x < len; ) {
            aChar = theString.charAt(x++);
            if (aChar == '\\') {
                aChar = theString.charAt(x++);
                if (aChar == 'u') {
                    // Read the xxxx
                    int value = 0;
                    for (int i = 0; i < 4; i++) {
                        aChar = theString.charAt(x++);
                        switch (aChar) {
                            case '0':
                            case '1':
                            case '2':
                            case '3':
                            case '4':
                            case '5':
                            case '6':
                            case '7':
                            case '8':
                            case '9':
                                value = (value << 4) + aChar - '0';
                                break;
                            case 'a':
                            case 'b':
                            case 'c':
                            case 'd':
                            case 'e':
                            case 'f':
                                value = (value << 4) + 10 + aChar - 'a';
                                break;
                            case 'A':
                            case 'B':
                            case 'C':
                            case 'D':
                            case 'E':
                            case 'F':
                                value = (value << 4) + 10 + aChar - 'A';
                                break;
                            default:
                                throw new IllegalArgumentException(
                                        "Malformed   \\uxxxx   encoding.");
                        }

                    }
                    outBuffer.append((char) value);
                } else {
                    switch (aChar) {
                        case 't':
                            aChar = '\t';
                            break;
                        case 'r':
                            aChar = '\r';
                            break;
                        case 'n':
                            aChar = '\n';
                            break;
                        case 'f':
                            aChar = '\f';
                            break;
                        default:
                            break;
                    }
                    outBuffer.append(aChar);
                }
            } else {
                outBuffer.append(aChar);
            }
        }
        return outBuffer.toString();
    }

    public static void main(String[] args) throws Exception {
        String s = new HttpUtil().setUrlStr("https://www.baidu.com").get();
        System.out.println(s);
    }
}