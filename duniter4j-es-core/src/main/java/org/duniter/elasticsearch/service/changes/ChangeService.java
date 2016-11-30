package org.duniter.elasticsearch.service.changes;

/*
 * #%L
 * Duniter4j :: ElasticSearch Plugin
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

/*
    Copyright 2015 ForgeRock AS

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.stream.BytesStreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.indexing.IndexingOperationListener;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesLifecycle;
import org.elasticsearch.indices.IndicesService;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChangeService {

    private static final String SETTING_PRIMARY_SHARD_ONLY = "duniter.changes.primaryShardOnly";
    private static final String SETTING_LISTEN_SOURCE = "duniter.changes.listenSource";

    private final ESLogger log = Loggers.getLogger(ChangeService.class);

    private static final Map<String, ChangeListener> LISTENERS = new HashMap<>();

    @Inject
    public ChangeService(final Settings settings, IndicesService indicesService) {
        final boolean allShards = !settings.getAsBoolean(SETTING_PRIMARY_SHARD_ONLY, Boolean.FALSE);
        final String[] sourcesStr = settings.getAsArray(SETTING_LISTEN_SOURCE, new String[]{"*"});
        final Set<ChangeSource> sources = new HashSet<>();
        for(String sourceStr : sourcesStr) {
            sources.add(new ChangeSource(sourceStr));
        }

        indicesService.indicesLifecycle().addListener(new IndicesLifecycle.Listener() {
            @Override
            public void afterIndexShardStarted(IndexShard indexShard) {
                final String indexName = indexShard.routingEntry().getIndex();
                if (allShards || indexShard.routingEntry().primary()) {

                    indexShard.indexingService().addListener(new IndexingOperationListener() {
                        @Override
                        public void postCreate(Engine.Create create) {
                            ChangeEvent change=new ChangeEvent(
                                    indexName,
                                    create.type(),
                                    create.id(),
                                    new DateTime(),
                                    ChangeEvent.Operation.CREATE,
                                    create.version(),
                                    create.source()
                            );

                            addChange(change);
                        }

                        @Override
                        public void postDelete(Engine.Delete delete) {
                            ChangeEvent change=new ChangeEvent(
                                    indexName,
                                    delete.type(),
                                    delete.id(),
                                    new DateTime(),
                                    ChangeEvent.Operation.DELETE,
                                    delete.version(),
                                    null
                            );

                            addChange(change);
                        }

                        @Override
                        public void postIndex(Engine.Index index) {

                            ChangeEvent change=new ChangeEvent(
                                    indexName,
                                    index.type(),
                                    index.id(),
                                    new DateTime(),
                                    ChangeEvent.Operation.INDEX,
                                    index.version(),
                                    index.source()
                            );

                            addChange(change);
                        }

                        private boolean filter(String index, String type, String id, ChangeSource source) {
                            if (source.getIndices() != null && !source.getIndices().contains(index)) {
                                return false;
                            }

                            if (source.getTypes() != null && !source.getTypes().contains(type)) {
                                return false;
                            }

                            if (source.getIds() != null && !source.getIds().contains(id)) {
                                return false;
                            }

                            return true;
                        }

                        private boolean filter(String index, ChangeEvent change) {
                            for (ChangeSource source : sources) {
                                if (filter(index, change.getType(), change.getId(), source)) {
                                    return true;
                                }
                            }

                            return false;
                        }

                        private void addChange(ChangeEvent change) {

                            if (!filter(indexName, change)) {
                                return;
                            }


                            String message;
                            try {
                                XContentBuilder builder = new XContentBuilder(JsonXContent.jsonXContent, new BytesStreamOutput());
                                builder.startObject()
                                        .field("_index", indexName)
                                        .field("_type", change.getType())
                                        .field("_id", change.getId())
                                        .field("_timestamp", change.getTimestamp())
                                        .field("_version", change.getVersion())
                                        .field("_operation", change.getOperation().toString());
                                if (change.getSource() != null) {
                                    builder.rawField("_source", change.getSource());
                                }
                                builder.endObject();

                                message = builder.string();
                            } catch (IOException e) {
                                log.error("Failed to write JSON", e);
                                return;
                            }

                            for (ChangeListener listener : LISTENERS.values()) {
                                try {
                                    listener.onChanges(message);
                                } catch (Exception e) {
                                    log.error("Failed to send message", e);
                                }

                            }

                        }
                    });
                }
            }

        });
    }

    public static void registerListener(ChangeListener listener) {
        LISTENERS.put(listener.getId(), listener);
    }

    public static void unregisterListener(ChangeListener listener) {
        LISTENERS.remove(listener.getId());
    }


}
