package org.gparallelizer.enhancer

import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.gparallelizer.Asynchronizer

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 14, 2009
 */

public class AsynchronousEnhancerInstanceTest extends GroovyTestCase {

    public void testEnhancement() {
        def instance = 'test'
        AsynchronousEnhancer.enhanceInstance(instance)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        instance.toUpperCase() {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'TEST', result.get()

        shouldFail(MissingMethodException) {
            instance.nonExistantMethod()
        }

        shouldFail(MissingMethodException) {
            instance.nonExistantMethod {}
        }

        shouldFail(MissingMethodException) {
            'anotherTest'.toUpperCase() {}
        }
    }

    public void testPoolEnhancement() {
        def instance = 'test'
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceInstance(instance, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            instance.toUpperCase() {
                result.set(it)
                latch.countDown()
            }

            latch.await(30, TimeUnit.SECONDS)
            assertEquals 'TEST', result.get()

            shouldFail(MissingMethodException) {
                instance.nonExistantMethod()
            }

            shouldFail(MissingMethodException) {
                instance.nonExistantMethod {}
            }

            shouldFail(MissingMethodException) {
                'anotherTest'.toUpperCase() {}
            }
        }
    }

    public void testPropertyEnhancement() {
        def instance = 'test'
        AsynchronousEnhancer.enhanceInstance(instance)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        instance.getBytes {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assertEquals 'test'.bytes, result.get()

        assertEquals 4, instance.size()

        shouldFail(MissingPropertyException) {
            instance.nonExistantProperty
        }

        shouldFail(MissingMethodException) {
            instance.nonExistantProperty {}
        }

        shouldFail(MissingMethodException) {
            'anotherTest'.getBytes() {}
        }
    }

    public void testPropertyPoolEnhancement() {
        def instance = 'test'
        Asynchronizer.withAsynchronizer {ExecutorService pool ->
            AsynchronousEnhancer.enhanceInstance(instance, pool)

            final AtomicReference result = new AtomicReference()
            final CountDownLatch latch = new CountDownLatch(1)

            instance.getBytes {
                result.set(it)
                latch.countDown()
            }

            latch.await(30, TimeUnit.SECONDS)
            assertEquals 'test'.bytes, result.get()

            assertEquals 4, instance.size()

            shouldFail(MissingPropertyException) {
                instance.nonExistantProperty
            }

            shouldFail(MissingMethodException) {
                instance.nonExistantProperty {}
            }

            shouldFail(MissingMethodException) {
                'anotherTest'.getBytes() {}
            }

            shouldFail(MissingMethodException) {
                instance.bytes(1, 2) {
                    println(it)
                }
            }
        }
    }
}