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
import org.jcsp.groovy.plugAndPlay.*

class Manager1Only implements CSProcess {

    def ChannelInputList inputs
    def ChannelOutputList outputs
    def ChannelInput fromBlender
    def ChannelOutput toBlender
    def int hoppers = 3

    void run() {
        def alt = new ALT(inputs)
        while (true) {
            // read 1 from one of the three hoppers
            def ah = alt.select()
            inputs[ah].read()
            println "Accepted Hopper is $ah"
            // now read 1 from the blender
            fromBlender.read()
            // now send a response to the accepted hopper and the blender
            outputs[ah].write(1)
            toBlender.write(1)
            // now read terminating 2 from blender
            fromBlender.read()
            // and the terminating 2 from the accepted hopper
            inputs[ah].read()
            // now send a response to the hopper and the blender
            outputs[ah].write(1)
            toBlender.write(1)
        }
    }
}
