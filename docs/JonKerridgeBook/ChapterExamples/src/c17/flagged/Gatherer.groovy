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

class Gatherer implements CSProcess {

    def ChannelInput inChannel
    def ChannelOutput outChannel
    def ChannelOutput gatheredData

    void run() {
        while (true) {
            def v = inChannel.read()
            if (v instanceof FlaggedSystemData) {
                def s = new SystemData(a: v.a, b: v.b, c: v.c)
                outChannel.write(s)
                gatheredData.write(v)
            }
            else {
                outChannel.write(v)
            }
        }
    }
}