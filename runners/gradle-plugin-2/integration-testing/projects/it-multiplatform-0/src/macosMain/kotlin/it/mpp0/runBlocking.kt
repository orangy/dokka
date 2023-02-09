package it.mpp0

import kotlinx.coroutines.CoroutineScope

actual fun <T> CoroutineScope.runBlocking(block: suspend () -> T): T {
    TODO("Not yet implemented")
}
