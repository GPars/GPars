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

class DFVASTTransformationTest extends GroovyTestCase {

    public void testDFVReturningMethod() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.*
import groovyx.gpars.dataflow.DataFlowVariable
@ActiveObject
class A {
    def result = new DataFlowVariable()

    @ActiveMethod
    DataFlowVariable foo() {
        result << Thread.currentThread()
        new DataFlowVariable() << 10
    }
}

@ActiveObject
class B extends A {
}

[new A(), new B()]
""")
        def result = a.foo()
        assert a.result.val != Thread.currentThread()
        assert result.val == 10

        result = b.foo()
        assert b.result.val != Thread.currentThread()
        assert result.val == 10
    }

    public void testDFVReturningMethodAsynchronicity() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.*
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CyclicBarrier
@ActiveObject
class A {
    def result = new DataFlowVariable()
    def barrier = new CyclicBarrier(2)

    @ActiveMethod
    DataFlowVariable foo() {
        result << Thread.currentThread()
        barrier.await()
        new DataFlowVariable() << 10
    }
}

@ActiveObject
class B extends A {
}

[new A(), new B()]
""")
        def result = a.foo()
        assert !result.bound
        a.barrier.await()
        assert a.result.val != Thread.currentThread()
        assert result.val == 10
    }

    public void testDFVReturningMethodException() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.*
import groovyx.gpars.dataflow.DataFlowVariable
import java.util.concurrent.CyclicBarrier
@ActiveObject
class A {
    def result = new DataFlowVariable()
    def barrier = new CyclicBarrier(2)

    @ActiveMethod
    DataFlowVariable foo() {
        result << Thread.currentThread()
        barrier.await()
        throw new RuntimeException('test')
    }
}

@ActiveObject
class B extends A {
}

[new A(), new B()]
""")
        def result = a.foo()
        assert !result.bound
        a.barrier.await()
        assert a.result.val != Thread.currentThread()
        assert result.val instanceof RuntimeException
        assert result.val.message == 'test'
        shouldFail(RuntimeException) {
            result.get()
        }
        assert result.bound
    }
}



