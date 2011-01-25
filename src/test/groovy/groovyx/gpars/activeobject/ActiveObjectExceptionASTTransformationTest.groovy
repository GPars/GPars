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
}

@ActiveObject
class MyExceptionWrapper {
    def result = new DataFlowVariable()

    private def bar() {
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
