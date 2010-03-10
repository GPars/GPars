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

package c7

import org.jcsp.lang.*
import org.jcsp.groovy.*


class Client implements CSProcess {

    def ChannelInput receiveChannel
    def ChannelOutput requestChannel
    def clientNumber

    def selectList = []

    void run() {
        def iterations = selectList.size
        println "Client $clientNumber has $iterations values in $selectList"
        for (i in 0..<iterations) {
            def key = selectList[i]
            requestChannel.write(key)
            def v = receiveChannel.read()
            println "Client $clientNumber: with $key has value $v"
        }
        println "Client $clientNumber has finished"
    }
}
