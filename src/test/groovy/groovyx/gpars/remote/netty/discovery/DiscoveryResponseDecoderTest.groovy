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
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.util.CharsetUtil
import spock.lang.Specification

class DiscoveryResponseDecoderTest extends Specification {
    def "Decode"() {
        setup:
        def decoder = new DiscoveryResponseDecoder()
        def channelChandlerContext = Mock(ChannelHandlerContext)
        def actorUrl = "test-group/test-actor"
        def packet = createDatagramPacket(actorUrl)
        def resultList = []

        when:
        decoder.decode channelChandlerContext, packet, resultList

        then:
        resultList.size() == 1

        def response = (resultList.get(0)) as DiscoveryResponse
        response.getActorUrl() ==  actorUrl
        response.getServerSocketAddress().getPort() == 1234
    }

    DatagramPacket createDatagramPacket(String actorUrl) {
        def serverSocketAddress = new InetSocketAddress(InetAddresses.forString("192.168.1.123"), 1234)
        def portBuf = Unpooled.copyInt 1234
        def urlBuf = Unpooled.copiedBuffer actorUrl, CharsetUtil.UTF_8
        return new DatagramPacket(Unpooled.wrappedBuffer(portBuf, urlBuf), serverSocketAddress, serverSocketAddress)
    }
}
