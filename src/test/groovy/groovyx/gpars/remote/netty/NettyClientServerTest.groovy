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

class NettyClientServerTest extends GroovyTestCase implements NettyTest {
    static LocalHost LOCALHOST = new LocalHost()

    NettyServer server;
    NettyClient client;

    @Override
    void setUp() {
        super.setUp();

        server = new NettyServer(LOCALHOST, LOCALHOST_ADDRESS, LOCALHOST_PORT)
        server.start()
        server.channelFuture.sync()

        client = new NettyClient(LOCALHOST, LOCALHOST_ADDRESS, LOCALHOST_PORT)
        client.start()
        client.channelFuture.sync()
    }

    @Override
    void tearDown() {
        super.tearDown();

        client.stop()
        server.stop()
    }

    public void testConnectionLocal() {
        assert client.channelFuture.isSuccess()
    }
}
