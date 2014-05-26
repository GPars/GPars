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

class NettyClientTest extends GroovyTestCase {
    public void testClient() {
        // client connects to server
        NettyClient client = new NettyClient(NettyServerTest.ADDRESS_TO_BIND, 63607) // change port number!
        client.start()
        println "Client ${client.channel} active=${client.channel.isActive()}"

        // send message
        client.channel.writeAndFlush new TestSerialMsg()

        // stop client
        client.stop()
    }
}
