package io.github.portfoligno.caffeine.multicast.internal

internal
sealed class Either<out A, out B> {
  class Left<out A>(val value: A): Either<A, Nothing>()

  class Right<out B>(val value: B): Either<Nothing, B>()
}

internal
inline fun <A, B, X> Either<A, B>.fold(fa: (A) -> X, fb: (B) -> X): X =
    when (this) {
      is Either.Left -> fa(value)
      is Either.Right -> fb(value)
    }
