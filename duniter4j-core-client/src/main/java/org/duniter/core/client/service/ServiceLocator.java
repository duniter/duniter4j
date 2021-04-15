package org.duniter.core.client.service;

/*
 * #%L
 * Duniter4j :: Core API
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
import org.duniter.core.client.repositories.PeerRepository;
import org.duniter.core.client.service.bma.BlockchainRemoteService;
import org.duniter.core.client.service.bma.NetworkRemoteService;
import org.duniter.core.client.service.bma.TransactionRemoteService;
import org.duniter.core.client.service.bma.WotRemoteService;
import org.duniter.core.client.service.elasticsearch.CurrencyPodRemoteService;
import org.duniter.core.client.service.local.CurrencyService;
import org.duniter.core.client.service.local.DividendService;
import org.duniter.core.client.service.local.NetworkService;
import org.duniter.core.client.service.local.PeerService;
import org.duniter.core.service.CryptoService;
import org.duniter.core.service.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class ServiceLocator implements Closeable {


    /* Logger */
    private static final Logger log = LoggerFactory.getLogger(ServiceLocator.class);

    /**
     * The shared instance of this ServiceLocator.
     */
    private static ServiceLocator instance = new ServiceLocator();


    private BeanFactory beanFactory = null;

    protected ServiceLocator() {
        init();
    }

    protected ServiceLocator(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        init();
    }

    public void init() {
        initBeanFactory();
    }

    @Override
    public void close() throws IOException {
        if (beanFactory != null) {
            log.debug("Closing ServiceLocator...");

            try {
                beanFactory.close();
            }
            catch(Exception e) {
                // log & continue
                log.debug("Could not close bean factory: " + e.getMessage(), e);
            }
        }
        beanFactory = null;
    }

    /**
     * replace the default shared instance of this Class
     *
     * @param newInstance the new shared service locator instance.
     */
    public static void setInstance(ServiceLocator newInstance) {
        instance = newInstance;
    }

    /**
     * Gets the shared instance of this Class
     *
     * @return the shared service locator instance.
     */
    public static ServiceLocator instance() {
        return instance;
    }

    protected void setBeanFactory(BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    public BlockchainRemoteService getBlockchainRemoteService() {
        return getBean(BlockchainRemoteService.class);
    }

    public TransactionRemoteService getTransactionRemoteService() {
        return getBean(TransactionRemoteService.class);
    }

    public CryptoService getCryptoService() {
        return getBean(CryptoService.class);
    }

    public HttpService getHttpService() {
        return getBean(HttpService.class);
    }
    public PeerService getPeerService() {
        return getBean(PeerService.class);
    }

    public CurrencyService getCurrencyService() {
        return getBean(CurrencyService.class);
    }

    public DataContext getDataContext() {
        return getBean(DataContext.class);
    }

    public NetworkRemoteService getNetworkRemoteService() {
        return getBean(NetworkRemoteService.class);
    }
    public WotRemoteService getWotRemoteService() {
        return getBean(WotRemoteService.class);
    }

    public CurrencyPodRemoteService getCurrencyPodRemoteService() {
        return getBean(CurrencyPodRemoteService.class);
    }

    public DividendService getDividendService() {
        return getBean(DividendService.class);
    }

    public MailService getMailService() {
        return getBean(MailService.class);
    }

    public NetworkService getNetworkService() {
        return getBean(NetworkService.class);
    }

    public PeerRepository getPeerRepository() {
        return getBean(PeerRepository.class);
    }

    public <S extends Bean> S getBean(Class<S> clazz) {
        if (beanFactory == null) {
            initBeanFactory();
        }
        return beanFactory.getBean(clazz);
    }

    /* -- Internal methods -- */

    protected void initBeanFactory() {
        if (beanFactory == null) {
            beanFactory = new BeanFactory();
        }
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

}
