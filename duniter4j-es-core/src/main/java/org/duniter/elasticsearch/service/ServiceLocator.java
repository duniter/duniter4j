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


import org.duniter.core.beans.Bean;
import org.duniter.core.beans.BeanFactory;
import org.duniter.core.client.dao.CurrencyDao;
import org.duniter.core.client.dao.PeerDao;
import org.duniter.core.client.dao.mem.MemoryCurrencyDaoImpl;
import org.duniter.core.client.dao.mem.MemoryPeerDaoImpl;
import org.duniter.core.client.service.DataContext;
import org.duniter.core.client.service.HttpService;
import org.duniter.core.client.service.HttpServiceImpl;
import org.duniter.core.client.service.bma.*;
import org.duniter.core.client.service.local.*;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.client.service.local.CurrencyService;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.service.CryptoService;
import org.duniter.core.service.Ed25519CryptoServiceImpl;
import org.duniter.core.service.MailService;
import org.duniter.core.service.MailServiceImpl;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;

import java.io.IOException;

@Singleton
public class ServiceLocator
        extends org.duniter.core.client.service.ServiceLocator
        {
    private static final ESLogger logger = ESLoggerFactory.getLogger("duniter.service");

    private static BeanFactory beanFactory = null;

    @Inject
    public ServiceLocator() {
        super();
        if (logger.isDebugEnabled()) {
            logger.debug("Starting Duniter4j ServiceLocator...");
        }
        setBeanFactory(getOrCreateBeanFactory());

        org.duniter.core.client.service.ServiceLocator.setInstance(this);
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

    protected static BeanFactory getOrCreateBeanFactory() {
        if (beanFactory != null) {
            return beanFactory;
        }
        beanFactory = org.duniter.core.client.service.ServiceLocator.instance().getBeanFactory()
                .bind(BlockchainRemoteService.class, BlockchainRemoteServiceImpl.class)
                .bind(NetworkRemoteService.class, NetworkRemoteServiceImpl.class)
                .bind(WotRemoteService.class, WotRemoteServiceImpl.class)
                .bind(TransactionRemoteService.class, TransactionRemoteServiceImpl.class)
                .bind(CryptoService.class, Ed25519CryptoServiceImpl.class)
                .bind(MailService.class, MailServiceImpl.class)
                .bind(PeerService.class, PeerServiceImpl.class)
                .bind(CurrencyService.class, CurrencyServiceImpl.class)
                .bind(NetworkService.class, NetworkServiceImpl.class)
                .bind(HttpService.class, HttpServiceImpl.class)
                .bind(CurrencyDao.class, MemoryCurrencyDaoImpl.class)
                .bind(PeerDao.class, MemoryPeerDaoImpl.class)
                .add(DataContext.class);
        return beanFactory;
    }

    public static class Provider<T extends Bean> implements org.elasticsearch.common.inject.Provider<T> {

        private final Class<T> clazz;
        private final BeanFactory beanFactory;

        public Provider(Class<T> clazz) {
            this.clazz = clazz;
            this.beanFactory = getOrCreateBeanFactory();
        }

        public T get() {
            return beanFactory.getBean(clazz);
        }
    }
}
