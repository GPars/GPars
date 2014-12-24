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

import spock.lang.Specification
import spock.lang.Timeout

class NettyServerTest extends Specification {
    def static PORT = 9001

    @Timeout(5)
    def "test if NettyServer starts"() {
        setup:
        NettyServer server = new NettyServer(null, getHostAddress(), PORT, null)

        when:
        server.start()
        server.channelFuture.sync()

        then:
        server.channelFuture.isSuccess()

        server.stop()
    }

    def "test if stopping server fails when server is not running"() {
        setup:
        NettyServer server = new NettyServer(null, getHostAddress(), PORT, null)

        when:
        server.stop()

        then:
        IllegalStateException e = thrown()
        e.message == "Server has not been started"
    }

    @Timeout(5)
    def "test if only one server instance starts at given host:port"() {
        setup:
        def servers = [new NettyServer(null, getHostAddress(), PORT, null), new NettyServer(null, getHostAddress(), PORT, null)]

        when:
        servers*.start()
        servers*.channelFuture*.sync()

        then:
        thrown(BindException)

        servers*.stop()
    }

    String getHostAddress() {
        InetAddress.getLocalHost().getHostAddress()
    }
}