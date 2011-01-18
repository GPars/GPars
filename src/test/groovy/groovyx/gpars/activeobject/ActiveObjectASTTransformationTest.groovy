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
import org.codehaus.groovy.control.MultipleCompilationErrorsException

class ActiveObjectASTTransformationTest extends GroovyTestCase {
    //todo exception reporting - test
    //todo pass no arguments to the create() method
    //todo avoid actor field duplicates in hierarchies
    //todo ensure correct instance is being used with inheritance hierarchies - gaps, no gaps

    // todo test static methods, inheritance of active methods, correctness
    //todo inheritance with different actor field names allows for multiple actors per instance

    //todo pass in the group
    //todo finish all methods before exit


    //todo return a DFV
    //todo update GDSL



    public void testActorIsActive() {
        final actor = new MyWrapper().internalActiveObjectActor
        assert actor.active
    }

    public void testActorUniqueness() {
        final actor1 = new MyWrapper().internalActiveObjectActor
        final actor2 = new MyWrapper().internalActiveObjectActor
        assert actor1.active
        assert actor2.active
        assert !actor1.is(actor2)
    }

    public void testActorMessages() {
        final MyWrapper wrapper = new MyWrapper()
        final actor = wrapper.internalActiveObjectActor
        actor.send([wrapper, 'bar', 231])
        assert wrapper.result.val == 231
    }

    public void testActorBlockingMessages() {
        final MyWrapper wrapper = new MyWrapper()
        final actor = wrapper.internalActiveObjectActor
        assert 431 == actor.sendAndWait([wrapper, 'bar', 431])
    }

    public void testVoidMethod() {
        final MyWrapper wrapper = new MyWrapper()
        assertNull wrapper.baz(92)
        assert 92 == wrapper.result.val
    }

    public void testNonVoidMethod() {
        final MyWrapper wrapper = new MyWrapper()
        assert 12 == wrapper.bar(12)
        assert 12 == wrapper.result.val
    }

    public void testAlternativeActorName() {
        final MyAlternativeWrapper wrapper = new MyAlternativeWrapper()
        assert 30 == wrapper.foo(10, 20)
        assert 60 == wrapper.foo(40, 20)
        final actor = wrapper.alternativeActorName
        assert 7 == actor.sendAndWait([wrapper, 'foo', 4, 3])
    }

    public void testActiveObjectInheritance() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.*
@ActiveObject
class A {
    @ActiveMethod
    def foo() {
    }
}

@ActiveObject
class B extends A {
}

[new A(), new B()]
""")
        assert a.internalActiveObjectActor.active
        assert b.internalActiveObjectActor.active
    }

    public void testActiveObjectInheritanceWithReverseOrder() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.*

@ActiveObject
class B extends A {
}

@ActiveObject
class A {
    @ActiveMethod
    def foo() {
    }
}

[new A(), new B()]
""")
        assert a.internalActiveObjectActor.active
        assert b.internalActiveObjectActor.active
    }

    public void testActiveObjectHoldingCollidingFieldShouldFail() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            shell.evaluate("""
import groovyx.gpars.activeobject.*
    @ActiveObject
    class A {
        @ActiveMethod
        def foo() {
        }
    }

    @ActiveObject
    class B extends A {
        def internalActiveObjectActor = 20
    }
    """)
        }
    }
}
@ActiveObject
class MyWrapper {
    def result = new DataFlowVariable()

    def foo() {
        println 'Foo'
    }

    @ActiveMethod
    def bar(int value) {
        result.bind(value)
        value
    }

    @ActiveMethod
    void baz(int value) {
        result.bind(value)
    }
}

@ActiveObject("alternativeActorName")
class MyAlternativeWrapper {
    @ActiveMethod
    def foo(int a, int b) {
        a + b
    }
}
