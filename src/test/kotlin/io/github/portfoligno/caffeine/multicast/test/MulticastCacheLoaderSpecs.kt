package io.github.portfoligno.caffeine.multicast.test

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.portfoligno.caffeine.multicast.BulkCacheLoader
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

internal
fun loadConcurrentlyWithCounts(keys: Iterable<Int>): Pair<AtomicInteger, List<Future<Map<Int, Int>>>> {
  // Count how many times a inner cache loader is invoked
  val loadCounts = AtomicInteger()
  // A start signal
  val go = Semaphore(-1)

  // A cache built with a multicasted loader
  val cache = Caffeine
      .newBuilder()
      // No caching, to properly detect the multicast effect
      .expireAfterWrite(Duration.ZERO)
      .build(BulkCacheLoader.multicast(false) { ks: Iterable<Int> ->
        loadCounts.getAndIncrement()
        go.acquireUninterruptibly(0)

        // Some computation
        ks.associate { it to 2 * it }
      })

  // Load 10 times concurrently
  val sequence = generateSequence {
    Executors
        .newFixedThreadPool(1)
        .submit(Callable {
          cache.getAll(keys)
        })
  }
  val results: List<Future<Map<Int, Int>>> = sequence.take(10).toList()

  // Wait long enough for every thread ready
  Thread.sleep(2000)

  // Start
  go.release(1)

  return loadCounts to results
}
