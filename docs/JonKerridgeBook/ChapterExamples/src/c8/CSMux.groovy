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

package c8

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CSMux implements CSProcess {
    def ChannelInputList inClientChannels
    def ChannelOutputList outClientChannels
    def ChannelInputList fromServers
    def ChannelOutputList toServers
    def serverAllocation = []    // list of lists of keys contained in each server
    void run() {
        def servers = toServers.size()
        def muxAlt = new ALT(inClientChannels)
        while (true) {
            def index = muxAlt.select()
            def key = inClientChannels[index].read()
            def server = -1
            for (i in 0..<servers) {
                if (serverAllocation[i].contains(key)) {
                    server = i
                    break
                }
            }
            toServers[server].write(key)
            def value = fromServers[server].read()
            outClientChannels[index].write(value)
        }
    }
}