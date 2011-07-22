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

package groovyx.gpars.dataflow.stream

import static groovyx.gpars.dataflow.Dataflow.task

class SyncDataflowStreamTest extends GroovyTestCase {

    def stream = new SyncDataflowStream(2)

    public void testDecreasingPartiesBelowZero() {
        shouldFail(IllegalArgumentException) {
            new SyncDataflowStream(0).decrementParties()
        }

        final SyncDataflowStream stream = new SyncDataflowStream(1)
        stream.decrementParties()
        shouldFail(IllegalArgumentException) {
            stream.decrementParties()
        }
        stream.incrementParties()
        stream.decrementParties()
        shouldFail(IllegalArgumentException) {
            stream.decrementParties()
        }
    }

    void testEmptyStream() {
        task {
            stream << SyncDataflowStream.eos()
        }
        task {
            stream.first
        }
        stream.first
        assert stream.isEmpty()
    }

    void testStreamWithOneObject() {
        stream.decrementParties()
        task {
            stream << "first"
        }
        assert stream.first == "first"
        assert !stream.isEmpty()
    }

    void testStreamWithSeveralObjects() {
        stream.decrementParties()
        task {
            stream << "first" << "second" << "third" << SyncDataflowStream.eos();
        }
        assert stream.first == "first"
        assert stream.rest.first == "second"
        assert stream.rest.rest.first == "third"
        assert stream.rest.rest.rest.isEmpty()
    }
}