package org.duniter.core.client.util.http;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.duniter.core.client.config.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;

public abstract class HttpClients {

    private static final Logger log = LoggerFactory.getLogger(HttpClients.class);

    private static ThreadLocal<HttpClientConnectionManager> connectionManagerMapper = new ThreadLocal<HttpClientConnectionManager>() {
        @Override
        public HttpClientConnectionManager initialValue() {

            if (log.isDebugEnabled()) log.debug("[HttpClients] Creating new HttpClientConnectionManager, for thread [%s]", Thread.currentThread().getId());

            Configuration config = Configuration.instance();

            return createConnectionManager(
                    config.getNetworkMaxTotalConnections(),
                    config.getNetworkMaxConnectionsPerRoute(),
                    config.getNetworkTimeout());
        }
    };

    private static ThreadLocal<HttpClient> httpClientsMapper =  new ThreadLocal<HttpClient>() {
        @Override
        public HttpClient initialValue() {
            HttpClientConnectionManager connectionManager= connectionManagerMapper.get();
            if (log.isDebugEnabled()) log.debug("[HttpClients] Creating new HttpClient, for thread [%s]", Thread.currentThread().getId());
            return createHttpClient(connectionManager, 0);
        }

        @Override
        public void remove() {
            super.remove();
        }
    };

    public static HttpClient getThreadHttpClient(final Integer timeout) {
        if (timeout <= 0) return getThreadHttpClient();

        final HttpClientConnectionManager connectionManager = connectionManagerMapper.get();
        return createHttpClient(connectionManager, timeout);
    }

    public static HttpClient getThreadHttpClient() {
        return httpClientsMapper.get();
    }

    /**
     * Remove client from the thread
     */
    public static void remove() {
        connectionManagerMapper.remove();
        httpClientsMapper.remove();
    }

    public static HttpClient createHttpClient(int timeout) {
        return createHttpClient(null,timeout);
    }

    public static HttpClient createHttpClient(HttpClientConnectionManager connectionManager, int timeout) {
        if (timeout <= 0)  {
            Configuration config = Configuration.instance();
            timeout = config.getNetworkTimeout();
        }

        return org.apache.http.impl.client.HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(createRequestConfig(timeout))
                .setRetryHandler(createRetryHandler(timeout))
                .build();
    }


    public static PoolingHttpClientConnectionManager createConnectionManager(
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

    public static RequestConfig createRequestConfig(int timeout) {
        return RequestConfig.custom()
                .setSocketTimeout(timeout).setConnectTimeout(timeout)
                .setMaxRedirects(1)
                .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                .build();
    }


    protected static HttpRequestRetryHandler createRetryHandler(int timeout) {
        final int maxRetryCount = (timeout < 1000) ? 2 : 3;
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

                if (!retrying) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed request to " + HttpClientContext.adapt(context).getRequest().getRequestLine() + ": " + exception.getMessage());
                    }
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // Retry if the request is considered idempotent
                    if (log.isDebugEnabled()) log.debug("Failed (but will retry) request to " + request.getRequestLine() + ": " + exception.getMessage());
                    return true;
                }
                return false;
            }
        };
    }

//
//    private PoolingHttpClientConnectionManager connectionManager;
//
//    private static final ThreadLocal<SimpleCache<Integer, HttpClient>> threadLocalCache = new ThreadLocal<SimpleCache<Integer, HttpClient>>() {
//        @Override
//        protected SimpleCache<Integer, HttpClient> initialValue() {
//            int cacheTimeInMillis = 1000 * 60 * 5; // 5 min cache
//            SimpleCache<Integer, HttpClient> cache = new SimpleCache<Integer, HttpClient>(cacheTimeInMillis*100) {
//                @Override
//                public HttpClient load(Integer timeout) {
//                    return createHttpClient(timeout);
//                }
//            };
//            cache.registerRemoveListener(item -> {
//                log.debug("Closing HttpClient...");
//                closeQuietly(item);
//            });
//        }
//    };
//
//
//    protected static HttpClient createHttpClient(int timeout) {
//        if (connectionManager == null) {
//            Configuration config = Configuration.instance();
//            connectionManager = createConnectionManager(
//                    config.getNetworkMaxTotalConnections(),
//                    config.getNetworkMaxConnectionsPerRoute(),
//                    config.getNetworkTimeout());
//        }
//
//        return org.apache.http.impl.client.HttpClients.custom()
//                .setConnectionManager(connectionManager)
//                .setDefaultRequestConfig(requestConfigCache.get(timeout))
//                .setRetryHandler(createRetryHandler(timeout))
//                .build();
//    }
}
