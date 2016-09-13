package org.duniter.core.model;

/*
 * #%L
 * SIH-Adagio :: Synchro Server WebApp
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2012 - 2014 Ifremer
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


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

public class NullProgressionModel implements ProgressionModel, Serializable {

    private static final long serialVersionUID = 1L;

    public NullProgressionModel() {
        super();
    }

    @Override
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
    }

    @Override
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public synchronized String getMessage() {
        return null;
    }

    @Override
    public synchronized void setTask(String task) {
    }

    @Override
    public synchronized String getTask() {
        return null;
    }

    @Override
    public synchronized void setMessage(String progressionMessage) {
    }

    @Override
    public synchronized void setTotal(int total) {
    }

    @Override
    public int getTotal() {
        return 0;
    }

    @Override
    public synchronized void setCurrent(int current) {
    }

    @Override
    public synchronized int getCurrent() {
        return 0;
    }

    @Override
    public synchronized void increment() {}

    @Override
    public synchronized void increment(int increment){}

    @Override
    public synchronized void increment(String message) {}

    public boolean isCancel() {
        return false;
    }

    @Override
    public void cancel() {}

    @Override
    public synchronized Status getStatus() {
        return null;
    }

    @Override
    public synchronized void setStatus(Status progressionStatus) {
    }

}
