package team.sipe.dolearn.ruthetum.sse

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SampleThreadController {
    private val log = lazy { LoggerFactory.getLogger(this.javaClass) }.value

    @GetMapping("/threads")
    fun thread(): String {
        val currThread = Thread.currentThread()
        log.info("Current thread: ${currThread.name}, is virtual: ${currThread.isVirtual}")
        return currThread.name
    }
}