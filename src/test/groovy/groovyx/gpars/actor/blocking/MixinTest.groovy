//  GPars (formerly GParallelizer)
//
//  Copyright © 2008-9  The original author or authors
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License. 

package groovyx.gpars.actor.blocking

import groovyx.gpars.actor.AbstractPooledActor
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class MixinTest extends GroovyTestCase {

    public void testSomething() {
        //todo enable the mixin tests or remove
    }

    public void _testClassMixin() {
        volatile def result = null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        Company.metaClass {
            mixin AbstractPooledActor

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
        latch.await(90, TimeUnit.SECONDS)
        company.stop()
        assertEquals('Message', result)
        stopLatch.await(90, TimeUnit.SECONDS)
        assert stopFlag.get()
    }

    public void _testInstanceMixin() {
        volatile def result = null
        final CountDownLatch latch = new CountDownLatch(1)
        final CountDownLatch stopLatch = new CountDownLatch(1)
        final AtomicBoolean stopFlag = new AtomicBoolean(false)

        final Corporation corp = new Corporation(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])
        corp.metaClass {
            mixin AbstractPooledActor

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
        latch.await(90, TimeUnit.SECONDS)
        corp.stop()
        assertEquals('Message', result)
        stopLatch.await(90, TimeUnit.SECONDS)
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
