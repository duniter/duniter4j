package org.duniter.core.client.service;

/*
 * #%L
 * UCoin Java :: Core Client API
 * %%
 * Copyright (C) 2014 - 2016 EIS
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.config.ConfigurationOption;
import org.duniter.core.client.model.bma.Constants;
import org.duniter.core.client.model.bma.Error;
import org.duniter.core.client.model.bma.jackson.JacksonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.bma.BmaTechnicalException;
import org.duniter.core.client.service.exception.*;
import org.duniter.core.exception.BusinessException;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.util.ObjectUtils;
import org.duniter.core.util.StringUtils;
import org.duniter.core.util.cache.SimpleCache;
import org.duniter.core.util.websocket.WebsocketClientEndpoint;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceConfigurationError;

/**
 * Created by eis on 05/02/15.
 */
public class HttpServiceImpl implements HttpService, Closeable, InitializingBean{

    private static final Logger log = LoggerFactory.getLogger(HttpServiceImpl.class);

    public static final String URL_PEER_ALIVE = "/blockchain/parameters";

    private PoolingHttpClientConnectionManager connectionManager;

    protected ObjectMapper objectMapper;
    protected Peer defaultPeer;
    private boolean debug;
    protected Joiner pathJoiner = Joiner.on('/');
    protected SimpleCache<Integer, RequestConfig> requestConfigCache;
    protected SimpleCache<Integer, HttpClient> httpClientCache;
    protected int defaultTimeout;

    protected Map<URI, WebsocketClientEndpoint> wsEndPoints = new HashMap<>();

    public HttpServiceImpl() {
        super();
        this.debug = log.isDebugEnabled();
    }

    @Override
    public void afterPropertiesSet() throws Exception {

        // Initialize caches
        initCaches();

        this.objectMapper = JacksonUtils.newObjectMapper();
    }

    /**
     * Initialize caches
     */
    protected void initCaches() {
        Configuration config = Configuration.instance();
        int cacheTimeInMillis = config.getNetworkCacheTimeInMillis();
        defaultTimeout = config.getNetworkTimeout() > 0 ?
                config.getNetworkTimeout() :
                Integer.parseInt(ConfigurationOption.NETWORK_TIMEOUT.getDefaultValue());

        requestConfigCache = new SimpleCache<Integer, RequestConfig>(cacheTimeInMillis*100) {
            @Override
            public RequestConfig load(Integer timeout) {
                // Use config default timeout, if 0
                if (timeout <= 0) timeout = defaultTimeout;
                return createRequestConfig(timeout);
            }
        };

        httpClientCache = new SimpleCache<Integer, HttpClient>(cacheTimeInMillis*100) {
            @Override
            public HttpClient load(Integer timeout) {
                return createHttpClient(timeout);
            }
        };
        httpClientCache.registerRemoveListener(item -> {
            log.debug("Closing HttpClient...");
            closeQuietly(item);
        });
    }

    public void connect(Peer peer) throws PeerConnectionException {
        if (peer == null) {
            throw new IllegalArgumentException("argument 'peer' must not be null");
        }
        if (peer == defaultPeer) {
            return;
        }

        HttpGet httpGet = new HttpGet(getPath(peer, URL_PEER_ALIVE));
        boolean isPeerAlive;
        try {
            isPeerAlive = executeRequest(httpClientCache.get(0/*=default timeout*/), httpGet);
        } catch(TechnicalException e) {
           this.defaultPeer = null;
           throw new PeerConnectionException(e);
        }
        if (!isPeerAlive) {
            this.defaultPeer = null;
            throw new PeerConnectionException("Unable to connect to peer: " + peer.toString());
        }
        this.defaultPeer = peer;
    }

    public boolean isConnected() {
        return this.defaultPeer != null;
    }

    @Override
    public void close() throws IOException {
        httpClientCache.clear();
        requestConfigCache.clear();

        if (wsEndPoints.size() != 0) {
            for (WebsocketClientEndpoint clientEndPoint: wsEndPoints.values()) {
                clientEndPoint.close();
            }
            wsEndPoints.clear();
        }

        connectionManager.close();
    }

