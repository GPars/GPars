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

import com.google.common.net.InetAddresses
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import spock.lang.Specification

class DiscoveryIntegrationTest extends Specification {

    public static final String ACTOR_URL = "test-group/test-actor"
    public static final InetSocketAddress SERVER_SOCKET_ADDRESS =
            new InetSocketAddress(new InetAddresses().forString("192.168.1.123"), 1234)

    def "Encode and decode DiscoveryRequest"() {
        setup:
        def encoder = new DiscoveryRequestEncoder(1234)
        def decoder = new DiscoveryRequestDecoder()
        def channelHandlerContext = Mock(ChannelHandlerContext)
        def request = new DiscoveryRequest(ACTOR_URL)
        def encodedResultList = []
        def decodedResultList = []

        when:
        encoder.encode channelHandlerContext, request, encodedResultList
        decoder.decode channelHandlerContext, encodedResultList[0] as DatagramPacket, decodedResultList

        then:
        def decodedRequest = decodedResultList[0] as DiscoveryRequestWithSender
        decodedRequest.getRequest().getActorUrl() == ACTOR_URL
    }

    def "Encode and decode DiscoveryResponse"() {
        setup:
        def encoder = new DiscoveryResponseWithRecipientEncoder()
        def decoder = new DiscoveryResponseDecoder()
        def channelHandlerContext = Mock(ChannelHandlerContext)
        def responseWithRecipient = createResponseWithRecipient()
        def encodedResultList = []
        def decodedResultList = []

        when:
        encoder.encode channelHandlerContext, responseWithRecipient, encodedResultList
        def packet = encodedResultList[0] as DatagramPacket
        decoder.decode channelHandlerContext, new DatagramPacket(packet.content(), null, SERVER_SOCKET_ADDRESS), decodedResultList

        then:
        def decodedResponse = decodedResultList[0] as DiscoveryResponse
        decodedResponse.getActorUrl() == ACTOR_URL
        decodedResponse.getServerSocketAddress() == SERVER_SOCKET_ADDRESS
    }

    def createResponseWithRecipient() {
        def response = new DiscoveryResponse(ACTOR_URL, SERVER_SOCKET_ADDRESS)
        return new DiscoveryResponseWithRecipient(response, new InetSocketAddress(4321))
    }
}
