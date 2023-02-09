package it.mpp0

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

actual fun <T> CoroutineScope.runBlocking(block: suspend () -> T): T {
    TODO("Not yet implemented")
}

fun <T> CoroutineScope.customAsync(block: suspend () -> T): Deferred<T> {
    return async { block() }
}
