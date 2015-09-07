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

package groovyx.gpars.integration.remote.dataflow

import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.DataflowVariable
import groovyx.gpars.dataflow.remote.RemoteDataflowQueue
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RemoteDataflowsDataflowQueueWithServerTest extends RemoteSpecification {
    def static PORT = 9031

    @Shared
    RemoteDataflows serverRemoteDataflows

    @Shared
    RemoteDataflows clientRemoteDataflows

    def setupSpec() {
        serverRemoteDataflows = RemoteDataflows.create()
        serverRemoteDataflows.startServer getHostAddress(), PORT

        clientRemoteDataflows = RemoteDataflows.create()
    }

    def cleanupSpec() {
        serverRemoteDataflows.stopServer()
    }

    RemoteDataflowQueue publishNewQueueAndGetRemotely(DataflowQueue queue, String queueName) {
        serverRemoteDataflows.publish queue, queueName
        RemoteDataflowQueue remoteQueue = null
        def received = false
        while (!received) {
            try {
                remoteQueue = clientRemoteDataflows.getDataflowQueue getHostAddress(), PORT, queueName get(10, TimeUnit.MILLISECONDS)
                received = true
            } catch (TimeoutException e) { }
        }
        remoteQueue
    }

    @Timeout(5)
    def "can retrieve published DataflowQueue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-0"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

        then:
        remoteQueue != null
    }

    @Timeout(5)
    def "can get item from queue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-1"
        def testValue = "test-value"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

        queue << testValue
        def receivedTestValue = remoteQueue.val

        then:
        receivedTestValue == testValue
    }

    @Timeout(5)
    def "can put item to queue"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-2"
        def testValue = "test-value"

        when:
        def remoteQueue = publishNewQueueAndGetRemotely(queue, queueName)

        remoteQueue << testValue
        def receivedTestValue = queue.val

        then:
        receivedTestValue == testValue
    }

    @Timeout(5)
    def "test if not bounded DataflowVariable can be pushed into DataflowQueue and become bounded later"() {
        setup:
        def queue = new DataflowQueue()
        def queueName = "test-queue-3"
        def remoteQueue = publishNewQueueAndGetRemotely queue, queueName
        def testVariable = new DataflowVariable()

        when:
        remoteQueue << testVariable

        def resultVariable = new DataflowVariable()
        queue.whenBound {
            resultVariable << it
        }

        testVariable << queueName


        then:
        resultVariable.val == queueName
    }
}
