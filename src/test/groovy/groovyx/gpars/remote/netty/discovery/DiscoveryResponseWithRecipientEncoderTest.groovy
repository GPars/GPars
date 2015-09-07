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

package groovyx.gpars.remote.netty.discovery

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import spock.lang.Specification

class DiscoveryResponseWithRecipientEncoderTest extends Specification {
    def "Encode"() {
        setup:
        def encoder = new DiscoveryResponseWithRecipientEncoder()
        def channelHandlerContext = Mock(ChannelHandlerContext)
        def actorUrl = "test-group/test-actor"
        def response = createDiscoveryResponseWithRecipient(actorUrl)
        def resultList = []

        when:
        encoder.encode channelHandlerContext, response, resultList

        then:
        resultList.size() == 1

        def packet = (resultList.get(0)) as DatagramPacket
        packet != null
    }

    DiscoveryResponseWithRecipient createDiscoveryResponseWithRecipient(String actorUrl) {
        def inetSocketAddress = new InetSocketAddress(1234)
        new DiscoveryResponseWithRecipient(new DiscoveryResponse(actorUrl, inetSocketAddress), inetSocketAddress)
    }
}
