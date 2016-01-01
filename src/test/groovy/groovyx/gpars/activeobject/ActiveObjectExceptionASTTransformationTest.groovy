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

import groovyx.gpars.dataflow.DataflowVariable

public class ActiveObjectExceptionASTTransformationTest extends GroovyTestCase {

    public void testCorrectMethod() {
        assert 10 == new MyExceptionWrapper().foo(10)
        assert 20 == new MyExceptionWrapper().foo(20)
    }

    public void testIncorrectMethod() {
        shouldFail(RuntimeException) {
            new MyExceptionWrapper().exceptionFoo(10)
        }
    }

    public void testIncorrectNonActiveMethod() {
        final MyExceptionWrapper a = new MyExceptionWrapper()
        shouldFail(RuntimeException) {
            a.exceptionBar(10)
        }
        assert a.result.val != Thread.currentThread()
    }

    public void testIncorrectAsynchronousMethod() {
        final MyExceptionWrapper a = new MyExceptionWrapper()
        shouldFail(RuntimeException) {
            a.exceptionBaz(10).get()
        }
        assert a.result.val != Thread.currentThread()
    }

    public void testExceptionRecovery() {
        final def a = new MyExceptionWrapperWithErrorHandler()

        def result = a.exceptionBaz(10).get()

        assert a.result.val != Thread.currentThread()
        assert result == MyExceptionWrapperWithErrorHandler.NEW_VALUE
        assert a.lastRecoveredMethod == 'exceptionBaz'
    }

    public void testMemoizedMethod() {
        final def a = new MyExceptionWrapperWithErrorHandler()

        def ignore = a.exceptionBaz(10).get()

        for (int i in 1..5)
            testExceptionRecovery()
    }

    public void testNoRecoveryForBlocking() {
        shouldFail(RuntimeException) {
            new MyExceptionWrapperWithErrorHandler().exceptionFoo(10)
        }
    }

    public void testNoProblemWithRecoveryError() {
        new MyExceptionWrapperWithErrorHandlerException().doThrow()
    }

    public void testNoProblemWithRecoveryError2() {
        shouldFail(Exception) {
            new MyExceptionWrapperWithErrorHandlerException().doReturnThrow().get()
        }
    }

    public void testFailSilently() {
        new MyExceptionWrapperWithoutHandler().doThrow()
    }

    public void testFailAndNotModifyException() {
        try {
            new MyExceptionWrapperWithoutHandler().doReturnThrow().get()
        }
        catch (def e) {
            assert e.message == 'proper message'
        }
    }
}

@ActiveObject
class MyExceptionWrapper {
    def result = new DataflowVariable()

    private static def bar() {
        throw new RuntimeException('test')
    }

    @ActiveMethod(blocking = true)
    def foo(value) {
        result << Thread.currentThread()
        value
    }

    @ActiveMethod(blocking = true)
    def exceptionFoo(value) {
        result << Thread.currentThread()
        throw new RuntimeException('test')
    }

    @ActiveMethod(blocking = true)
    def exceptionBar(value) {
        result << Thread.currentThread()
        bar()
    }

    @ActiveMethod
    void exceptionBaz(value) {
        result << Thread.currentThread()
        throw new RuntimeException('test')
    }
}

@ActiveObject
class MyExceptionWrapperWithErrorHandler implements ActorWithExceptionHandler {
    public static final String NEW_VALUE = "new value"

    def result = new DataflowVariable()
    def lastCurrentThread;
    def lastRecoveredMethod;

    @ActiveMethod(blocking = true)
    def exceptionFoo(value) {
        result << Thread.currentThread()
        throw new RuntimeException('test')
    }

    @ActiveMethod
    def exceptionBaz(value) {
        lastCurrentThread = Thread.currentThread()

        result << lastCurrentThread
        throw new RuntimeException('test')
    }

    @Override
    Object recoverFromException(String methodName, Exception e) {
        lastRecoveredMethod = methodName

        return NEW_VALUE
    }
}

@ActiveObject
class MyExceptionWrapperWithErrorHandlerException implements ActorWithExceptionHandler {
    @ActiveMethod
    void doThrow() {
        throw new Exception()
    }

    @ActiveMethod
    DataflowVariable doReturnThrow() {
        throw new Exception()
    }

    @Override
    Object recoverFromException(String methodName, Exception e) {
        throw new Exception(e)
    }
}
@ActiveObject
class MyExceptionWrapperWithoutHandler {
    @ActiveMethod
    void doThrow() {
        throw new Exception()
    }

    @ActiveMethod
    DataflowVariable doReturnThrow() {
        throw new Exception('proper message')
    }
}