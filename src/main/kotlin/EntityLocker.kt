import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.locks.ReentrantLock

class EntityLocker {


    companion object {
        /**
         * Maps of locks.
         * Will grows infinitely. For real use:
         * 1. Must be limited.
         * 2. Must be cleared when limit reached.
         */
        private val locks: MutableMap<Any, ReentrantLock> = ConcurrentHashMap<Any, ReentrantLock>()

        /**
         * Try lock entity by ID.
         * If timeout not set try to acquire lock infinitely.
         * @throws TimeoutException when lock not acquired during non-zero timeout.
         */
        @JvmStatic
        fun tryLockAndExecute(id: Any, timeout: Long = 0, protectedCode: Runnable) {
            val lock = locks.computeIfAbsent(id) { ReentrantLock(true) }
            try {
                if (timeout == 0L) {
                    lock.lock()
                } else {
                    if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                        throw TimeoutException("Unable to acquire lock for $id in $timeout ms.")
                    }
                }
                protectedCode.run()
            } finally {
                if (lock.isHeldByCurrentThread) {
                    lock.unlock()
                }
            }
        }
    }
}