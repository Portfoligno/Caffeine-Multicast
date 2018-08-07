package io.github.portfoligno.caffeine.multicast.test

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.portfoligno.caffeine.multicast.BulkCacheLoader.multicast
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicInteger

class MulticastCacheLoaderSpec : StringSpec({
  "Duplicated loading should be skipped" {
    // Count how many times a inner cache loader is invoked
    val loadCount = AtomicInteger()
    // A start signal
    val go = Semaphore(-1)

    // A cache built with a multicasted loader
    val cache = Caffeine
        .newBuilder()
        // No caching, to properly detect the multicast effect
        .expireAfterWrite(Duration.ZERO)
        .build(multicast(false) { ks: Iterable<Int> ->
          loadCount.getAndIncrement()
          go.acquireUninterruptibly(0)

          // Some computation
          ks.associate { it to 2 * it }
        })

    // Load 10 times concurrently
    val sequence = generateSequence {
      Executors
          .newFixedThreadPool(1)
          .submit(Callable {
            cache.getAll(listOf(42, 42, 44))
          })
    }
    val results: List<Future<Map<Int, Int>>> = sequence.take(10).toList()

    // Wait long enough for every thread ready
    do {
      Thread.sleep(2000)
    }
    while (loadCount.get() == 0)

    // Start
    go.release(1)

    // Check results
    results.reduce { a, b ->
      // Results should be consistent
      b.get() shouldBe a.get()
      a
    }
    // This should be fail for either a malfunction or not enough waiting
    loadCount.get() shouldBe 1
  }
})
