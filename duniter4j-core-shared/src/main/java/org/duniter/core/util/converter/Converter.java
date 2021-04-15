package org.duniter.core.util.converter;

import javax.annotation.Nullable;

@FunctionalInterface
public interface Converter<S, T> {
    @Nullable
    T convert(S var1);
}