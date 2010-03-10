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

package c9

import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventOWBuffer implements CSProcess {

    def ChannelInput inChannel
    def ChannelInput getChannel
    def ChannelOutput outChannel

    def void run() {
        def owbAlt = new ALT([inChannel, getChannel])
        def INCHANNEL = 0
        def GETCHANNEL = 1
        def preCon = new boolean[2]
        preCon[INCHANNEL] = true
        preCon[GETCHANNEL] = false
        def e = new EventData()
        def missed = -1
        while (true) {
            def index = owbAlt.priSelect(preCon)
            switch (index) {
                case INCHANNEL:
                    e = inChannel.read().copy()
                    missed = missed + 1
                    e.missed = missed
                    preCon[GETCHANNEL] = true
                    break
                case GETCHANNEL:
                    def s = getChannel.read()
                    outChannel.write(e)
                    missed = -1                 // reset the missed count field
                    preCon[GETCHANNEL] = false
                    break
            }  // end switch
        }  // end while
    }  // end run
}