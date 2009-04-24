package org.gparallelizer.actors.pooledActors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Feb 27, 2009
 */
public class EventMixinTest extends GroovyTestCase {

    public void testSomething() {}
    
    public void _testClassMixin() {
        volatile def result=null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        //todo react should not be required to take the timeout parameter
        TestCompany.metaClass {
            mixin DefaultPooledActor

            act = {->
                react(1) {
                    result = it
                    latch.countDown()
                }
            }

            afterStop = {
                stopFlag.set(true)
                stopLatch.countDown()
            }
        }

        final TestCompany company = new TestCompany(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])

        company.start()
        company.send("Message")
        latch.await(30, TimeUnit.SECONDS)
        company.stop()
        assertEquals('Message', result)
        stopLatch.await(30, TimeUnit.SECONDS)
        assert stopFlag.get()
    }

    public void _testInstanceMixin() {
        volatile def result=null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        final TestCorporation corp   = new TestCorporation(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])
        corp.metaClass {
            mixin DefaultPooledActor

            act = {->
                react(1) {
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

class TestCompany {
    String name
    List<String> employees
}

class TestCorporation {
    String name
    List<String> employees
}