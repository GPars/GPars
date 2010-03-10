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

package c17.counted

import org.jcsp.groovy.*
import org.jcsp.lang.*

class CountingSampler implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel
    def ChannelInput sampleRequest
    def ChannelOutput countReturn

    void run() {
        def sampleAlt = new ALT([sampleRequest, inChannel])
        def counter = 0
        while (true) {
            counter = counter + 1
            def index = sampleAlt.priSelect()
            if (index == 0) {
                sampleRequest.read()
                def v = inChannel.read()
                outChannel.write(v)
                println "Sampled Value was ${v}"
                countReturn.write(counter)
            }
            else {
                outChannel.write(inChannel.read())
            }
        }
    }
}