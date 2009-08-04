package org.gparallelizer.enhancer

import org.gparallelizer.dataflow.DataFlowVariable

public class EnhancerTest extends GroovyTestCase {
    public void testClassEnhancement() {
        ActorMetaClass.enhanceClass TestProcessor
        final def processor = new TestProcessor()
        final def result = processor.process('test')
        assertEquals 'TEST', result
    }

    public void testClassEnhancementWithPooledActorMetaClass() {
        NonBlockingActorMetaClass.enhanceClass TestProcessor
        final def processor = new TestProcessor()
        final def result = processor.process('test')
        assert result instanceof DataFlowVariable
        assertEquals 'TEST', result.val
    }

}

class TestProcessor {
    public String process(String value) {
        return value.toUpperCase()
    }
}