package org.duniter.elasticsearch.action;

/*
 * #%L
 * duniter4j-elasticsearch-plugin
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

import org.duniter.elasticsearch.rest.currency.RestCurrencyIndexAction;
import org.duniter.elasticsearch.rest.history.RestHistoryDeleteIndexAction;
import org.duniter.elasticsearch.action.market.*;
import org.duniter.elasticsearch.action.message.RestMessageInboxIndexAction;
import org.duniter.elasticsearch.action.message.RestMessageOutboxIndexAction;
import org.duniter.elasticsearch.action.registry.*;
import org.duniter.elasticsearch.rest.security.RestSecurityAuthAction;
import org.duniter.elasticsearch.rest.security.RestSecurityController;
import org.duniter.elasticsearch.rest.security.RestSecurityFilter;
import org.duniter.elasticsearch.rest.security.RestSecurityGetChallengeAction;
import org.duniter.elasticsearch.action.user.RestUserProfileIndexAction;
import org.duniter.elasticsearch.action.user.RestUserProfileUpdateAction;
import org.duniter.elasticsearch.action.user.RestUserSettingsIndexAction;
import org.duniter.elasticsearch.action.user.RestUserSettingsUpdateAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class RestModule extends AbstractModule implements Module {

    @Override protected void configure() {

        // Currency
        bind(RestCurrencyIndexAction.class).asEagerSingleton();

        // Market
        bind(RestMarketRecordIndexAction.class).asEagerSingleton();
        bind(RestMarketRecordUpdateAction.class).asEagerSingleton();
        bind(RestMarketCommentIndexAction.class).asEagerSingleton();
        bind(RestMarketCommentUpdateAction.class).asEagerSingleton();
        bind(RestMarketCategoryAction.class).asEagerSingleton();

        // Registry
        bind(RestRegistryRecordIndexAction.class).asEagerSingleton();
        bind(RestRegistryRecordUpdateAction.class).asEagerSingleton();
        bind(RestRegistryCommentIndexAction.class).asEagerSingleton();
        bind(RestregistryCommentUpdateAction.class).asEagerSingleton();
        bind(RestRegistryCategoryAction.class).asEagerSingleton();

        // User
        bind(RestUserProfileIndexAction.class).asEagerSingleton();
        bind(RestUserProfileUpdateAction.class).asEagerSingleton();
        bind(RestUserSettingsIndexAction.class).asEagerSingleton();
        bind(RestUserSettingsUpdateAction.class).asEagerSingleton();

        // Authentication & Security
        bind(RestSecurityGetChallengeAction.class).asEagerSingleton();
        bind(RestSecurityAuthAction.class).asEagerSingleton();
        bind(RestSecurityFilter.class).asEagerSingleton();
        bind(RestSecurityController.class).asEagerSingleton();

        // History
        bind(RestHistoryDeleteIndexAction.class).asEagerSingleton();

        // Message
        bind(RestMessageInboxIndexAction.class).asEagerSingleton();
        bind(RestMessageOutboxIndexAction.class).asEagerSingleton();
    }
}