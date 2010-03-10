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

package c17.flagged

import org.jcsp.groovy.*
import org.jcsp.lang.*

class Sampler implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel
    def ChannelInput sampleRequest

    void run() {
        def sampleAlt = new ALT([sampleRequest, inChannel])
        while (true) {
            def index = sampleAlt.priSelect()
            if (index == 0) {
                sampleRequest.read()
                def v = inChannel.read()
                def fv = new FlaggedSystemData(a: v.a, b: v.b, testFlag: true)
                outChannel.write(fv)
            }
            else {
                outChannel.write(inChannel.read())
            }
        }
    }
}