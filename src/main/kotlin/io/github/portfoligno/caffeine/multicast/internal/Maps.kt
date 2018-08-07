package io.github.portfoligno.caffeine.multicast.internal

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.ConcurrentMap

internal
fun <K, V> weakMapOf(): ConcurrentMap<K, V> =
    Caffeine
        .newBuilder()
        .weakKeys()
        .build<K, V>()
        .asMap()
