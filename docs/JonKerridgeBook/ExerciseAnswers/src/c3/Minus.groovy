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
import org.jcsp.plugNplay.ProcessRead
import org.jcsp.groovy.*

class Minus implements CSProcess {

    def ChannelInput inChannel0
    def ChannelInput inChannel1
    def ChannelOutput outChannel

    void run() {

        ProcessRead read0 = new ProcessRead(inChannel0)
        ProcessRead read1 = new ProcessRead(inChannel1)
        def parRead2 = new PAR([read0, read1])

        while (true) {
            parRead2.run()
            outChannel.write(read0.value - read1.value)
        }
    }
}
            