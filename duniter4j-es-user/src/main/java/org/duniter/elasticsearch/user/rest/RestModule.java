package org.duniter.elasticsearch.user.rest;

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

import org.duniter.elasticsearch.user.rest.history.RestHistoryDeleteIndexAction;
import org.duniter.elasticsearch.user.rest.message.RestMessageInboxIndexAction;
import org.duniter.elasticsearch.user.rest.message.RestMessageOutboxIndexAction;
import org.duniter.elasticsearch.user.rest.user.RestUserProfileIndexAction;
import org.duniter.elasticsearch.user.rest.user.RestUserProfileUpdateAction;
import org.duniter.elasticsearch.user.rest.user.RestUserSettingsIndexAction;
import org.duniter.elasticsearch.user.rest.user.RestUserSettingsUpdateAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class RestModule extends AbstractModule implements Module {

    @Override protected void configure() {

        // User
        bind(RestUserProfileIndexAction.class).asEagerSingleton();
        bind(RestUserProfileUpdateAction.class).asEagerSingleton();
        bind(RestUserSettingsIndexAction.class).asEagerSingleton();
        bind(RestUserSettingsUpdateAction.class).asEagerSingleton();

        // History
        bind(RestHistoryDeleteIndexAction.class).asEagerSingleton();

        // Message
        bind(RestMessageInboxIndexAction.class).asEagerSingleton();
        bind(RestMessageOutboxIndexAction.class).asEagerSingleton();
    }
}