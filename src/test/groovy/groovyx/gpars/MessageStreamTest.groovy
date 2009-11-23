package groovyx.gpars

import groovyx.gpars.MessageStream.ResultWaiter
import groovyx.gpars.actor.Actors
import java.util.concurrent.TimeUnit

/**
 * @author Vaclav Pech, Dierk Koenig
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

    void testSendWithoutArguments() {
        def called = false
        def stream = [send: {msg -> called = true; null }] as MessageStream
        stream.send()
        assert called
    }

    public void testForward() {
        volatile def flag1 = false
        volatile def flag2 = false
        volatile def flag3 = false

        def receiver = Actors.actor {
            react {
                flag1 = true
                react {
                    flag2 = true
                    react {
                        flag3 = true
                    }
                }
            }
        }

        def processor = Actors.actor {
            react {
                reply '1'
                it.reply '2'
                it.sender.send '3'
            }
        }

        processor.send('0', receiver)
        receiver.join()
        assert flag1 && flag2 && flag3
    }

}
