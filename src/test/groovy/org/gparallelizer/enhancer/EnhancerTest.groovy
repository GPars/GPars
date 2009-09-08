//  GParallelizer
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
