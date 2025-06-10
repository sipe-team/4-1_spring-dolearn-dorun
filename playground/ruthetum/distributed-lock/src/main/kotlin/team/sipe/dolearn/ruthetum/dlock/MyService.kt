package team.sipe.dolearn.ruthetum.dlock

import org.springframework.stereotype.Service
import team.sipe.dolearn.ruthetum.dlock.aop.DistributedLock
import team.sipe.dolearn.ruthetum.dlock.functional.LockManager

@Service
class MyService(
    private val lockManager: LockManager,
) {
    @DistributedLock
    fun executeWithAopLock(lockKey: String) {
        // Perform some operation that requires a lock
        println("Doing something with user 1")
        // Simulate some work
        Thread.sleep(1000)
    }

    fun executeWithFunctionalLock(userId: Long) {
        lockManager.userLock(userId) {
            // Perform some operation that requires a lock
            println("Doing something with user 1")
            // Simulate some work
            Thread.sleep(1000)
        }
    }
}