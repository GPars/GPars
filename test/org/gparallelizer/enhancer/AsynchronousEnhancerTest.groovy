package org.gparallelizer.enhancer

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.gparallelizer.Asynchronizer
import java.util.concurrent.ExecutorService

/**
 *
 * @author Vaclav Pech
 * Date: Jan 9, 2009
 */
public class AsynchronousEnhancerTest extends GroovyTestCase {

    protected void tearDown() {
        super.tearDown();
        AsynchronousEnhancer.unenhanceClass(String)
    }


    public void testEnhancement() {
        AsynchronousEnhancer.enhanceClass(String)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        "test".toUpperCase() {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'TEST', result.get()

        shouldFail (MissingMethodException) {
            'test'.nonExistantMethod()
        }

        shouldFail (MissingMethodException) {
            'test'.nonExistantMethod {}
        }

        AsynchronousEnhancer.unenhanceClass(String)

        shouldFail(MissingMethodException) {
            'anotherTest'.toUpperCase() {
                println(it)
            }
        }
    }

    public void testPoolEnhancement() {
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceClass(String, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            "test".toUpperCase() {
                result.set(it)
                latch.countDown()
            }

            latch.await(30, TimeUnit.SECONDS)
            assertEquals 'TEST', result.get()

            shouldFail (MissingMethodException) {
                'test'.nonExistantMethod()
            }

            shouldFail (MissingMethodException) {
                'test'.nonExistantMethod {}
            }

            AsynchronousEnhancer.unenhanceClass(String)

            shouldFail(MissingMethodException) {
                'anotherTest'.toUpperCase() {
                    println(it)
                }
            }
        }
    }

    public void testPropertyEnhancement() {
        AsynchronousEnhancer.enhanceClass(String)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        "test".getBytes {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'test'.bytes, result.get()

        assertEquals 4, 'test'.size()

        shouldFail (MissingPropertyException) {
            'test'.nonExistantProperty
        }

        shouldFail (MissingMethodException) {
            'test'.nonExistantProperty {}
        }

        AsynchronousEnhancer.unenhanceClass(String)

        shouldFail(MissingMethodException) {
            'anotherTest'.bytes(1, 2) {
                println(it)
            }
        }
    }

    public void testPropertyPoolEnhancement() {
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceClass(String, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            "test".getBytes {
                result.set(it)
                latch.countDown()
            }

            latch.await(30, TimeUnit.SECONDS)
            assertEquals 'test'.bytes, result.get()

            assertEquals 4, 'test'.size()

            shouldFail (MissingPropertyException) {
                'test'.nonExistantProperty
            }

            shouldFail (MissingMethodException) {
                'test'.nonExistantProperty {}
            }

            AsynchronousEnhancer.unenhanceClass(String)

            shouldFail(MissingMethodException) {
                'anotherTest'.bytes(1, 2) {
                    println(it)
                }
            }
        }
    }

    public void testUnenhancement() {
        AsynchronousEnhancer.enhanceClass(String)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        "test".toUpperCase() { }

        AsynchronousEnhancer.unenhanceClass(String)

        "test".toUpperCase() {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'TEST', result.get()

        shouldFail(MissingMethodException) {
            'anotherTest'.toUpperCase() {
                println(it)
            }
        }
    }

    public void testPoolUnenhancement() {
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceClass(String, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            "test".toUpperCase() { }

            AsynchronousEnhancer.unenhanceClass(String)

            "test".toUpperCase() {
                result.set(it)
                latch.countDown()
            }

            shouldFail(MissingMethodException) {
                'anotherTest'.toUpperCase() {
                    println(it)
                }
            }
        }
    }
}