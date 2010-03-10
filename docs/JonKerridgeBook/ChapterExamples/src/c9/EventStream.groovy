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

class EventStream implements CSProcess {

    def int source = 0
    def int initialValue = 0
    def int iterations = 10
    def ChannelOutput outChannel

    def void run() {
        def i = initialValue
        1.upto(iterations) {
            def e = new EventData(source: source, data: i)
            outChannel.write(e)
            i = i + 1
        }
        println "Source $source has finished"
    }
}

