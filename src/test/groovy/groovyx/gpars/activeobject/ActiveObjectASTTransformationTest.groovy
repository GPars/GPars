// GPars - Groovy Parallel Systems
//
// Copyright © 2008-2013  The original author or authors
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

import groovyx.gpars.actor.Actors
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise
import org.codehaus.groovy.control.MultipleCompilationErrorsException

@SuppressWarnings("SpellCheckingInspection")
public class ActiveObjectASTTransformationTest extends GroovyTestCase {
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
        actor.send([[wrapper, 'bar', 231].toArray(), new DataflowVariable()].toArray())
        assert wrapper.result.val == 231
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
        final DataflowVariable result = new DataflowVariable()
        actor.send([[wrapper, 'foo', 4, 3].toArray(), result].toArray())
        assert 7 == result.get()
    }

    public void testActiveObjectInheritance() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

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
        assert a.internalActiveObjectActor != b.internalActiveObjectActor
    }

    public void testActiveObjectInheritanceWithReverseOrder() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

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
        assert a.internalActiveObjectActor != b.internalActiveObjectActor
    }

    public void testActiveObjectHoldingCollidingFieldShouldFail() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

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

    public void testStaticMethodsCannotBeActive() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

    import groovyx.gpars.dataflow.DataflowVariable
    @ActiveObject
    class A {
    static DataflowVariable result = new DataflowVariable()
        @ActiveMethod
        static def foo(value) {
        result << Thread.currentThread()
        }
    }
    new A()
    """)
        }
    }

    public void testActiveMethodInNonActiveSuperClassIsIgnored() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

import groovyx.gpars.dataflow.DataflowVariable
class A {
    def result = new DataflowVariable()
    @ActiveMethod
    def foo(value) {
        result << Thread.currentThread()
    }
}

@ActiveObject
class B extends A {
}

[new A(), new B()]
""")
        shouldFail(MissingPropertyException) {
            a.internalActiveObjectActor
        }
        assert b.internalActiveObjectActor.active
        a.foo(10)
        assert a.result instanceof Promise
        assert a.result.get() == Thread.currentThread()

        b.foo(20)
        assert b.result.val == Thread.currentThread()

    }

    public void testActiveMethodInNonActiveSubClassIsIgnored() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

import groovyx.gpars.dataflow.DataflowVariable
@ActiveObject
class A {
    def result = new DataflowVariable()
}

class B extends A {
    @ActiveMethod
    def foo(value) {
        result << Thread.currentThread()
    }
}

[new A(), new B()]
""")
        a.internalActiveObjectActor
        assert a.internalActiveObjectActor.active
        assert b.internalActiveObjectActor.active
        shouldFail(MissingMethodException) {
            a.foo(10)
        }

        b.foo(20)
        assert b.result.val == Thread.currentThread()
    }

    public void testOverridenNonActiveMethod() {
        final GroovyShell shell = new GroovyShell()
        def (a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

import groovyx.gpars.dataflow.DataflowVariable
class A {
    def result = new DataflowVariable()

    def foo(value) {
        result << Thread.currentThread()
    }
}

@ActiveObject
class B extends A {
    @ActiveMethod
    def foo(value) {
        def a = 'stuff'
        super.foo(value)
    }
}

[new A(), new B()]
""")
        shouldFail(MissingPropertyException) {
            a.internalActiveObjectActor
        }
        assert b.internalActiveObjectActor.active
        a.foo(10)
        assert a.result.val == Thread.currentThread()

        b.foo(20)
        assert b.result.val != Thread.currentThread()
    }

    public void testComplexInheritance() {
        final GroovyShell shell = new GroovyShell()
        def (a, b, c1, c2) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

import groovyx.gpars.dataflow.DataflowVariable
@ActiveObject
class A {
    def result = new DataflowVariable()
    @ActiveMethod
    def fooA(value) {
        result << Thread.currentThread()
    }
}

class B extends A {
}

@ActiveObject
class C extends B {
    @ActiveMethod
    def fooC(value1, value2) {
        result << Thread.currentThread()
    }
}
[new A(), new B(), new C(), new C()]
""")
        assert a.internalActiveObjectActor.active
        assert b.internalActiveObjectActor.active

        a.fooA(10)
        assert a.result.val != Thread.currentThread()

        b.fooA(20)
        assert b.result.val != Thread.currentThread()

        c1.fooA(30)
        assert c1.result.val != Thread.currentThread()

        c2.fooA(40)
        assert c2.result.val != Thread.currentThread()
    }

    public void testComplexInheritanceInDifferentOrder() {
        final GroovyShell shell = new GroovyShell()
        def (a, b, c1, c2) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
@ActiveObject
class C extends B {
    @ActiveMethod
    def fooC(value1, value2) {
        result << Thread.currentThread()
    }
}

class B extends A {
}

@ActiveObject
class A {
    def result = new DataflowVariable()
    @ActiveMethod
    def fooA(value) {
        result << Thread.currentThread()
    }
}

[new A(), new B(), new C(), new C()]
""")
        assert a.internalActiveObjectActor.active
        assert b.internalActiveObjectActor.active

        a.fooA(10)
        assert a.result.val != Thread.currentThread()

        b.fooA(20)
        assert b.result.val != Thread.currentThread()

        c1.fooA(30)
        assert c1.result.val != Thread.currentThread()

        c2.fooA(40)
        assert c2.result.val != Thread.currentThread()
    }

    public void testPolymorphism() {
        final GroovyShell shell = new GroovyShell()
        def decryptor = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod

@ActiveObject
class Decryptor {
    @ActiveMethod(blocking=true)
    String decrypt(String encryptedText) {

        return encryptedText.reverse()
    }

    @ActiveMethod(blocking=true)
    Integer decrypt(Integer encryptedNumber) {
        return -1*encryptedNumber
    }
}

new Decryptor()
""")
        assert 'dcba' == decryptor.decrypt('abcd')
        assert -10 == decryptor.decrypt(10)
    }

    public void testActiveMethodCallingNonActiveMethod() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowQueue
@ActiveObject
class A {
    def result = new DataflowQueue()
    def nonActiveFoo(value) {
        result << Thread.currentThread()
        return null
    }

    @ActiveMethod
    void activeFoo(value) {
        result << Thread.currentThread()
        nonActiveFoo(value)
    }
}
new A()
""")
        assert a.internalActiveObjectActor.active

        a.nonActiveFoo(10)
        assert a.result.val == Thread.currentThread()

        a.activeFoo(10)
        final t1 = a.result.val
        final t2 = a.result.val
        assert t1 != Thread.currentThread()
        assert t2 != Thread.currentThread()
        assert t1 == t2
    }

    public void testActiveMethodCallingActiveMethod() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowQueue
