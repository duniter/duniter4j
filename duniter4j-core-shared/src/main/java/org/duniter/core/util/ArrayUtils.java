package org.duniter.core.util;

/*
 * #%L
 * UCoin Java :: Core Shared
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Created by eis on 23/12/14.
 */
public class ArrayUtils {

    public static boolean isNotEmpty(Object[] coll) {
        return coll != null && coll.length > 0;
    }
    public static boolean isEmpty(Object[] coll) {
        return coll == null || coll.length == 0;
    }

    public static String join(final Object[] array) {
        return join(array, ", ");
    }

    public static String join(final Object[] array, final  String separator) {
        if (array == null || array.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder(array.length * 7);
        sb.append(array[0]);
        for (int i = 1; i < array.length; i++) {
            sb.append(separator);
            sb.append(array[i]);
        }
        return sb.toString();
    }

    public static int size(final Object[] array) {
        return array == null ? 0 : array.length;
    }

}
