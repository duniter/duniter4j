package org.duniter.core.util;

/*-
 * #%L
 * SUMARiS :: Sumaris Core Shared
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
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
import com.google.common.collect.*;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.duniter.core.exception.TechnicalException;
import org.duniter.core.model.IEntity;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * helper class for beans (split by property, make sure list exists, ...)
 * Created by blavenie on 13/10/15.
 */
public class Beans {

    protected Beans() {
        // helper class does not instantiate
    }

    /**
     * <p>getList.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Collection<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Lists.newArrayList();
        } else if (list instanceof List<?>){
            return (List<E>) list;
        } else {
            return Lists.newArrayList(list);
        }
    }

    /**
     * <p>getList.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Iterable<E> iterable) {
        return getList(iterable.iterator());
    }

    /**
     * <p>getList.</p>
     *
     * @param iterator a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getList(Iterator<E> iterator) {
        List<E> result = Lists.newArrayList();
        while(iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    /**
     * <p>getStream.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> Stream<E> getStream(Iterable<E> iterable) {
        return getStream(iterable.spliterator());
    }

    public static <E> Stream<E> getStream(Spliterator<E> iterator) {
        return StreamSupport.stream(iterator, false);
    }

    /**
     * <p>getStream.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> E[] toArray(Iterable<E> iterable, IntFunction<E[]> generator) {
        return getStream(iterable).toArray(generator);
    }


    /**
     * <p>getList.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> Stream<E> getStream(Collection<E> list) {
        if (list == null) {
            return Stream.empty();
        }
        return list.stream();
    }

    public static <E> Stream<E> getStream(E[] array) {
        if (array == null) {
            return Stream.empty();
        }
        return Arrays.stream(array);
    }

    /**
     * <p>getListWithoutNull.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link List} object.
     */
    public static <E> List<E> getListWithoutNull(Collection<E> list) {
        List<E> result = getList(list);
        result.removeAll(Collections.singleton((E) null));
        return result;
    }

