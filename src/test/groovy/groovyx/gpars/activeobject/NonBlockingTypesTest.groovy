// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

class NonBlockingTypesTest extends GroovyTestCase {
    public void testDefault() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    def foo() {
        return 10
    }
}

[new A()]
""")
        assert 10 == a.foo().get()
    }

    public void testObject() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    Object foo() {
        return 10
    }
}

[new A()]
""")
        assert 10 == a.foo().get()
    }

    public void testVoid() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    void foo() {
        return
    }
}

[new A()]
""")
        assert null == a.foo()
    }

    public void testDataflowVariable() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    DataflowVariable foo() {
        return 10
    }
}

[new A()]
""")
        assert 10 == a.foo().get()
    }

    public void testDataflowPromise() {
        final GroovyShell shell = new GroovyShell()
        def (a) = shell.evaluate("""
import groovyx.gpars.activeobject.ActiveObject
import groovyx.gpars.activeobject.ActiveMethod
import groovyx.gpars.dataflow.Promise
import groovyx.gpars.dataflow.DataflowVariable

@ActiveObject
class A {
    @ActiveMethod
    Promise<Integer> foo() {
        return 10
    }
}

[new A()]
""")
        assert 10 == a.foo().get()
    }

    public void testDataflowInteger() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            def (a) = shell.evaluate("""
    import groovyx.gpars.activeobject.ActiveObject
    import groovyx.gpars.activeobject.ActiveMethod
    import groovyx.gpars.dataflow.Promise
    import groovyx.gpars.dataflow.DataflowVariable

    @ActiveObject
    class A {
        @ActiveMethod
        Integer foo() {
            return 10
        }
    }

    [new A()]
    """)
        }
    }

    public void testDataflowInt() {
        final GroovyShell shell = new GroovyShell()
        shouldFail(MultipleCompilationErrorsException) {
            def (a) = shell.evaluate("""
    import groovyx.gpars.activeobject.ActiveObject
    import groovyx.gpars.activeobject.ActiveMethod
    import groovyx.gpars.dataflow.Promise
    import groovyx.gpars.dataflow.DataflowVariable

    @ActiveObject
    class A {
        @ActiveMethod
        int foo() {
            return 10
        }
    }

    [new A()]
    """)
        }
    }

}
