package team.sipe.dolearn.ruthetum.dlock.functional

import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Component
class LockManager(private val redissonClient: RedissonClient) {

    fun <R> userLock(userId: Long, block: () -> R): R {
        val lock = redissonClient.getLock("user:$userId")
        return try {
            lock.lock()
            block()
        } finally {
            lock.unlock()
        }
    }
}