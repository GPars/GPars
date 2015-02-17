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

package groovyx.gpars.remote

import spock.lang.Specification

class LocalHostTest extends Specification {

    def "test if start server can be executed only once"() {
        setup:
        def localHostMock = new LocalHostMock()

        when:
        localHostMock.startServer(getHostAddress(), 11223)
        localHostMock.startServer(getHostAddress(), 11224)

        then:
        thrown(IllegalStateException)
    }

    def "test if stop server cannot be executed if server is not started"() {
        setup:
        def localHostMock = new LocalHostMock()

        when:
        localHostMock.stopServer()

        then:
        thrown(IllegalStateException)
    }

    String getHostAddress() {
        InetAddress.getLocalHost().getHostAddress()
    }
}