@ActiveObject
class A {
    def result = new DataflowQueue()
    @ActiveMethod(blocking=true)
    def activeFoo1(value) {
        result << Thread.currentThread()
        return null
    }

    @ActiveMethod(blocking=true)
    def activeFoo2(value) {
        result << Thread.currentThread()
        activeFoo1(value)
    }
}
new A()
""")
        assert a.internalActiveObjectActor.active

        a.activeFoo1(10)
        assert a.result.val != Thread.currentThread()

        a.activeFoo2(10)
        final t1 = a.result.val
        final t2 = a.result.val
        assert t1 != Thread.currentThread()
        assert t2 != Thread.currentThread()
        assert t1 == t2
    }

    public void testTwoActorFields() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            shell.evaluate("""
    import groovyx.gpars.activeobject.ActiveObject
    import groovyx.gpars.activeobject.ActiveMethod
    import groovyx.gpars.dataflow.DataflowVariable
    @ActiveObject(actorName = "fieldB")
    class C extends B {
        @ActiveMethod
        def fooC(value1, value2) {
            result << Thread.currentThread()
        }
    }

    class B extends A {
    }

    @ActiveObject(actorName = "fieldA")
    class A {
        def result = new DataflowVariable()
        @ActiveMethod
        def fooA(value) {
            result << Thread.currentThread()
        }
    }

    [new A(), new B(), new C()]
    """)
        }
    }

    public void testComposingDFVs() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    DataflowVariable foo() {
        new DataflowVariable() << 10
    }
}

new A()
""")
        assert 10 == a.foo().get()
        assert a.foo().get() instanceof Integer
        assert a.foo() instanceof Promise
    }

    public void testComposingDFVsWithDelayedBind() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

@ActiveObject
class A {
    @ActiveMethod
    Promise foo() {
        final DataflowVariable r = new DataflowVariable()
        Thread.start {
            sleep 1000
            r.bind(10)
        }
        return r
    }
}

new A()
""")
        assert 10 == a.foo().get()
        assert a.foo().get() instanceof Integer
        assert a.foo() instanceof Promise
    }

    public void testComposingDFVsWithoutExplicitReturnType() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    def foo() {
        new DataflowVariable() << 10
    }
}

new A()
""")
        assert 10 == a.foo().get()
        assert a.foo().get() instanceof Integer
        assert a.foo() instanceof Promise
    }

    public void testComposingDFVsWithDelayedBindAndWithoutExplicitReturnType() {
        final GroovyShell shell = new GroovyShell()
        def a = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise

@ActiveObject
class A {
    @ActiveMethod
    def foo() {
        final DataflowVariable r = new DataflowVariable()
        Thread.start {
            sleep 1000
            r.bind(10)
        }
        return r
    }
}

new A()
""")
        assert 10 == a.foo().get()
        assert a.foo().get() instanceof Integer
        assert a.foo() instanceof Promise
    }

    public void testGrabbingInternalActorsGroup() {
        final GroovyShell shell = new GroovyShell()
        def (group, a, b) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.group.NonDaemonPGroup
import groovyx.gpars.activeobject.ActiveObjectRegistry

@ActiveObject
class A {
    @ActiveMethod(blocking = true)
    def retrieveGroup() {
        internalActiveObjectActor.parallelGroup
    }
}

@ActiveObject("Custom")
class B {
    @ActiveMethod(blocking = true)
    def retrieveGroup() {
        internalActiveObjectActor.parallelGroup
    }
}

final g = new NonDaemonPGroup()
ActiveObjectRegistry.instance.register('Custom', g)
[g, new A(), new B()]
""")
        assert Actors.defaultActorPGroup == a.retrieveGroup()
        assert group == b.retrieveGroup()
    }

}

@ActiveObject
class MyWrapper {
    def result = new DataflowVariable()

    def foo() {
        println 'Foo'
    }

    @ActiveMethod(blocking = true)
    def bar(int value) {
        result.bind(value)
        value
    }

    @ActiveMethod
    void baz(int value) {
        result.bind(value)
    }
}

@ActiveObject(actorName = "alternativeActorName")
class MyAlternativeWrapper {
    @ActiveMethod(blocking = true)
    def foo(int a, int b) {
        a + b
    }
}
