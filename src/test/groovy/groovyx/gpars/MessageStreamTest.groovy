package groovyx.gpars

import groovyx.gpars.MessageStream.ResultWaiter
import java.util.concurrent.TimeUnit

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
  
  void testSendWithoutArguments() {
    def called = false
    def stream = [send: { msg -> called = true; null }] as MessageStream
    stream.send()
    assert called
  }

}
