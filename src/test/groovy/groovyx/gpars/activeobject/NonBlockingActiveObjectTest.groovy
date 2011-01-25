// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.activeobject

import groovyx.gpars.dataflow.DataFlowVariable

public class NonBlockingActiveObjectTest extends GroovyTestCase {

    public void testBlockingMethodBlocks() {
        final MyNonBlockingWrapper wrapper = new MyNonBlockingWrapper()
        assert 21 == wrapper.blockingFoo(20)
        assert wrapper.result.val != Thread.currentThread()
    }

    public void testNonBlockingMethod() {
        final MyNonBlockingWrapper wrapper = new MyNonBlockingWrapper()
        assert 19 == wrapper.nonBlockingFoo(20).val
        assert wrapper.result.val != Thread.currentThread()
    }

    public void testNonBlockingVoidMethod() {
        final MyNonBlockingWrapper wrapper = new MyNonBlockingWrapper()
        wrapper.nonBlockingVoidFoo(20)
        assert wrapper.result.val != Thread.currentThread()
    }

    public void testNonBlockingTypedMethod() {
        final MyNonBlockingWrapper wrapper = new MyNonBlockingWrapper()
        assert 19 == wrapper.nonBlockingTypedFoo(20).val
        assert wrapper.result.val != Thread.currentThread()
    }
}

@ActiveObject()
class MyNonBlockingWrapper {
    def result = new DataFlowVariable()

    @ActiveMethod
    public def nonBlockingFoo(value) {
        result << Thread.currentThread()
        value - 1
    }

    @ActiveMethod
    public void nonBlockingVoidFoo(value) {
        result << Thread.currentThread()
    }

    @ActiveMethod
    public def nonBlockingTypedFoo(value) {
        result << Thread.currentThread()
        value - 1
    }

    @ActiveMethod(blocking = true)
    public int blockingFoo(value) {
        result << Thread.currentThread()
        value + 1
    }
}
