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

package c9;

import org.jcsp.lang.*
import org.jcsp.groovy.*

class EventHandler implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel

    def void run() {

        One2OneChannel get = Channel.createOne2One()
        One2OneChannel transfer = Channel.createOne2One()
        One2OneChannel toBuffer = Channel.createOne2One()

        def handlerList = [new EventReceiver(eventIn: inChannel,
                eventOut: toBuffer.out()),
                new EventOWBuffer(inChannel: toBuffer.in(),
                        getChannel: get.in(), outChannel: transfer.out()),
                new EventPrompter(inChannel: transfer.in(),
                        getChannel: get.out(), outChannel: outChannel)
        ]
        new PAR(handlerList).run()
    }
}