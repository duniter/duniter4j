package org.duniter.core.client.repositories;

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

import org.duniter.core.beans.Bean;
import org.duniter.core.client.model.local.ICurrency;
import org.duniter.core.repositories.CrudRepository;
import org.duniter.core.util.Beans;

import java.util.stream.Collectors;

/**
 * Created by eis on 07/02/15.
 */
public interface CurrencyRepository<E extends ICurrency> extends Bean, CrudRepository<String, E> {

    default Iterable<String> findAllIds() {
        return Beans.getStream(findAll()).map(E::getId).collect(Collectors.toList());
    }
}
