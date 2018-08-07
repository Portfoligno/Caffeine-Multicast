package io.github.portfoligno.caffeine.multicast;

import com.github.benmanes.caffeine.cache.CacheLoader;
import io.github.portfoligno.caffeine.multicast.internal.MulticastCacheLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

@FunctionalInterface
public interface BulkCacheLoader<K, V> extends CacheLoader<K, V> {
  @NotNull Map<K, V> loadAll(@NotNull Iterable<? extends K> keys) throws Exception;

  @Override
  default @Nullable V load(@NotNull K key) throws Exception {
    return loadAll(Collections.singleton(key)).get(key);
  }

  static <K, V> @NotNull BulkCacheLoader<K, V> multicast(
      boolean identityKey,
      @NotNull BulkCacheLoader<K, V> loader
  ) {
    return new MulticastCacheLoader<>(identityKey, loader);
  }
}
