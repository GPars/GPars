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

class NettyClientServerTest extends GroovyTestCase {

    public void testConnectionLocal() {
        // start server on localhost
        NettyServer server = new NettyServer('localhost')
        server.start()
        println "Server ${server.channel} active=${server.channel.isActive()}"
        def serverStarted = server.channel.isActive()
        def clientConnected = false

        if (serverStarted) {
            def serverPort = ((InetSocketAddress) server.channel.localAddress()).getPort()

            //client connects to server
            NettyClient client = new NettyClient('localhost', serverPort)
            client.start()
            println "Client ${client.channel} active=${client.channel.isActive()}"
            clientConnected = client.channel.isActive()

            // stop client
            client.stop()
        }
        // stop server
        server.stop()

        // client sends message
        assert serverStarted && clientConnected
    }

    public void testMessageLocal() {
        // start server on localhost
        NettyServer server = new NettyServer('localhost')
        server.start()
        println "Server ${server.channel} active=${server.channel.isActive()}"
        def serverStarted = server.channel.isActive()
        def clientConnected = false

        if (serverStarted) {
            def serverPort = ((InetSocketAddress) server.channel.localAddress()).getPort()

            //start client on localhost
            NettyClient client = new NettyClient('localhost', serverPort)

            // client connects to server
            client.start()
            println "Client ${client.channel} active=${client.channel.isActive()}"
            clientConnected = client.channel.isActive()

            client.channel.writeAndFlush("Hi, I'm client!").sync()

            // stop client
            client.stop()
        }
        // stop server
        server.stop()

        // client sends message
        assert serverStarted && clientConnected
    }

    public void testMultipleConnectionsLocal() {
        // start server on localhost
        NettyServer server = new NettyServer('localhost')
        server.start()
        println "Server ${server.channel} active=${server.channel.isActive()}"
        def serverStarted = server.channel.isActive()
        def client1Connected = false
        def client2Connected = false

        if (serverStarted) {
            def serverPort = ((InetSocketAddress) server.channel.localAddress()).getPort()

            //client1 connects to server
            NettyClient client1 = new NettyClient('localhost', serverPort)
            client1.start()
            println "Client ${client1.channel} active=${client1.channel.isActive()}"
            client1Connected = client1.channel.isActive()

            //client2 connects to server
            NettyClient client2 = new NettyClient('localhost', serverPort)
            client2.start()
            println "Client ${client2.channel} active=${client2.channel.isActive()}"
            client2Connected = client2.channel.isActive()

            // stop clients
            client1.stop()
            client2.stop()
        }
        // stop server
        server.stop()

        // client sends message
        assert serverStarted && client1Connected && client2Connected
    }
}
