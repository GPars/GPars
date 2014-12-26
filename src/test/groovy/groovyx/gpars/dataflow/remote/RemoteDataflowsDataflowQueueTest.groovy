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

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import spock.lang.Specification

class RemoteDataflowsDataflowQueueTest extends Specification {
    RemoteDataflows remoteDataflows

    void setup() {
        remoteDataflows = RemoteDataflows.create()
    }

    def "retrieving not published DataflowQueue returns null"() {
        when:
        def queue = remoteDataflows.get DataflowQueue, "test-queue"

        then:
        queue == null
    }

    def "can publish DataflowQueue"() {
        setup:
        DataflowQueue<String> queue = new DataflowQueue<>()
        def queueName = "test-queue"

        when:
        remoteDataflows.publish queue, queueName
        def publishedQueue = remoteDataflows.get DataflowQueue, queueName

        then:
        publishedQueue == queue
    }

    def "retrieving DataflowQueue from remote host returns Future"() {
        setup:
        def PORT = 9030
        def queueName = "test-queue"

        when:
        def queuePromise = remoteDataflows.getDataflowQueue getHostAddress(), PORT, queueName

        then:
        queuePromise != null
        queuePromise instanceof DataflowVariable
    }

    def "retrieving DataflowQueue from remote host returns the same Promise"() {
        setup:
        def PORT = 9030
        def queueName = "test-queue"

        when:
        def queuePromise1 = remoteDataflows.getDataflowQueue getHostAddress(), PORT, queueName
        def queuePromise2 = remoteDataflows.getDataflowQueue getHostAddress(), PORT, queueName

        then:
        queuePromise1.is(queuePromise2)
    }

    String getHostAddress() {
        InetAddress.getLocalHost().getHostAddress()
    }
}
