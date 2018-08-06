@file:JvmName("MulticastCacheLoader")
package io.github.portfoligno.caffeine.multicast

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

@JvmName("create")
fun <K, V> multicast(loader: BulkCacheLoader<K, V>): BulkCacheLoader<K, V> =
    MulticastCacheLoader(loader)

internal
class MulticastCacheLoader<K, V>(
    private
    val delegate: BulkCacheLoader<K, V>
) : BulkCacheLoader<K, V> {
  private
  val locks = ConcurrentHashMap<K, Semaphore>()

  override
  fun loadAll(keys: Iterable<K>): Map<K, V> {
    val lock = lazy {
      Semaphore(-1)
    }
    val iterator = keys.iterator()
    val toLoad = ArrayList<K>()
    val toWait = IdentityHashMap<Semaphore, Unit>()

    try {
      // First check for semaphore initialization
      while (iterator.hasNext()) {
        val k = iterator.next()

        val s = locks.computeIfAbsent(k) {
          lock.value
        }
        if (!lock.isInitialized()) {
          toWait[s] = Unit
        }
        else {
          // the mapping function is invoked exclusively
          toLoad.add(k)
          break
        }
      }
      // Check for matched identity of the semaphore
      if (lock.isInitialized()) {
        val lockValue = lock.value

        iterator.forEach {
          val s = locks.getOrPut(it) {
            lockValue
          }
          if (s !== lockValue) {
            toWait[s] = Unit
          }
          else {
            toLoad.add(it)
          }
        }
      }
      // Load all values with permission acquired
      val loaded =
          delegate.loadAll(Collections.unmodifiableList(toLoad))

      // Wait for other values
      toWait.keys.forEach {
        it.acquireUninterruptibly(0)
      }
      return loaded
    }
    finally {
      if (lock.isInitialized()) {
        // Release computation states
        val lockValue = lock.value
        lockValue.release()

        toLoad.forEach { k ->
          locks.computeIfPresent(k) { _, s ->
            if (s !== lockValue) s else null
          }
        }
      }
    }
  }
}
