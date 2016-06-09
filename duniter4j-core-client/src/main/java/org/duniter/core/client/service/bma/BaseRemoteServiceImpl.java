package org.duniter.core.client.service.bma;

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

import org.duniter.core.beans.InitializingBean;
import org.duniter.core.beans.Service;
import org.duniter.core.client.model.local.Peer;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.local.PeerService;
import org.duniter.core.client.service.ServiceLocator;
import org.apache.http.client.methods.HttpUriRequest;

import java.io.IOException;

/**
 * Created by eis on 05/02/15.
 */
public abstract class BaseRemoteServiceImpl implements Service, InitializingBean {

    protected HttpService httpService;
    protected PeerService peerService;

    @Override
    public void afterPropertiesSet() {
        httpService = ServiceLocator.instance().getHttpService();
        peerService = ServiceLocator.instance().getPeerService();
    }

    @Override
    public void close() throws IOException {
        httpService = null;
        peerService = null;
    }

    public <T> T executeRequest(Peer peer, String absolutePath, Class<? extends T> resultClass)  {
        return httpService.executeRequest(peer, absolutePath, resultClass);
    }

    public <T> T executeRequest(long currencyId, String absolutePath, Class<? extends T> resultClass)  {
        Peer peer = peerService.getActivePeerByCurrencyId(currencyId);
        return httpService.executeRequest(peer, absolutePath, resultClass);
    }

    public <T> T executeRequest(HttpUriRequest request, Class<? extends T> resultClass)  {
        return httpService.executeRequest(request, resultClass);
    }

    public String getPath(long currencyId, String aPath) {
        Peer peer = peerService.getActivePeerByCurrencyId(currencyId);
        return httpService.getPath(peer, aPath);
    }

    public String getPath(Peer peer, String aPath) {
        return httpService.getPath(peer, aPath);
    }
}
