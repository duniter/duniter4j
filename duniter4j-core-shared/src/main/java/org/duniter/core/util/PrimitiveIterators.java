package org.duniter.core.util;

/*-
 * #%L
 * Duniter4j :: Core Shared
 * %%
 * Copyright (C) 2014 - 2017 EIS
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

import java.util.Iterator;
import java.util.PrimitiveIterator;

public class PrimitiveIterators {

    public interface OfLong extends PrimitiveIterator.OfLong {
        long current();
    }

    private static OfLong nullLongSequence = new OfLong() {

        @Override
        public long nextLong() {
            return 0;
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        public long current() {
            return 0;
        }
    };

    public static class LongSequence implements OfLong {
        long value = 0;

        @Override
        public long nextLong() {
            return value++;
        }

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public long current() {
            return value;
        }
    }

    public static OfLong newLongSequence() {
        return new LongSequence();
    }

    public static OfLong nullLongSequence() {
        return nullLongSequence;
    }
}
