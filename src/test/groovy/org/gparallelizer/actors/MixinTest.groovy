package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class MixinTest extends GroovyTestCase {

    public void testClassMixin() {
        volatile def result=null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        Company.metaClass {
            mixin DefaultActor

            act = {->
                receive {
                    result = it
                    latch.countDown()
                }
            }

            afterStop = {
                stopFlag.set(true)
                stopLatch.countDown()
            }
        }

        final Company company = new Company(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])

        company.start()
        company.send("Message")
        latch.await(30, TimeUnit.SECONDS)
        company.stop()
        assertEquals('Message', result)
        stopLatch.await(30, TimeUnit.SECONDS)
        assert stopFlag.get()
    }

    public void testInstanceMixin() {
        volatile def result=null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        final Corporation corp   = new Corporation(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])
        corp.metaClass {
            mixin DefaultActor

            act = {->
                receive {
                    result = it
                    latch.countDown()
                }
            }

            afterStop = {
                stopFlag.set(true)
                stopLatch.countDown()
            }
        }

        corp.start()
        corp.send("Message")
        latch.await(30, TimeUnit.SECONDS)
        corp.stop()
        assertEquals('Message', result)
        stopLatch.await(30, TimeUnit.SECONDS)
        assert stopFlag.get()
    }
}

class Company {
    String name
    List<String> employees
}

class Corporation {
    String name
    List<String> employees
}