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

public class DataflowVariableBindErrorTest extends GroovyTestCase {

    public void testRebindToDifferentValue() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Object oldValue, final Object failedValue, final boolean uniqueBind) {
                result.bind(oldValue)
                result.bind(failedValue)
                result.bind(uniqueBind)
            }

            @Override
            void onBindError(final Object oldValue, final Throwable failedError) {
                result.bind(0)  //Should be never invoked
            }
        })
        variable << 10
        assert 10 == variable.val

        shouldFail(IllegalStateException) {
            variable << 20
        }

        assert 10 == variable.val

        assert 10 == result.val
        assert 20 == result.val
        assert false == result.val
    }

    public void testRebindSafelyToDifferentValue() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Object oldValue, final Object failedValue, final boolean uniqueBind) {
                result.bind(oldValue)
                result.bind(failedValue)
                result.bind(uniqueBind)
            }

            @Override
            void onBindError(final Object oldValue, final Throwable failedError) {
                result.bind(0)  //Should be never invoked
            }
        })
        variable << 10
        assert 10 == variable.val

        variable.bindSafely(20)

        assert 10 == variable.val

        assert 10 == result.val
        assert 20 == result.val
        assert false == result.val
    }

    public void testRebindToSameValueUniquely() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Object oldValue, final Object failedValue, final boolean uniqueBind) {
                result.bind(oldValue)
                result.bind(failedValue)
                result.bind(uniqueBind)
            }

            @Override
            void onBindError(final Object oldValue, final Throwable failedError) {
                result.bind(0)  //Should be never invoked
            }
        })
        variable << 10
        assert 10 == variable.val

        shouldFail(IllegalStateException) {
            variable.bindUnique(10)
        }

        assert 10 == variable.val

        assert 10 == result.val
        assert 10 == result.val
        assert true == result.val
    }

    public void testRebindError() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Throwable throwable, final Object failedValue, final boolean uniqueBind) {
                result.bind(throwable)
                result.bind(failedValue)
                result.bind(uniqueBind)
            }

            @Override
            void onBindError(final Object oldValue, final Throwable failedError) {
                result.bind(0)  //Should be never invoked
            }
        })
        variable.bindError(new IllegalStateException('test'))
        assert null == variable.val

        shouldFail(IllegalStateException) {
            variable << 20
        }

        assert null == variable.val

        assert result.val instanceof IllegalStateException
        assert 20 == result.val
        assert false == result.val
    }

    public void testRebindToAnError() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Object oldValue, final Object failedValue, final boolean uniqueBind) {
                result.bind(0)  //Should be never invoked
            }

            @Override
            void onBindError(final Object oldValue, final Throwable failedError) {
                result.bind(oldValue)
                result.bind(failedError)
            }
        })
        variable << 10
        assert 10 == variable.val

        shouldFail(IllegalStateException) {
            variable.bindError(new RuntimeException('test'))
        }

        assert 10 == variable.val

        assert 10 == result.val
        assert result.val instanceof RuntimeException
    }

    public void testRebindErrorToAnError() {
        final DataflowVariable variable = new DataflowVariable()

        final DataflowQueue result = new DataflowQueue()

        variable.getBindErrorManager().addBindErrorListener(new BindErrorAdapter() {
            @Override
            void onBindError(final Object oldValue, final Object failedValue, final boolean uniqueBind) {
                result.bind(0)  //Should be never invoked
            }

            @Override
            void onBindError(final Throwable throwable, final Throwable failedError) {
                result.bind(throwable)
                result.bind(failedError)
            }
        })
        variable.bindError(new IllegalStateException('test'))
        assert null == variable.val

        shouldFail(IllegalStateException) {
            variable.bindError(new RuntimeException('test'))
        }

        assert null == variable.val

        assert result.val instanceof IllegalStateException
        assert result.val instanceof RuntimeException
    }


}
