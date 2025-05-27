package team.sipe.dolearn.ruthetum.sse

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

class DispatchersConfig

val Dispatchers.VIRTUAL_THREAD: ExecutorCoroutineDispatcher
    get() = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()