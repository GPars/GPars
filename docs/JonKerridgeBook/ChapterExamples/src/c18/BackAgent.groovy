// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
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

package c18

import org.jcsp.net.*
import org.jcsp.lang.*
import org.jcsp.net.cns.*
import org.jcsp.net.tcpip.*
import org.jcsp.groovy.*

class BackAgent implements MobileAgent {

    def ChannelOutput toLocal
    def ChannelInput fromLocal
    def NetChannelLocation backChannel
    def results = []

    def connect(List c) {
        this.toLocal = c[0]
        this.fromLocal = c[1]
    }

    def disconnect() {
        toLocal = null
        fromLocal = null
    }

    void run() {
        def toRoot = NetChannelEnd.createOne2Net(backChannel)
        toLocal.write(results)
        results = fromLocal.read()
        def last = results.size - 1
        toRoot.write(results[last])
        toRoot.destroyWriter()
    }

}