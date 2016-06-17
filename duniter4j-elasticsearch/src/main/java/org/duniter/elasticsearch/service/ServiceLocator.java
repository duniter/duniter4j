package org.duniter.elasticsearch.service;

/*
 * #%L
 * UCoin Java Client :: Core API
 * %%
 * Copyright (C) 2014 - 2015 EIS
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


import org.duniter.core.beans.BeanFactory;
import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.dao.mem.MemoryCurrencyDaoImpl;
import org.duniter.core.client.dao.mem.MemoryPeerDaoImpl;
import org.duniter.core.client.service.DataContext;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.HttpServiceImpl;
import org.duniter.core.client.service.bma.*;
import org.duniter.core.client.service.local.CurrencyService;
import org.duniter.core.client.service.local.CurrencyServiceImpl;
import org.duniter.core.client.service.local.PeerService;
import org.duniter.core.client.service.local.PeerServiceImpl;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.service.Ed25519CryptoServiceImpl;
import org.duniter.elasticsearch.PluginSettings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

import java.io.IOException;

@Singleton
public class ServiceLocator
        extends org.duniter.core.client.service.ServiceLocator
        {
    private static final ESLogger log = ESLoggerFactory.getLogger(ServiceLocator.class.getName());

    public ServiceLocator() {
        super(createBeanFactory());
        log.info("Starting ServiceLocator (guice)");
        org.duniter.core.client.service.ServiceLocator.setInstance(this);
        log.info("Starting ServiceLocator [OK]");
    }

    @Override
    public void close() {
        try {
            super.close();
        }
        catch (IOException e) {
            throw new TechnicalException(e);
        }
        org.duniter.core.client.service.ServiceLocator.setInstance(null);
    }

    /* -- Internal methods -- */

    protected static BeanFactory createBeanFactory() {
        BeanFactory beanFactory = new BeanFactory()
                .bind(BlockchainRemoteService.class, BlockchainRemoteServiceImpl.class)
                .bind(NetworkRemoteService.class, NetworkRemoteServiceImpl.class)
                .bind(WotRemoteService.class, WotRemoteServiceImpl.class)
                .bind(TransactionRemoteService.class, TransactionRemoteServiceImpl.class)
                .bind(CryptoService.class, Ed25519CryptoServiceImpl.class)
                .bind(PeerService.class, PeerServiceImpl.class)
                .bind(CurrencyService.class, CurrencyServiceImpl.class)
                .bind(HttpService.class, HttpServiceImpl.class)
                .bind(CurrencyDao.class, MemoryCurrencyDaoImpl.class)
                .bind(PeerDao.class, MemoryPeerDaoImpl.class)
                .add(DataContext.class);
        return beanFactory;
    }
}
