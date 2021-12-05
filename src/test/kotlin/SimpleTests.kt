import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import java.util.concurrent.TimeoutException

class SimpleTests {

    @Test
    fun lockSingleEntitySuccessfullyFromOneThread() {
        val entity = spy(TestEntity(1))

        EntityLocker.tryLockAndExecute(entity.id) { entity.testMethod() }
        Mockito.verify(entity).testMethod()
    }

    @Test
    fun lockSingleEntityLinearFromTwoThread() {
        val entity = spy(TestEntity(1))

        val firstThread = Thread({
            EntityLocker.tryLockAndExecute(entity.id) {
                entity.testMethod()
            }
        }, "first")

        val secondThread = Thread({
            EntityLocker.tryLockAndExecute(entity.id) {
                Thread.sleep(1000)
                entity.testMethod()
            }
        }, "second")
        firstThread.start()
        Thread.sleep(50)
        secondThread.start()
        Mockito.verify(entity).testMethod()
        firstThread.join()
        secondThread.join()
        Mockito.verify(entity, times(2)).testMethod()
    }

    @Test
    fun lockMultipleEntitySuccessfully() {
        val first = spy(TestEntity(1))
        val second = spy(TestEntity(2))

        val firstThread = Thread({
            EntityLocker.tryLockAndExecute(first.id) {
                Thread.sleep(1000)
                first.testMethod()
            }
        }, "first")

        val secondThread = Thread({
            EntityLocker.tryLockAndExecute(second.id) {
                second.testMethod()
            }
        }, "second")

        firstThread.start()
        secondThread.start()
        secondThread.join()
        Mockito.verify(second).testMethod()
        Mockito.verify(first, never()).testMethod()
        firstThread.join()
        Mockito.verify(first).testMethod()
    }

    @Test
    fun lockWithTimeoutSuccessfully() {
        val entity = spy(TestEntity(1))

        val firstThread = Thread({
            EntityLocker.tryLockAndExecute(entity.id) {
                entity.testMethod()
                Thread.sleep(500)
            }
        }, "first")

        val secondThread = Thread({
            EntityLocker.tryLockAndExecute(entity.id, timeout = 1500) {
                Thread.sleep(100) //For test purpose. If testMethod() invokes immediately Mockito.verify fails, because second thread already invoke this method.
                entity.testMethod()
            }
        }, "second")
        firstThread.start()
        Thread.sleep(50)
        secondThread.start()
        firstThread.join()
        Mockito.verify(entity).testMethod()
        secondThread.join()
        Mockito.verify(entity, times(2)).testMethod()
    }

    @Test
    fun lockWithTimeoutExceptionally() {
        val entity = spy(TestEntity(1))
        val exception = assertThrows<TimeoutException> {
            val firstThread = Thread({
                EntityLocker.tryLockAndExecute(entity.id) {
                    entity.testMethod()
                    Thread.sleep(1000)
                }
            }, "first")

            firstThread.start()
            Thread.sleep(50)
            Mockito.verify(entity).testMethod()
            EntityLocker.tryLockAndExecute(entity.id, timeout = 100) {
                entity.testMethod()
            }

            firstThread.join()
        }
        assertEquals("Unable to acquire lock for 1 in 100 ms.", exception.message)
    }
}