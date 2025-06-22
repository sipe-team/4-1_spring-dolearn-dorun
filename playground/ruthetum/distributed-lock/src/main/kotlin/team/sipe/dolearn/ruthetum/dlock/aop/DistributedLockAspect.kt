package team.sipe.dolearn.ruthetum.dlock.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component

@Aspect
@Component
class DistributedLockAspect(private val redissonClient: RedissonClient) {

    @Around("@annotation(DistributedLock) && args(lockKey, ..)")
    fun executeWithLock(
        joinPoint: ProceedingJoinPoint,
        distributedLock: DistributedLock,
        lockKey: String,
    ): Any? {
        val lock = redissonClient.getLock(lockKey)
        return try {
            lock.lock()
            joinPoint.proceed()
        } finally {
            lock.unlock()
        }
    }
}