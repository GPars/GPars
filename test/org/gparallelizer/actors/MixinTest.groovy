package org.gparallelizer.actors

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 7, 2009
 */
public class MixinTest extends GroovyTestCase {

    public void testMixin1() {
    }

    public void _testMixin() {
        volatile def result=null
        final CountDownLatch latch = new CountDownLatch(1)

//        Company.metaClass {
//            mixin DefaultActor
//
//            act = {->
//                println 'AAAAAAAAAAAAAAAAAAaa'
//                receive {
//                    println 'BBBBBBBBBBBBBBBBBBBBBBBBBBB'
//                    result = it
//                    latch.countDown()
//                }
//            }
//        }

        final Company company = new Company(name: 'Company1', employees: ['Joe', 'Dave', 'Alice'])
        company.metaClass {
            mixin DefaultActor

            act = {->
                println 'AAAAAAAAAAAAAAAAAAaa'
                receive {
                    println 'BBBBBBBBBBBBBBBBBBBBBBBBBBB'
                    result = it
                    latch.countDown()
                }
            }
        }

        //todo enable mixins
        company.start()
        company.send("Message")
        latch.await(30, TimeUnit.SECONDS)
        company.stop()
        
        assertEquals('Message', result)
    }
}

class Company {
    String name
    List<String> employees

}