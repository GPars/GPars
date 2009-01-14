package org.gparallelizer.enhancer

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 *
 * @author Vaclav Pech
 * Date: Jan 14, 2009
 */

public class AsynchronousEnhancerExceptionTest extends GroovyTestCase {

    protected void tearDown() {
        super.tearDown();
        AsynchronousEnhancer.unenhanceClass(Validator)
    }

    public void testEnhancementWithMethod() {
        AsynchronousEnhancer.enhanceClass(Validator)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        new Validator().validate() {
            result.set(it)
            latch.countDown()
        }

        latch.await 30, TimeUnit.SECONDS
        assert result.get() instanceof Exception
    }

    public void testEnhancementWithProperty() {
        AsynchronousEnhancer.enhanceClass(Validator)

        final AtomicReference result = new AtomicReference()
        final CountDownLatch latch = new CountDownLatch(1)

        new Validator().getValue {
            result.set(it)
            latch.countDown()
        }

        latch.await(30, TimeUnit.SECONDS)
        assert result.get() instanceof Exception
    }
}

class Validator {
    String getValue() {
        throw new RuntimeException('test')
    }

    Object validate() {
        throw new RuntimeException('test')
    }
}
