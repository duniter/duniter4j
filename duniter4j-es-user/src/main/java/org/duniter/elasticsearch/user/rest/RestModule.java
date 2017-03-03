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

import org.duniter.elasticsearch.user.rest.group.RestGroupIndexAction;
import org.duniter.elasticsearch.user.rest.group.RestGroupUpdateAction;
import org.duniter.elasticsearch.user.rest.history.RestHistoryDeleteIndexAction;
import org.duniter.elasticsearch.user.rest.invitation.RestInvitationCertificationIndexAction;
import org.duniter.elasticsearch.user.rest.invitation.RestInvitationCertificationMarkAsReadAction;
import org.duniter.elasticsearch.user.rest.message.RestMessageInboxIndexAction;
import org.duniter.elasticsearch.user.rest.message.RestMessageInboxMarkAsReadAction;
import org.duniter.elasticsearch.user.rest.message.RestMessageOutboxIndexAction;
import org.duniter.elasticsearch.user.rest.message.compat.RestMessageRecordGetAction;
import org.duniter.elasticsearch.user.rest.message.compat.RestMessageRecordIndexAction;
import org.duniter.elasticsearch.user.rest.message.compat.RestMessageRecordMarkAsReadAction;
import org.duniter.elasticsearch.user.rest.message.compat.RestMessageRecordSearchAction;
import org.duniter.elasticsearch.user.rest.user.*;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Module;

public class RestModule extends AbstractModule implements Module {

    @Override protected void configure() {

        // User
        bind(RestUserProfileIndexAction.class).asEagerSingleton();
        bind(RestUserProfileUpdateAction.class).asEagerSingleton();
        bind(RestUserSettingsIndexAction.class).asEagerSingleton();
        bind(RestUserSettingsUpdateAction.class).asEagerSingleton();
        bind(RestUserEventMarkAsReadAction.class).asEagerSingleton();
        bind(RestUserEventSearchAction.class).asEagerSingleton();
        bind(RestUserAvatarAction.class).asEagerSingleton();

        // Group
        bind(RestGroupIndexAction.class).asEagerSingleton();
        bind(RestGroupUpdateAction.class).asEagerSingleton();

        // History
        bind(RestHistoryDeleteIndexAction.class).asEagerSingleton();

        // Message
        bind(RestMessageInboxIndexAction.class).asEagerSingleton();
        bind(RestMessageOutboxIndexAction.class).asEagerSingleton();
        bind(RestMessageInboxMarkAsReadAction.class).asEagerSingleton();

        // Invitation
        bind(RestInvitationCertificationIndexAction.class).asEagerSingleton();
        bind(RestInvitationCertificationMarkAsReadAction.class).asEagerSingleton();

        // Backward compatibility
        {
            // message/record
            bind(RestMessageRecordIndexAction.class).asEagerSingleton();
            bind(RestMessageRecordSearchAction.class).asEagerSingleton();
            bind(RestMessageRecordGetAction.class).asEagerSingleton();
            bind(RestMessageRecordMarkAsReadAction.class).asEagerSingleton();
        }
    }
}