package org.duniter.core.util.concurrent;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Helper class on CompletableFuture and concurrent classes
 * Created by blavenie on 24/03/17.
 */
public class CompletableFutures {

    private CompletableFutures() {
    }

    public static <T> CompletableFuture<List<T>> allOfToList(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> allDoneFuture =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v ->
                futures.stream()
                        .map(future -> future.join())
                        .filter(peer -> peer != null) // skip
                        .collect(Collectors.toList())
        );
    }

    public static <T> CompletableFuture<List<T>> allOfToList(List<CompletableFuture<T>> futures, Predicate<? super T> filter) {
        CompletableFuture<Void> allDoneFuture =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()]));
        return allDoneFuture.thenApply(v ->
                futures.stream()
                        .map(future -> future.join())
                        .filter(filter)
                        .collect(Collectors.toList())
        );
    }
}
