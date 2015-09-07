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

package groovyx.gpars.integration.remote

import com.google.common.net.InetAddresses
import groovyx.gpars.remote.RemotingContextWithUrls
import groovyx.gpars.remote.netty.discovery.DiscoveryClient
import groovyx.gpars.remote.netty.discovery.DiscoveryServer
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class DiscoveryServiceTest extends Specification {
    @Timeout(5)
    def "can use discovery mechanism"() {
        setup:
        def serverSocketAddress = new InetSocketAddress(InetAddresses.forString("1.2.3.4"), 1234)
        def remotingContext = { true } as RemotingContextWithUrls
        def discoveryServer = new DiscoveryServer(11345, serverSocketAddress, remotingContext)
        discoveryServer.start()
        def discoveryClient = new DiscoveryClient(11345)
        discoveryClient.start()


        when:
        def receivedServerSocketAddress = tryGetServerSocketAddress discoveryClient, "test-actor"

        then:
        receivedServerSocketAddress.getPort() == 1234

        cleanup:
        [discoveryServer, discoveryClient]*.stop()
    }

    SocketAddress tryGetServerSocketAddress(DiscoveryClient client, String actorUrl) {
        SocketAddress address = null
        def received = false
        while (!received) {
            try {
                println "ask"
                address = client.ask actorUrl get 10, TimeUnit.MILLISECONDS
                received = true
            } catch (TimeoutException e) {}
        }
        address
    }
}
