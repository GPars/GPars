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

class NettyClientTest extends GroovyTestCase implements NettyTest {
    public void testClientStartNoServer() {
        NettyClient client = new NettyClient(null, LOCALHOST_ADDRESS, LOCALHOST_PORT)
        client.start()

        shouldFail(ConnectException.class, {
            client.channelFuture.sync()
        })

        client.stop()
    }

    public void testClientStart() {

    }

    public void testClientCannotBeStoppedIfNotRunning() {
        NettyClient client = new NettyClient(null, LOCALHOST_ADDRESS, LOCALHOST_PORT)

        def message = shouldFail(IllegalStateException.class, {
            client.stop()
        })

        assert message == "Client has not been started"
    }
}
