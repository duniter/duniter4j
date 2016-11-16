package org.duniter.elasticsearch.websocket;

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

import org.elasticsearch.common.bytes.BytesReference;
import org.joda.time.DateTime;

public class ChangeEvent {
    private final String id;
    private final String type;
    private final DateTime timestamp;
    private final Operation operation;
    private final long version;
    private final BytesReference source;

    public enum Operation {
        INDEX,CREATE,DELETE
    }

    public ChangeEvent(String id, String type, DateTime timestamp, Operation operation, long version, BytesReference source) {
        this.id = id;
        this.type = type;
        this.timestamp = timestamp;
        this.operation = operation;
        this.version = version;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public Operation getOperation() {
        return operation;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getType() {
        return type;
    }

    public long getVersion() {
        return version;
    }

    public BytesReference getSource() {
        return source;
    }
}
