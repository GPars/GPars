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

package c12.fork

import org.jcsp.lang.*
import org.jcsp.groovy.*

class Fork implements CSProcess {

    def ChannelInput left
    def ChannelInput right

    def void run() {
        def fromPhilosopher = [left, right]
        def forkAlt = new ALT(fromPhilosopher)
        while (true) {
            def i = forkAlt.select()
            fromPhilosopher[i].read()      //pick up fork i
            fromPhilosopher[i].read()      //put down fork i
        }
    }
}

      
    