package org.gparallelizer.enhancer

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by IntelliJ IDEA.
 * User: vaclav
 * Date: Jan 14, 2009
 */

public class AsynchronousEnhancerInstanceExceptionTest extends GroovyTestCase {

    public void testEnhancementWithMethod1() {
    }
    
    public void _testEnhancementWithMethod() {
        Validator validator = new Validator()
        AsynchronousEnhancer.enhanceInstance(validator)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        validator.validate() {
            result.set(it)
            latch.countDown()
        }

        latch.await 30, TimeUnit.SECONDS
        assert result.get() instanceof Exception
    }

    public void _testEnhancementWithProperty() {
        Validator validator = new Validator()
        AsynchronousEnhancer.enhanceInstance(validator)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        validator.getValue {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assert result.get() instanceof Exception
    }
}