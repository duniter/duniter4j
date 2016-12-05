package org.duniter.elasticsearch;

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

import com.google.common.collect.Lists;
import org.duniter.elasticsearch.rest.RestModule;
import org.duniter.elasticsearch.security.SecurityModule;
import org.duniter.elasticsearch.service.ServiceModule;
import org.duniter.elasticsearch.threadpool.ThreadPool;
import org.duniter.elasticsearch.websocket.WebSocketModule;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;

import java.util.Collection;

public class Plugin extends org.elasticsearch.plugins.Plugin {

    private ESLogger log = ESLoggerFactory.getLogger(Plugin.class.getName());

    private boolean enable;

    @Inject public Plugin(Settings settings) {
        this.enable = settings.getAsBoolean("duniter.enabled", true);
    }

    @Override
    public String name() {
        return "duniter4j-es-core";
    }

    @Override
    public String description() {
        return "Duniter ElasticSearch Plugin";
    }

    @Override
    public Collection<Module> nodeModules() {
        Collection<Module> modules = Lists.newArrayList();
        if (!enable) {
            log.warn(description() + " has been disabled.");
            return modules;
        }
        modules.add(new SecurityModule());

        modules.add(new WebSocketModule());
        modules.add(new RestModule());

        modules.add(new ServiceModule());

        return modules;
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        Collection<Class<? extends LifecycleComponent>> components = Lists.newArrayList();
        if (!enable) {
            return components;
        }
        components.add(PluginSettings.class);
        components.add(ThreadPool.class);
        components.add(PluginInit.class);
        return components;
    }

    /* -- protected methods -- */


}