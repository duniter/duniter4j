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

import com.google.gson.Gson;
import org.duniter.core.beans.InitializingBean;
import org.duniter.core.client.config.Configuration;
import org.duniter.core.client.model.bma.Error;
import org.duniter.core.client.model.bma.gson.GsonUtils;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.exception.HttpBadRequestException;
import org.duniter.core.client.service.exception.JsonSyntaxException;
import org.duniter.core.client.service.exception.PeerConnectionException;
import org.duniter.core.exception.TechnicalException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.nuiton.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Created by eis on 05/02/15.
 */
public class HttpServiceImpl implements HttpService, Closeable, InitializingBean{

    private static final Logger log = LoggerFactory.getLogger(HttpServiceImpl.class);

    public static final String URL_PEER_ALIVE = "/blockchain/parameters";

    protected Integer baseTimeOut;
    protected Gson gson;
    protected HttpClient httpClient;
    protected Peer defaultPeer;
    private boolean debug;

    public HttpServiceImpl() {
        super();
        this.debug = log.isDebugEnabled();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Configuration config = Configuration.instance();
        this.gson = GsonUtils.newBuilder().create();
        this.baseTimeOut = config.getNetworkTimeout();
        this.httpClient = createHttpClient();
    }

    public void connect(Peer peer) throws PeerConnectionException {
        if (peer == null) {
            throw new IllegalArgumentException("argument 'peer' must not be null");
        }
        if (httpClient == null) {
            httpClient = createHttpClient();
        }
        if (peer == defaultPeer) {
            return;
        }

        HttpGet httpGet = new HttpGet(getPath(peer, URL_PEER_ALIVE));
        boolean isPeerAlive = false;
        try {
            isPeerAlive = executeRequest(httpClient, httpGet);
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
        if (httpClient instanceof CloseableHttpClient) {
            ((CloseableHttpClient)httpClient).close();
        }
        else if (httpClient instanceof Closeable) {
            ((Closeable)httpClient).close();
        }
        httpClient = null;
    }

    public <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass)  {
        return executeRequest(httpClient, request, resultClass);
    }

    public <T> T executeRequest(String absolutePath, Class<? extends T> resultClass)  {
        HttpGet httpGet = new HttpGet(getPath(absolutePath));
        return executeRequest(httpClient, httpGet, resultClass);
    }

    public <T> T executeRequest(Peer peer, String absolutePath, Class<? extends T> resultClass)  {
        HttpGet httpGet = new HttpGet(getPath(peer, absolutePath));
        return executeRequest(httpClient, httpGet, resultClass);
    }

    public String getPath(Peer peer, String absolutePath) {
        return new StringBuilder().append(peer.getUrl()).append(absolutePath).toString();
    }


    public String getPath(String absolutePath) {
        checkDefaultPeer();
        return new StringBuilder().append(defaultPeer.getUrl()).append(absolutePath).toString();
    }


    /* -- Internal methods -- */

    protected void checkDefaultPeer() {
        if (defaultPeer == null) {
            throw new IllegalStateException("No peer to connect");
        }
    }

    protected HttpClient createHttpClient() {
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(getRequestConfig())
                // .setDefaultCredentialsProvider(getCredentialsProvider())
                .build();
        return httpClient;
    }

    protected RequestConfig getRequestConfig() {
        // build request config for timeout
        return RequestConfig.custom().setSocketTimeout(baseTimeOut).setConnectTimeout(baseTimeOut).build();
    }

    @SuppressWarnings("unchecked")
    protected <T> T executeRequest(HttpClient httpClient, HttpUriRequest request, Class<? extends T> resultClass)  {
        T result = null;

        if (log.isDebugEnabled()) {
            log.debug("Executing request : " + request.getRequestLine());
        }

        HttpResponse response = null;
        try {
            response = httpClient.execute(request);

            if (log.isDebugEnabled()) {
                log.debug("Received response : " + response.getStatusLine());
            }

            switch (response.getStatusLine().getStatusCode()) {
                case HttpStatus.SC_OK: {
                    result = (T) parseResponse(response, resultClass);

                    response.getEntity().consumeContent();
                    break;
                }
                case HttpStatus.SC_UNAUTHORIZED:
                case HttpStatus.SC_FORBIDDEN:
                    throw new TechnicalException(I18n.t("duniter4j.client.authentication"));
                case HttpStatus.SC_BAD_REQUEST:
                    try {
                        Error error = (Error)parseResponse(response, Error.class);
                        throw new HttpBadRequestException(error);
                    }
                    catch(IOException e) {
                        throw new HttpBadRequestException(I18n.t("duniter4j.client.status", response.getStatusLine().toString()));
                    }
                default:
                    throw new TechnicalException(I18n.t("duniter4j.client.status", response.getStatusLine().toString()));
            }
        }
        catch (ConnectException e) {
            throw new TechnicalException(I18n.t("duniter4j.client.core.connect", request.toString()), e);
        }
        catch (SocketTimeoutException e) {
            throw new TechnicalException(I18n.t("duniter4j.client.core.timeout"), e);
        }
        catch (IOException e) {
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

        return result;
    }

    protected Object parseResponse(HttpResponse response, Class<?> ResultClass) throws IOException {
        Object result = null;

        boolean stringOutput = ResultClass != null && ResultClass.equals(String.class);

        // If trace enable, log the response before parsing
        Exception error = null;
        if (stringOutput) {
            InputStream content = null;
            try {
                content = response.getEntity().getContent();
                String stringContent = getContentAsString(content);
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

        // trace not enable
        else {
            InputStream content = null;
            try {
                content = response.getEntity().getContent();
                Reader reader = new InputStreamReader(content, StandardCharsets.UTF_8);
                if (ResultClass != null) {
                    result = gson.fromJson(reader, ResultClass);
                }
                else {
                    result = null;
                }
            }
            catch (com.google.gson.JsonSyntaxException e) {
                if (content != null) {
                    log.warn("Error while parsing JSON response: " + getContentAsString(content), e);
                }
                else {
                    log.warn("Error while parsing JSON response", e);
                }
                throw new JsonSyntaxException(I18n.t("duniter4j.client.core.invalidResponse"), e);
            }
            catch (Exception e) {
                throw new TechnicalException(I18n.t("duniter4j.client.core.invalidResponse"), e);
            }
            finally {
                if (content!= null) {
                    content.close();
                }
            }
        }

        if (result == null) {
            throw new TechnicalException(I18n.t("duniter4j.client.core.emptyResponse"));
        }

        return result;
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
}
