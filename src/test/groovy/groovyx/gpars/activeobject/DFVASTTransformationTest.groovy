// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2012  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.activeobject

import org.codehaus.groovy.control.MultipleCompilationErrorsException

public class DFVASTTransformationTest extends GroovyTestCase {

    public void testDFVReturningMethod() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
@ActiveObject
class A {
    def result = new DataflowVariable()

    @ActiveMethod
    DataflowVariable foo() {
        result << Thread.currentThread()
        new DataflowVariable() << 10
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

    public void testDFVReturningMethodAsynchronously() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
import java.util.concurrent.CyclicBarrier
@ActiveObject
class A {
    def result = new DataflowVariable()
    def barrier = new CyclicBarrier(2)

    @ActiveMethod
    def foo() {
        result << Thread.currentThread()
        barrier.await()
        10
    }
}

@ActiveObject
class B extends A {
}

[new A()]
""")
        def result = a.foo()
        assert !result.bound
        a.barrier.await()
        assert a.result.val != Thread.currentThread()
        assert result.val == 10
    }

    public void testDFVReturningMethodException() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
import java.util.concurrent.CyclicBarrier
@ActiveObject
class A {
    def result = new DataflowVariable()
    def barrier = new CyclicBarrier(2)

    @ActiveMethod
    def foo() {
        result << Thread.currentThread()
        barrier.await()
        throw new RuntimeException('test')
    }
}

@ActiveObject
class B extends A {
}

[new A()]
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

    public void testNonBlockingWithExplicitTypeIsNotAllowed() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            shell.evaluate("""
    import groovyx.gpars.activeobject.ActiveObject
    import groovyx.gpars.activeobject.ActiveMethod
    import groovyx.gpars.dataflow.DataflowVariable
    @ActiveObject
    class A {
        def result = new DataflowVariable()

        @ActiveMethod
        int foo() {
            10
        }
    }

    new A()
    """)
        }
    }
}



