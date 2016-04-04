package io.ucoin.ucoinj.elasticsearch.action;

/*
 * #%L
 * ucoinj-elasticsearch-plugin
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

import io.ucoin.ucoinj.elasticsearch.action.currency.RestCurrencyIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.market.RestMarketDemandIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.market.RestMarketOfferIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.registry.RestRegistryRecordIndexAction;
import io.ucoin.ucoinj.elasticsearch.action.security.RestSecurityAuthAction;
import io.ucoin.ucoinj.elasticsearch.action.security.RestSecurityGetChallengeAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class RestModule extends AbstractModule implements Module {

    @Override protected void configure() {
        bind(RestCurrencyIndexAction.class).asEagerSingleton();

        bind(RestMarketOfferIndexAction.class).asEagerSingleton();
        bind(RestMarketDemandIndexAction.class).asEagerSingleton();

        bind(RestRegistryRecordIndexAction.class).asEagerSingleton();

        bind(RestSecurityGetChallengeAction.class).asEagerSingleton();
        bind(RestSecurityAuthAction.class).asEagerSingleton();
    }
}