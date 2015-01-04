// GPars - Groovy Parallel Systems
//
// Copyright © 2014 The original author or authors
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

package groovyx.gpars.remote.netty

import groovyx.gpars.remote.LocalHost
import spock.lang.Specification
import spock.lang.Timeout

import java.util.concurrent.CountDownLatch

class NettyClientServerTest extends Specification {
    def static PORT = 9003

    @Timeout(5)
    def "test connection locally"() {
        setup:
        def serverConnected = false
        def clientConnected = false
        def connectedLatch = new CountDownLatch(2)

        LocalHost localHost = null
        NettyServer server = new NettyServer(localHost, getHostAddress(), PORT, { serverConnected = true; connectedLatch.countDown() } as ConnectListener)
        NettyClient client = new NettyClient(localHost, getHostAddress(), PORT, { clientConnected = true; connectedLatch.countDown() } as ConnectListener)

        when:
        server.start()
        server.channelFuture.sync()
        client.start()
        client.channelFuture.sync()

        then:
        client.channelFuture.isSuccess()
        connectedLatch.await()
        serverConnected
        clientConnected

        client.stop()
        server.stop()
    }

    String getHostAddress() {
        InetAddress.getLocalHost().getHostAddress()
    }
}
