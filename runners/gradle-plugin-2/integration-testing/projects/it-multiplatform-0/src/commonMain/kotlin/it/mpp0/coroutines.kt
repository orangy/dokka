package it.mpp0

import kotlinx.coroutines.CoroutineScope

expect fun <T> CoroutineScope.runBlocking(block: suspend () -> T) : T
