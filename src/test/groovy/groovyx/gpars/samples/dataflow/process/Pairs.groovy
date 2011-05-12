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

package groovyx.gpars.samples.dataflow.process

import groovyx.gpars.dataflow.Dataflow
import groovyx.gpars.dataflow.DataflowChannel
import groovyx.gpars.dataflow.DataflowQueue
import java.util.concurrent.Callable

final class Pairs implements Callable {
    private final DataflowChannel inChannel
    private final DataflowChannel outChannel

    def Pairs(final inChannel, final outChannel) {
        this.inChannel = inChannel;
        this.outChannel = outChannel;
    }

    public def call() {
        def a = new DataflowQueue()
        def b = new DataflowQueue()
        def c = new DataflowQueue();

        def group = Dataflow.retrieveCurrentDFPGroup()
        [
                new Plus(a, c, outChannel),
                new Copy(inChannel, a, b),
                new Tail(b, c)
        ].each {group.task it}
    }
}
