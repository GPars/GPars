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

package groovyx.gpars.dataflow

import groovyx.gpars.group.NonDaemonPGroup

import java.security.AccessControlException

/**
 * @author Vaclav Pech
 */
class ErrorHandlerTest extends GroovyTestCase {
    public void testBasicInvocation() {
        def df = new DataflowVariable()
        final result = df.then({ 10 }, { 20 })

        df.bind(new RuntimeException('test'))
        assert 20 == result.get()
    }

    public void testBasicErrorInvocation() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ 10 }, { 20 })

        df.bindError(new RuntimeException('test'))
        assert 20 == result.get()
    }

    public void testDefaultErrorHandler() {
        def df = new DataflowVariable()
        final result = df.then({ 10 })

        df.bind(new RuntimeException('test'))
        shouldFail(RuntimeException) {
            result.get()
        }
    }

    public void testDefaultErrorHandlerWithErrorInvocation() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ 10 })

        df.bindError(new RuntimeException('test'))
        shouldFail(RuntimeException) {
            result.get()
        }
    }

    public void testErrorHandlerChaining() {
        def df = new DataflowVariable()
        final result = df.then({ throw new RuntimeException('test') }).then({ it + 10 }).then({ it - 3 })

        df.bind(10)
        shouldFail(RuntimeException) {
            result.get()
        }
    }

    public void testErrorHandlerChainingWithErrorInvocation() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ throw new RuntimeException('test') }).then({ it + 10 }).then({ it - 3 })

        df.bind(10)
        shouldFail(RuntimeException) {
            result.get()
        }
    }

    public void testErrorMasking() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ throw new RuntimeException('test') }).then({ it + 10 }).then({ it - 3 }, { 0 })
        df.bind(10)
        assert 0 == result.get()
    }

    public void testErrorMaskingWithDifferentTypeOfHandledException() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ throw new RuntimeException('test') }).then({ it + 10 }, { IllegalStateException e -> println "should not handle this type of errors"; 100 })
                .then({ it - 3 }, { 0 })
        df.bind(10)
        assert 0 == result.get()
    }

    public void testErrorMaskingWithSuperTypeOfHandledException() {
        def df = new DataflowVariable<Integer>()
        final result = df.then({ throw new AccessControlException('test') }).then({ it + 10 }, { SecurityException e -> 100 })
                .then({ it - 3 }, { 0 })
        df.bind(10)
        assert 97 == result.get()
    }

    public void testWhenAllBoundBasicInvocation() {
        def df = new DataflowVariable<Integer>()
        def group = new NonDaemonPGroup()
        final result = group.whenAllBound([df], { 10 }, { -20 })

        df.bindError(new RuntimeException('test'))
        assert -20 == result.get()
    }

    public void testWhenAllBoundWithMultipleInputs() {
        def df1 = new DataflowVariable<Integer>()
        def df2 = new DataflowVariable<Integer>()
        def df3 = new DataflowVariable<Integer>()
        final result = Dataflow.whenAllBound([df1, df2, df3], { a, b, c -> 10 }, { -20 })

        df1.bind(30)
        df2.bindError(new RuntimeException('test'))
        assert -20 == result.get()
    }

    public void testWhenAllBoundWithMultipleInputsAndDefaultErrorHandler() {
        def df1 = new DataflowVariable<Integer>()
        def df2 = new DataflowVariable()
        def df3 = new DataflowVariable<Integer>()
        final result = Dataflow.whenAllBound([df1, df2, df3], { a, b, c -> 10 })

        df1.bind(30)
        df2.bind(new RuntimeException('test'))
        shouldFail(RuntimeException) {
            result.get()
        }
    }

    public void testWhenAllBoundWithMultipleInputsAndDefaultErrorHandlerAndErrorInvocation() {
        def df1 = new DataflowVariable<Integer>()
        def df2 = new DataflowVariable<Integer>()
        def df3 = new DataflowVariable<Integer>()
        final result = Dataflow.whenAllBound([df1, df2, df3], { a, b, c -> 10 })

        df1.bind(30)
        df2.bindError(new RuntimeException('test'))
        shouldFail(RuntimeException) {
            result.get()
        }
    }
}
