package org.duniter.core.client.repositories.mem;

/*
 * #%L
 * UCoin Java :: Core Client API
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

import com.google.common.base.Preconditions;
import org.duniter.core.beans.Bean;
import org.duniter.core.model.IEntity;
import org.duniter.core.repositories.CrudRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Created by blavenie on 29/12/15.
 */
public interface MemoryCrudRepository<ID extends Serializable, T extends IEntity<ID>> extends CrudRepository<ID, T> {

    default void delete(T entity) {
        Preconditions.checkNotNull(entity);
        deleteById(entity.getId());
    }

    default <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
        if (entities == null) return null;
        List<S> result = new ArrayList<>();
        entities.forEach(entity -> {
            S savedEntity = this.save(entity);
            result.add(savedEntity);
        });
        return result;
    }

    default Iterable<T> findAllById(Iterable<ID> ids) {
        if (ids == null) return null;
        List<T> result = new ArrayList<>();
        ids.forEach(entity -> this.findById(entity).map(result::add));
        return result;
    }

    default void deleteAll(Iterable<? extends T> entities) {
        if (entities != null) {
            entities.forEach(this::delete);
        }
    }

    default void deleteAll() {
        deleteAll(findAll());
    }
}
