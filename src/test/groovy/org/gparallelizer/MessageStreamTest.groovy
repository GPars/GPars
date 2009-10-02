package org.gparallelizer

import java.util.concurrent.TimeUnit
import org.gparallelizer.MessageStream.ResultWaiter

/**
 * @author Vaclav Pech
 */
class MessageStreamTest extends GroovyTestCase {
    public void testResultWaiterWithTimeoutAndException() {
        final def waiter = new ResultWaiter()
        waiter.send new RuntimeException('test')
        shouldFail(RuntimeException) {
            waiter.result
        }
        shouldFail(RuntimeException) {
            waiter.getResult(10, TimeUnit.MILLISECONDS)
        }
    }
}
