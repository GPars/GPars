// GPars - Groovy Parallel Systems
//
// Copyright © 2014  The original author or authors
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

package groovyx.gpars.dataflow.remote

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteDataflowsDataflowBroadcastTest extends Specification {
    RemoteDataflows remoteDataflows

    void setup() {
        remoteDataflows = RemoteDataflows.create()
    }

    def "retrieving not published DataflowBroadcast returns null"() {
        when:
        def stream = remoteDataflows.get DataflowBroadcast, "test-broadcast"

        then:
        stream == null
    }

    def "can publish DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcastStream = new DataflowBroadcast()
        def broadcastName = "test-broadcast"

        when:
        remoteDataflows.publish broadcastStream, broadcastName
        def publishedStream = remoteDataflows.get DataflowBroadcast, broadcastName

        then:
        publishedStream == broadcastStream
    }

    def "retrieving ReadChannel from remote host returns future"() {
        setup:
        def HOST = "dummy-host"
        def PORT = 9077 // dummy port
        def broadcastName = "test-broadcast"

        when:
        def streamFuture = remoteDataflows.getReadChannel HOST, PORT, broadcastName

        then:
        streamFuture != null
        streamFuture instanceof DataflowVariable
    }
}
