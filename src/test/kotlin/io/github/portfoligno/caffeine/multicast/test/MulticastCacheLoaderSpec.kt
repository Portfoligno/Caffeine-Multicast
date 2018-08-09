package io.github.portfoligno.caffeine.multicast.test

import io.kotlintest.matchers.beEmpty
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class MulticastCacheLoaderSpec : StringSpec({
  "Get empty should be empty" {
    val (loadCounts, results) =
        loadConcurrentlyWithCounts(setOf())

    // Check results
    results.forEach {
      // Results should be empty
      it.get().entries should beEmpty()
    }

    // Inner loader is not called
    loadCounts.get() shouldBe 0
  }

  "Get singleton should be singleton" {
    val (loadCounts, results) =
        loadConcurrentlyWithCounts(setOf(5))

    // Check results
    results.forEach {
      // Results should be singleton
      it.get() shouldBe mapOf(5 to 10)
    }

    // This should be fail for either a malfunction or not enough waiting
    loadCounts.get() shouldBe 1
  }

  "Get multiple keys should be consistent" {
    val (loadCounts, results) =
        loadConcurrentlyWithCounts(listOf(42, 42, 44))

    // Check results
    results.reduce { a, b ->
      // Results should be consistent
      b.get() shouldBe a.get()
      a
    }

    // This should fail for either a malfunction or not enough waiting
    loadCounts.get() shouldBe 1
  }
})
