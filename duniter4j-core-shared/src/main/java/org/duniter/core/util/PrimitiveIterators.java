package org.duniter.core.util;

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
