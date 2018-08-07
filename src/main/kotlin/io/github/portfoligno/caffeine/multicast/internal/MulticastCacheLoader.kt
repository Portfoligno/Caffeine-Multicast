package io.github.portfoligno.caffeine.multicast.internal

import io.github.portfoligno.caffeine.multicast.BulkCacheLoader
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.Semaphore
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

internal
class MulticastCacheLoader<K, V>(
    private
    val identityKeyEquivalence: Boolean,
    private
    val delegate: BulkCacheLoader<K, V>
) : BulkCacheLoader<K, V> {
  private
  val locks: MutableMap<K, Semaphore> =
      if (!identityKeyEquivalence) ConcurrentHashMap() else weakMapOf()
  private
  val temporaryResults = weakMapOf<Semaphore, Either<Throwable, Map<K, V>>>()

  override
  fun loadAll(keys: Iterable<K>): Map<K, V> {
    val lock = lazy {
      Semaphore(-1)
    }
    val iterator = keys.iterator()
    val toLoad = ArrayList<K>()
    val toWait = IdentityHashMap<Semaphore, MutableList<K>>()

    try {
      // First check for semaphore initialization
      while (iterator.hasNext()) {
        val k = iterator.next()

        val s = locks.computeIfAbsent(k) {
          lock.value
        }
        if (!lock.isInitialized()) {
          toWait.getOrPut(s) { ArrayList() }.add(k)
        }
        else {
          // the mapping function is invoked exclusively
          toLoad.add(k)
          break
        }
      }

      // Skip loading if there is no keys to load
      val loaded = if (!lock.isInitialized()) mapOf() else {
        // Check for matched identity of the semaphore
        val lockValue = lock.value

        iterator.forEach { k ->
          val s = locks.getOrPut(k) {
            lockValue
          }
          if (s !== lockValue) {
            toWait.getOrPut(s) { ArrayList() }.add(k)
          }
          else {
            toLoad.add(k)
          }
        }

        // Load all values with permission acquired
        delegate
            .loadAll(Collections.unmodifiableList(toLoad))
            .also {
              // Share loaded values
              temporaryResults[lockValue] = Either.Right(it)
            }
      }

      if (toWait.isNotEmpty()) {
        // Combined results
        val results: MutableMap<K, V> =
            if (!identityKeyEquivalence) HashMap(loaded) else IdentityHashMap(loaded)

        // Wait for other values
        toWait.forEach { s, ks ->
          s.acquireUninterruptibly(0)
          val r = temporaryResults[s]

          r!!.fold({
            // Propagate the error
            throw ExecutionException(it)
          }) { m ->
            // Gather successful values
            ks.forEach { k ->
              m[k]?.let { results[k] = it }
            }
          }
        }
        return results
      }
      // Plain results
      return loaded
    }
    catch (t: Throwable) {
      if (lock.isInitialized()) {
        // Result as an error
        temporaryResults[lock.value] = Either.Left(t)
      }
      throw t
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
