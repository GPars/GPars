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

package c3

import org.jcsp.lang.*
import org.jcsp.groovy.*
import org.jcsp.groovy.plugAndPlay.*

class DifferentiateNeg implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel

    void run() {

        One2OneChannel a = Channel.createOne2One()
        One2OneChannel b = Channel.createOne2One()
        One2OneChannel c = Channel.createOne2One()
        One2OneChannel d = Channel.createOne2One()

        def differentiateList = [new GPrefix(prefixValue: 0,
                inChannel: b.in(),
                outChannel: c.out()),
                new GPCopy(inChannel: inChannel,
                        outChannel0: a.out(),
                        outChannel1: b.out()),
                new Negator(inChannel: c.in(), outChannel: d.out()),
                new GPlus(inChannel0: a.in(),
                        inChannel1: d.in(),
                        outChannel: outChannel)
        ]

        new PAR(differentiateList).run()

    }
}