    public <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass)  {
        return executeRequest(httpClientCache.get(0), request, resultClass);
    }

    public <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass, Class<?> errorClass)  {
        //return executeRequest(httpClientCache.get(0), request, resultClass, errorClass);
        return executeRequest( createHttpClient(0), request, resultClass, errorClass);

    }

    public <T> T executeRequest(String absolutePath, Class<? extends T> resultClass)  {
        HttpGet httpGet = new HttpGet(getPath(absolutePath));
        return executeRequest(httpClientCache.get(0), httpGet, resultClass);
    }

    public <T> T executeRequest(Peer peer, String absolutePath, Class<? extends T> resultClass)  {
        HttpGet httpGet = new HttpGet(peer.getUrl() + absolutePath);
        return executeRequest(httpClientCache.get(0), httpGet, resultClass);
    }

    public String getPath(Peer peer, String... absolutePath) {
        String path = "/" + pathJoiner.skipNulls().join(absolutePath);
        return peer.getUrl() + path.replaceAll("//+", "/");
    }


    public String getPath(String... absolutePath) {
        checkDefaultPeer();
        String pathToAppend = pathJoiner.skipNulls().join(absolutePath);
        String result = pathJoiner.join(defaultPeer.getUrl(), pathToAppend);
        return result;
    }

    public URIBuilder getURIBuilder(URI baseUri, String... path)  {
        String pathToAppend = pathJoiner.skipNulls().join(path);

        int customQueryStartIndex = pathToAppend.indexOf('?');
        String customQuery = null;
        if (customQueryStartIndex != -1) {
            customQuery = pathToAppend.substring(customQueryStartIndex+1);
            pathToAppend = pathToAppend.substring(0, customQueryStartIndex);
        }

        URIBuilder builder = new URIBuilder(baseUri);

        builder.setPath(baseUri.getPath() + pathToAppend);
        if (StringUtils.isNotBlank(customQuery)) {
            builder.setCustomQuery(customQuery);
        }
        return builder;
    }

    /* -- Internal methods -- */

    protected void checkDefaultPeer() {
        if (defaultPeer == null) {
            throw new IllegalStateException("No peer to connect");
        }
    }

    protected PoolingHttpClientConnectionManager createConnectionManager(
            int maxTotalConnections,
            int maxConnectionsPerRoute,
            int timeout) {
        PoolingHttpClientConnectionManager connectionManager
                = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(maxTotalConnections);
        connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);
        connectionManager.setDefaultSocketConfig(SocketConfig.custom()
                .setSoTimeout(timeout).build());
        return connectionManager;
    }

    protected HttpClient createHttpClient(int timeout) {
        if (connectionManager == null) {
            Configuration config = Configuration.instance();
            connectionManager = createConnectionManager(
                    config.getNetworkMaxTotalConnections(),
                    config.getNetworkMaxConnectionsPerRoute(),
                    config.getNetworkTimeout());
        }

        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfigCache.get(timeout))
                .setRetryHandler(createRetryHandler(timeout))
                .build();
    }

    protected HttpRequestRetryHandler createRetryHandler(int timeout) {
        if (timeout <= 0) timeout = defaultTimeout;
        final int maxRetryCount = (timeout < defaultTimeout) ? 2 : 3;
        return new HttpRequestRetryHandler() {
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {

                boolean retrying = true;
                if (exception instanceof NoRouteToHostException) {
                    // Bad DNS name
                    retrying =false;
                }
                else if (exception instanceof InterruptedIOException) {
                    // Timeout
                    retrying = false;
                }
                else if (exception instanceof UnknownHostException) {
                    // Unknown host
                    retrying = false;
                }
                else if (exception instanceof SSLException) {
                    // SSL handshake exception
                    retrying = false;
                }
                else if (exception instanceof HttpHostConnectException) {
                    // Host connect error
                    retrying = false;
                }

                if (retrying && executionCount >= maxRetryCount) {
                    // Do not retry if over max retry count
                    return false;
                }


                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                if (!retrying) {
                    log.debug("Failed request to " + request.getRequestLine() + ": " + exception.getMessage());
                    return false;
                }

                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // Retry if the request is considered idempotent
                    log.debug("Failed (but will retry) request to " + request.getRequestLine() + ": " + exception.getMessage());
                    return true;
                }
                return false;
            }
        };
    }

    protected RequestConfig createRequestConfig(int timeout) {
        return RequestConfig.custom()
                .setSocketTimeout(timeout).setConnectTimeout(timeout)
                .setMaxRedirects(1)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();
    }

    protected <T> T executeRequest(HttpClient httpClient, HttpUriRequest request, Class<? extends T> resultClass)  {
        return executeRequest(httpClient, request, resultClass, Error.class);
    }

    @SuppressWarnings("unchecked")
    protected <T> T executeRequest(HttpClient httpClient, HttpUriRequest request, Class<? extends T> resultClass, Class<?> errorClass)  {
        return executeRequest(httpClient, request, resultClass, errorClass, 1);
    }

    protected <T> T executeRequest(HttpClient httpClient, HttpUriRequest request, Class<? extends T> resultClass, Class<?> errorClass, int retryCount)  {
        T result = null;

        if (debug) {
            log.debug("Executing request : " + request.getRequestLine());
        }

        boolean retry = false;
        HttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (debug) {
                log.debug("Received response : " + response.getStatusLine());
            }

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK: {
                    if (resultClass == null || resultClass.equals(HttpResponse.class)) {
                        result = (T)response;
                    }
                    else {
                        result = (T) parseResponse(request, response, resultClass);
                        EntityUtils.consume(response.getEntity());
                    }
                    break;
                }
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw new HttpUnauthorizeException(I18n.t("duniter4j.client.authentication"));
                case HttpStatus.SC_NOT_FOUND:
                    throw new HttpNotFoundException(I18n.t("duniter4j.client.notFound", request.toString()));
                case HttpStatus.SC_BAD_REQUEST:
                    try {
                        Object errorResponse = parseResponse(request, response, errorClass);
                        if (errorResponse instanceof Error) {
                            throw new HttpBadRequestException((Error)errorResponse);
                        }
                        else {
                            throw new HttpBadRequestException(errorResponse.toString());
                        }
                    }
                    catch(IOException e) {
                        throw new HttpBadRequestException(I18n.t("duniter4j.client.status", response.getStatusLine().toString()));
                    }

                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                case Constants.HttpStatus.SC_TOO_MANY_REQUESTS:
                    retry = true;
                    break;
                default:
                    String defaultMessage = I18n.t("duniter4j.client.status", request.toString(), response.getStatusLine().toString());
                    if (isContentType(response, ContentType.APPLICATION_JSON)) {
                        JsonNode node = objectMapper.readTree(response.getEntity().getContent());
                        if (node.hasNonNull("ucode")) {
                            throw new BmaTechnicalException(node.get("ucode").asInt(), node.get("message").asText(defaultMessage));
                        }
                    }
                    throw new TechnicalException(defaultMessage);
            }
        }
        catch (ConnectException e) {
            throw new HttpConnectException(I18n.t("duniter4j.client.core.connect", request.toString()), e);
        }
        catch (SocketTimeoutException | ConnectTimeoutException e) {
            throw new HttpTimeoutException(I18n.t("duniter4j.client.core.timeout"), e);
        }
        catch (TechnicalException | BusinessException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new TechnicalException(e.getMessage(), e);
        }
        finally {
            // Close is need
            if (response instanceof CloseableHttpResponse) {
                try {
                    ((CloseableHttpResponse) response).close();
                }
                catch(IOException e) {
                    // Silent is gold
                }
            }
        }

        // HTTP requests limit exceed, retry when possible
        if (retry) {
            if (retryCount > 0) {
                log.debug(String.format("Service unavailable: waiting [%s ms] before retrying...", Constants.Config.TOO_MANY_REQUEST_RETRY_TIME));
                try {
                    Thread.sleep(Constants.Config.TOO_MANY_REQUEST_RETRY_TIME);
                } catch (InterruptedException e) {
                    throw new TechnicalException(I18n.t("duniter4j.client.status", request.toString(), response.getStatusLine().toString()));
                }
                // iterate
                return executeRequest(httpClient, request, resultClass, errorClass, retryCount - 1);
            }
            else {
                throw new TechnicalException(I18n.t("duniter4j.client.status", request.toString(), response.getStatusLine().toString()));
            }
        }

        return result;
    }

    protected Object parseResponse(HttpUriRequest request, HttpResponse response, Class<?> ResultClass) throws IOException {
        Object result = null;

        boolean isStreamContent = ResultClass == null || ResultClass.equals(InputStream.class);
        boolean isStringContent = !isStreamContent && ResultClass != null && ResultClass.equals(String.class);

        InputStream content = response.getEntity().getContent();

        // If should return an inputstream
        if (isStreamContent) {
            result = content; // must be close by caller
        }

        // If should return String
        else if (isStringContent) {
            try {
                String stringContent = getContentAsString(content);
                // Add a debug before returning the result
                if (log.isDebugEnabled()) {
                    log.debug("Parsing response:\n" + stringContent);
                }
                return stringContent;
            }
            finally {
                if (content!= null) {
                    content.close();
                }
            }
        }

        // deserialize Json
        else {

            try {
                result = readValue(content, ResultClass);
            }
            catch (Exception e) {
                String requestPath = request.getURI().toString();

                // Check if content-type error
                ContentType contentType = ContentType.getOrDefault(response.getEntity());
                String actualMimeType = contentType.getMimeType();
                if (!ObjectUtils.equals(ContentType.APPLICATION_JSON.getMimeType(), actualMimeType)) {
                    throw new TechnicalException(I18n.t("duniter4j.client.core.invalidResponseContentType", requestPath, ContentType.APPLICATION_JSON.getMimeType(), actualMimeType));
                }

                // throw a generic error
                throw new TechnicalException(I18n.t("duniter4j.client.core.invalidResponse", requestPath), e);
            }
            finally {
                if (content!= null) {
                    content.close();
                }
            }
        }

        if (result == null) {
            throw new TechnicalException(I18n.t("duniter4j.client.core.emptyResponse", request.getURI().toString()));
        }

        return result;
    }

    private boolean isContentType(HttpResponse response, ContentType expectedContentType) {
        if (response.getEntity() == null) return false;
        ContentType contentType = ContentType.getOrDefault(response.getEntity());
        String actualMimeType = contentType.getMimeType();
        return ObjectUtils.equals(expectedContentType.getMimeType(), actualMimeType);
    }

    protected String getContentAsString(InputStream content) throws IOException {
        Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8);
        StringBuilder result = new StringBuilder();
        char[] buf = new char[64];
        int len = 0;
        while((len = reader.read(buf)) != -1) {
            result.append(buf, 0, len);
        }
        return result.toString();
    }

    protected boolean executeRequest(HttpClient httpClient, HttpUriRequest request)  {

        if (log.isDebugEnabled()) {
            log.debug("Executing request : " + request.getRequestLine());
        }

        try {
            HttpResponse response = httpClient.execute(request);

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK: {
                    response.getEntity().consumeContent();
                    return true;
                }
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw new TechnicalException(I18n.t("duniter4j.client.authentication"));
                default:
                    throw new TechnicalException(I18n.t("duniter4j.client.status", response.getStatusLine().toString()));
            }

        }
        catch (ConnectException e) {
            throw new TechnicalException(I18n.t("duniter4j.client.core.connect"), e);
        }
        catch (IOException e) {
            throw new TechnicalException(e.getMessage(), e);
        }
    }

    public static void closeQuietly(HttpClient httpClient) {
        try {
            if (httpClient instanceof CloseableHttpClient) {
                ((CloseableHttpClient) httpClient).close();
            } else if (httpClient instanceof Closeable) {
                ((Closeable) httpClient).close();
            }
        } catch(IOException e) {
            // silent is gold
        }
    }

    public WebsocketClientEndpoint getWebsocketClientEndpoint(Peer peer, String path, boolean autoReconnect) {

        try {
            URI wsBlockURI = new URI(String.format("%s://%s:%s%s",
                    peer.isUseSsl() ? "wss" : "ws",
                    peer.getHost(),
                    peer.getPort(),
                    path));

            // Get the websocket, or open new one if not exists
            WebsocketClientEndpoint wsClientEndPoint = wsEndPoints.get(wsBlockURI);
            if (wsClientEndPoint == null || wsClientEndPoint.isClosed()) {
                log.info(String.format("Starting to listen on [%s]...", wsBlockURI.toString()));
                wsClientEndPoint = new WebsocketClientEndpoint(wsBlockURI, autoReconnect);
                wsEndPoints.put(wsBlockURI, wsClientEndPoint);
            }

            return wsClientEndPoint;

        } catch (URISyntaxException | ServiceConfigurationError ex) {
            throw new TechnicalException(String.format("Could not create URI need for web socket [%s]: %s", path, ex.getMessage()));
        }

    }

    public <T> T readValue(String json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    public <T> T readValue(byte[] json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(json, clazz);
    }

    public <T> T readValue(InputStream json, Class<T> clazz) throws IOException {
        return objectMapper.readValue(new InputStreamReader(json, Charsets.UTF_8.name()), clazz);
    }

}