    /**
     * <p>getSet.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link Set} object.
     */
    public static <E> Set<E> getSet(Collection<E> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Sets.newHashSet();
        } else {
            return Sets.newHashSet(list);
        }
    }

    /**
     * <p>getSetWithoutNull.</p>
     *
     * @param list a {@link Collection} object.
     * @param <E> a E object.
     * @return a {@link Set} object.
     */
    public static <E> Set<E> getSetWithoutNull(Collection<E> list) {
        Set<E> result = getSet(list);
        result.removeAll(Collections.singleton((E) null));
        return result;
    }

    /**
     * <p>getMap.</p>
     *
     * @param map a {@link Map} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> Map<K, V> getMap(Map<K, V> map) {
        if (MapUtils.isEmpty(map)) {
            return Maps.newHashMap();
        } else {
            return Maps.newHashMap(map);
        }
    }

    public static <T> T safeGet(List<T> list, int index) {
        if (list == null)
            return null;
        try {
            return list.get(index);
        } catch (IndexOutOfBoundsException ignored) {
            return null;
        }
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> Map<K, V> splitByProperty(Iterable<V> list, String propertyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        if (list == null) return new HashMap<>();
        return getMap(Maps.uniqueIndex(list, input -> getProperty(input, propertyName)));
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param propertyName a {@link String} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K, V> ListMultimap<K, V> splitByNotUniqueProperty(Iterable<V> list, String propertyName) {
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        if (list == null) return ArrayListMultimap.create();
        return Multimaps.index(list, input -> getProperty(input, propertyName));
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <V> Multimap<Integer, V> splitByNotUniqueHashcode(Iterable<V> list) {
        return list != null ? Multimaps.index(list, Object::hashCode) : ArrayListMultimap.create();
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntity<K>> Map<K, V> splitById(Iterable<V> list) {
        return list != null ? getMap(Maps.uniqueIndex(list, IEntity::getId)) : new HashMap<>();
    }

    /**
     * <p>splitByProperty.</p>
     *
     * @param list a {@link Iterable} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link Map} object.
     */
    public static <K extends Serializable, V extends IEntity<K>> List<K> collectIds(Collection<V> list) {
        return transformCollection(list, IEntity::getId);
    }

    /**
     * <p>collectProperties.</p>
     *
     * @param collection a {@link Collection} object.
     * @param propertyName a {@link String} object.
     * @param <K> a K object.
     * @param <V> a V object.
     * @return a {@link List} object.
     */
    public static <K, V> List<K> collectProperties(Collection<V> collection, String propertyName) {
        if (CollectionUtils.isEmpty(collection)) return new ArrayList<>();
        Preconditions.checkArgument(StringUtils.isNotBlank(propertyName));
        return collection.stream().map((Function<V, K>) v -> getProperty(v, propertyName)).collect(Collectors.toList());

    }

    private static <K, V> Function<V, K> newPropertyFunction(final String propertyName) {
        return input -> getProperty(input, propertyName);
    }

    /**
     * <p>getProperty.</p>
     *
     * @param object       a K object.
     * @param propertyName a {@link String} object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     * @return a V object.
     */
    @SuppressWarnings("unchecked")
    public static <K, V> V getProperty(K object, String propertyName) {
        try {
            return (V) PropertyUtils.getProperty(object, propertyName);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new TechnicalException( String.format("Could not find property %1s on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    /**
     * <p>setProperty.</p>
     *
     * @param object       a K object.
     * @param propertyName a {@link String} object.
     * @param value        a V object.
     * @param <K>          a K object.
     * @param <V>          a V object.
     */
    public static <K, V> void setProperty(K object, String propertyName, V value) {
        try {
            PropertyUtils.setProperty(object, propertyName, value);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new TechnicalException( String.format("Could not set property %1s not found on object of type %2s", propertyName, object.getClass().getName()), e);
        }
    }

    public static Integer[] asIntegerArray(Collection<Integer> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.toArray(new Integer[0]);
    }

    public static String[] asStringArray(Collection<String> values) {
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        return values.toArray(new String[0]);
    }

    public static String[] asStringArray(String value, String delimiter) {
        if (StringUtils.isBlank(value)) return new String[0];
        StringTokenizer tokenizer = new StringTokenizer(value, delimiter);
        String[] values = new String[tokenizer.countTokens()];
        int i=0;
        while (tokenizer.hasMoreTokens()) {
            values[i] = tokenizer.nextToken();
            i++;
        }
        return values;
    }

    public static <E> List<E> filterCollection(Collection<E> collection, Predicate<E> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <O, E> List<O> transformCollection(Collection<? extends E> collection, Function<E, O> function) {
        return collection.stream().map(function).collect(Collectors.toList());
    }

    public static <K, V> Map<K, V> mergeMap(Map<K, V> map1, Map<K, V> map2) {
        if (MapUtils.isEmpty(map1)) return map2;
        if (MapUtils.isEmpty(map2)) return map1;
        return Stream.concat(map1.entrySet().stream(), map2.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

     /**
     * Compute equality of 2 Map
     * should return true if :
     * - both map is exact same object
     * - both are null
     * - both have same size and each entry set of first map are also present in the second
     *
     * @param map1 first map
     * @param map2 second map
     * @return true if they are equal
     */
    public static <K, V> boolean mapsAreEquals(Map<K, V> map1, Map<K, V> map2) {
        if (map1 == map2)
            return true;
        if (map1 == null || map2 == null || map1.size() != map2.size())
            return false;
        if (map1 instanceof IdentityHashMap || map2 instanceof IdentityHashMap)
            throw new IllegalArgumentException("Cannot compare IdentityHashMap's");
        return map1.entrySet().stream()
            .allMatch(e -> e.getValue().equals(map2.get(e.getKey())));
    }
}
