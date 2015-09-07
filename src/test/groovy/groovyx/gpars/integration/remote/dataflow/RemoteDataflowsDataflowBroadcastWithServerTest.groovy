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

import groovyx.gpars.dataflow.DataflowBroadcast
import groovyx.gpars.dataflow.DataflowReadChannel
import groovyx.gpars.dataflow.remote.RemoteDataflows
import groovyx.gpars.integration.remote.RemoteSpecification
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Timeout

class RemoteDataflowsDataflowBroadcastWithServerTest extends RemoteSpecification {
    def static PORT = 9177

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

    DataflowReadChannel publishNewBroadcastAndGetRemotely(DataflowBroadcast broadcast, String broadcastName) {
        serverRemoteDataflows.publish broadcast, broadcastName
        sleep 250
        clientRemoteDataflows.getReadChannel getHostAddress(), PORT, broadcastName get()
    }

    @Timeout(5)
    def "can retrieve published DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-0"

        when:
        def remoteChannel = publishNewBroadcastAndGetRemotely broadcast, broadcastName

        then:
        remoteChannel != null
    }

    @Timeout(5)
    def "can get item from DataflowBroadcast"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-1"
        def testValue = "test-value"

        when:
        def remoteChannel = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        broadcast << testValue

        def receivedTestValue = remoteChannel.val

        then:
        receivedTestValue == testValue
    }

    @Timeout(5)
    def "can get the same item from DataflowBroadcast with local and remote read channels"() {
        setup:
        DataflowBroadcast broadcast = new DataflowBroadcast()
        def broadcastName = "test-broadcast-2"
        def testValue = "test-value"

        when:
        def remoteChannel1 = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        def remoteChannel2 = publishNewBroadcastAndGetRemotely broadcast, broadcastName
        def localChannel = broadcast.createReadChannel()

        broadcast << testValue

        then:
        remoteChannel1 != remoteChannel2
        [remoteChannel1, remoteChannel2, localChannel].collect { it.val } every { it == testValue }
    }
}
