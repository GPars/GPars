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
    public void testEnhancement1() {

    }
    public void _testEnhancement() {
        AsynchronousEnhancer.enhanceClass(String)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        "test".toUpperCase() {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'TEST', result.get()

        AsynchronousEnhancer.unenhanceClass(String)

        shouldFail(MissingMethodException) {
            'tests'.toUpperCase() {
                println(it)
            }
        }
    }

    public void _testPoolEnhancement() {
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

            AsynchronousEnhancer.unenhanceClass(String)

            shouldFail(MissingMethodException) {
                'tests'.toUpperCase() {
                    println(it)
                }
            }
        }
    }

    public void _testPropertEnhancement() {
        AsynchronousEnhancer.enhanceClass(String)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        "test".bytes {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'test'.bytes, result.get()

        assertEquals 4, 'test'.size()

        AsynchronousEnhancer.unenhanceClass(String)

        shouldFail(MissingMethodException) {
            'tests'.bytes(1, 2) {
                println(it)
            }
        }
    }

    public void _testPropertyPoolEnhancement() {
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceClass(String, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            "test".bytes {
                result.set(it)
                latch.countDown()
            }

            latch.await(30, TimeUnit.SECONDS)
            assertEquals 'test'.bytes, result.get()

            assertEquals 4, 'test'.size()

            AsynchronousEnhancer.unenhanceClass(String)

            shouldFail(MissingMethodException) {
                'tests'.bytes(1, 2) {
                    println(it)
                }
            }
        }
    }
}