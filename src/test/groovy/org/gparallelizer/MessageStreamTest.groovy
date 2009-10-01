package org.gparallelizer

import java.util.concurrent.TimeUnit
import org.gparallelizer.MessageStream.ResultWaiter

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Oct 1, 2009
 * Time: 9:40:15 PM
 * To change this template use File | Settings | File Templates.
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
